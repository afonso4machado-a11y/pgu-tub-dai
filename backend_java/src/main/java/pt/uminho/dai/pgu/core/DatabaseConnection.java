package pt.uminho.dai.pgu.core;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
 
public class DatabaseConnection {
    // Variáveis de ambiente configuradas no Azure ou localmente
    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_DB = "tub";
    private static final String DEFAULT_USER = "tub_user";
    private static final String DEFAULT_PASS = "tub_pass";

    private static final java.util.Map<String, String> envCache = new java.util.HashMap<>();

    private DatabaseConnection() {}

    private static String getEnv(String key, String def) {
        if (envCache.isEmpty()) {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(".env");
                if (java.nio.file.Files.exists(path)) {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(path);
                    for (String line : lines) {
                        if (line.contains("=") && !line.startsWith("#")) {
                            String[] parts = line.split("=", 2);
                            envCache.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Aviso: Não foi possível ler .env: " + e.getMessage());
            }
        }
        String val = System.getenv(key);
        if (val == null) val = envCache.get(key);
        return (val != null) ? val : def;
    }

    public static Connection obterConexao() throws SQLException {
        String host = getEnv("DB_HOST", DEFAULT_HOST);
        String dbName = getEnv("DB_NAME", DEFAULT_DB);
        String user = getEnv("DB_USER", DEFAULT_USER);
        String pass = getEnv("DB_PASS", DEFAULT_PASS);

        // URL base com parâmetros de estabilidade e segurança para Azure
        String url = String.format("jdbc:mysql://%s:3306/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=5000", 
                                   host, dbName);

        // Se estivermos no Azure MySQL Flexible Server, o SSL pode ser obrigatório
        if (!host.equals("localhost")) {
            url = url.replace("useSSL=false", "useSSL=true&requireSSL=false&verifyServerCertificate=false&enabledTLSProtocols=TLSv1.2");
        }

        return DriverManager.getConnection(url, user, pass);
    }
}