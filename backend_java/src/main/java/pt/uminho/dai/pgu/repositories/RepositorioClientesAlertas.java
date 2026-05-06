package pt.uminho.dai.pgu.repositories;
import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.services.*;

import java.sql.*;
import java.util.List;

public class RepositorioClientesAlertas {

    public RepositorioClientesAlertas() {}

    public RepositorioClientesAlertas(boolean semBD) {
        // Construtor para uso em testes
    }

    public void guardar(String clienteId, List<Long> alertaIds) {
        if (alertaIds.isEmpty()) return;
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO clientes_alertas (cliente_id, alerta_id) VALUES (?, ?)")) {
            for (Long alertaId : alertaIds) {
                ps.setString(1, clienteId);
                ps.setLong(2, alertaId);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar ligação cliente-alerta: " + e.getMessage());
        }
    }
}