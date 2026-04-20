package pt.uminho.dai.pgu.core;
 
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/tub?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "tub_user";
    private static final String PASS = "tub_pass";
 
    private DatabaseConnection() {}
 
    public static Connection obterConexao() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}