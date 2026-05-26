package pt.uminho.dai.pgu.data;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
 
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestão centralizada de conexões à base de dados MySQL com HikariCP.
 * 
 * HikariCP oferece:
 * - Connection pooling com limite configurável
 * - Connection reuse (elimina overhead de crear conexões)
 * - Automatic retry com exponential backoff
 * - Monitoramento de conexões ociosas
 * 
 * Prioridade de configuração:
 * 1. Variáveis de ambiente do sistema (Azure App Service → Configuration)
 * 2. Ficheiro .env local (desenvolvimento)
 * 3. Defaults (localhost/tub para Docker local)
 */
public class DatabaseConnection {
 private static final String DEFAULT_HOST = "localhost";
 private static final String DEFAULT_DB = "tub";
 private static final String DEFAULT_USER = "";
 private static final String DEFAULT_PASS = "";

 private static HikariDataSource dataSource;
 private static final java.util.Map<String, String> envCache = new java.util.HashMap<>();
 private static boolean envLoaded = false;

 private DatabaseConnection() {}

 public static synchronized String getEnv(String key, String def) {
 if (!envLoaded) {
 envLoaded = true;
 try {
 java.nio.file.Path path = java.nio.file.Paths.get(".env");
 if (java.nio.file.Files.exists(path)) {
 java.util.List<String> lines = java.nio.file.Files.readAllLines(path);
 for (String line : lines) {
 String trimmed = line.trim();
 if (trimmed.contains("=") && !trimmed.startsWith("#") && !trimmed.isEmpty()) {
 String[] parts = trimmed.split("=", 2);
 if (parts.length == 2 && !parts[1].trim().isEmpty()) {
 envCache.put(parts[0].trim(), parts[1].trim());
 }
 }
 }
 System.out.println("[DB] Ficheiro .env carregado com " + envCache.size() + " variáveis.");
 }
 } catch (Exception e) {
 System.err.println("[DB] Aviso: Não foi possível ler .env: " + e.getMessage());
 }
 }

 String val = System.getenv(key);
 if (val == null || val.isBlank()) val = envCache.get(key);
 return (val != null && !val.isBlank()) ? val : def;
 }

 private static synchronized void initializeDataSource() throws SQLException {
 if (dataSource != null && !dataSource.isClosed()) {
 return;
 }

 String host = getEnv("DB_HOST", DEFAULT_HOST);
 String dbName = getEnv("DB_NAME", DEFAULT_DB);
 String user = getEnv("DB_USER", DEFAULT_USER);
 String pass = getEnv("DB_PASS", DEFAULT_PASS);
 String poolSize = getEnv("DB_POOL_SIZE", "10");
 String maxPoolSize = getEnv("DB_MAX_POOL_SIZE", "20");

 if (user.isBlank() || pass.isBlank()) {
 throw new SQLException("Acesso Negado: As credenciais da base de dados não estão configuradas nas variáveis de ambiente. Verifique o ficheiro .env ou o Azure Key Vault.");
 }

 StringBuilder url = new StringBuilder();
 url.append("jdbc:mysql://").append(host).append(":3306/").append(dbName);
 url.append("?serverTimezone=UTC");
 url.append("&allowPublicKeyRetrieval=true");
 url.append("&useUnicode=true&characterEncoding=UTF-8");
 url.append("&useSSL=").append(host.equals("localhost") || host.equals("127.0.0.1") ? "false" : "true");
 url.append("&requireSSL=").append(host.equals("localhost") || host.equals("127.0.0.1") ? "false" : "true");

 HikariConfig config = new HikariConfig();
 config.setJdbcUrl(url.toString());
 config.setUsername(user);
 config.setPassword(pass);
 config.setMaximumPoolSize(Integer.parseInt(maxPoolSize));
 config.setMinimumIdle(Integer.parseInt(poolSize));
 config.setConnectionTimeout(3000); // Fail-fast: 3s (era 5s)
 config.setIdleTimeout(300000); // Liberta idle mais cedo: 5min (era 10min)
 config.setMaxLifetime(900000); // Recicla conexões: 15min (era 30min)
 config.setAutoCommit(true);
 config.setLeakDetectionThreshold(15000);
 config.setPoolName("PGU-TUB-Pool");
 config.setValidationTimeout(1000); // NOVO: Valida conexão em 1s
 config.setConnectionTestQuery("SELECT 1"); // NOVO: Health check antes de entregar conexão

 dataSource = new HikariDataSource(config);
 System.out.println("[DB] HikariCP initialized with pool size: " + poolSize + ", max: " + maxPoolSize);
 }

 public static Connection obterConexao() throws SQLException {
 if (dataSource == null || dataSource.isClosed()) {
 initializeDataSource();
 }
 return dataSource.getConnection();
 }

 public static void fecharDataSource() {
 if (dataSource != null && !dataSource.isClosed()) {
 dataSource.close();
 System.out.println("[DB] HikariCP DataSource closed.");
 }
 }
}