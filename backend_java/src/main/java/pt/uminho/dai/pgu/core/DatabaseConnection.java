package pt.uminho.dai.pgu.core;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestão centralizada de conexões à base de dados MySQL.
 * 
 * Prioridade de configuração:
 *   1. Variáveis de ambiente do sistema (Azure App Service → Configuration)
 *   2. Ficheiro .env local (desenvolvimento)
 *   3. Defaults (localhost/tub para Docker local)
 * 
 * Quando o host não é localhost, ativa SSL/TLS automaticamente
 * para comunicação segura com Azure MySQL Flexible Server.
 */
public class DatabaseConnection {
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_DB = "tub";
    private static final String DEFAULT_USER = "tub_user";
    private static final String DEFAULT_PASS = "tub_pass";

    // Cache do .env — carregado uma única vez no arranque
    private static final java.util.Map<String, String> envCache = new java.util.HashMap<>();
    private static boolean envLoaded = false;

    private DatabaseConnection() {}

    /**
     * Resolve uma variável de configuração com fallback:
     * System.getenv → .env file → default
     */
    private static synchronized String getEnv(String key, String def) {
        // Carregar .env apenas uma vez
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

        // Prioridade: System env (Azure) → .env file → default
        String val = System.getenv(key);
        if (val == null || val.isBlank()) val = envCache.get(key);
        return (val != null && !val.isBlank()) ? val : def;
    }

    /**
     * Obtém uma conexão JDBC à base de dados MySQL.
     * 
     * Para Azure MySQL Flexible Server:
     * - SSL/TLS é ativado automaticamente
     * - connectTimeout e socketTimeout previnem conexões penduradas
     * - allowPublicKeyRetrieval necessário para autenticação caching_sha2_password
     */
    public static Connection obterConexao() throws SQLException {
        String host = getEnv("DB_HOST", DEFAULT_HOST);
        String dbName = getEnv("DB_NAME", DEFAULT_DB);
        String user = getEnv("DB_USER", DEFAULT_USER);
        String pass = getEnv("DB_PASS", DEFAULT_PASS);

        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://").append(host).append(":3306/").append(dbName);
        url.append("?serverTimezone=UTC");
        url.append("&allowPublicKeyRetrieval=true");
        url.append("&connectTimeout=5000");
        url.append("&socketTimeout=30000");
        url.append("&autoReconnect=true");

        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            // Desenvolvimento local — sem SSL
            url.append("&useSSL=false");
        } else {
            // Produção (Azure MySQL Flexible Server) — SSL obrigatório
            url.append("&useSSL=true");
            url.append("&requireSSL=true");
            url.append("&verifyServerCertificate=false");
            url.append("&enabledTLSProtocols=TLSv1.2");
        }

        return DriverManager.getConnection(url.toString(), user, pass);
    }
}