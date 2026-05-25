package pt.uminho.dai.pgu.p7_data.operacao_tempo_real;

import pt.uminho.dai.pgu.p7_data.DatabaseConnection;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;

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

    /**
     *  OPTIMIZAÇÃO: Insere todos os pares (cliente, alerta) numa única
     * conexão JDBC com batch. Reduz de N conexões para 1 conexão.
     */
    public void guardarEmLote(List<String> clienteIds, List<Long> alertaIds) {
        if (clienteIds.isEmpty() || alertaIds.isEmpty()) return;
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO clientes_alertas (cliente_id, alerta_id) VALUES (?, ?)")) {
            int batchCount = 0;
            for (String clienteId : clienteIds) {
                for (Long alertaId : alertaIds) {
                    ps.setString(1, clienteId);
                    ps.setLong(2, alertaId);
                    ps.addBatch();
                    batchCount++;
                    // Flush a cada 1000 registos para evitar OOM em lotes muito grandes
                    if (batchCount % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
            }
            ps.executeBatch(); // Flush final
        } catch (SQLException e) {
            System.err.println("[BATCH] Erro ao distribuir alertas em lote: " + e.getMessage());
        }
    }
}