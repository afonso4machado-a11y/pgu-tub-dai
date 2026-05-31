package pt.uminho.dai.pgu.data.analitica_historico;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.business.analitica_historico.RegistoAcao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Secure database repository for Audit logs.
 * Only allows INSERT (batch) and SELECT operations, making it secure and tamper-proof.
 */
public class RepositorioAuditLogs {

    /**
     * Saves a list of backoffice action logs securely using batch insert and parameterized queries.
     *
     * @param logs the list of logs to persist.
     */
    public void guardarEmLote(List<RegistoAcao> logs) {
        if (logs == null || logs.isEmpty()) return;
        
        String sql = "INSERT INTO registo_acoes_backoffice (session_id, utilizador_email, utilizador_nome, acao_tipo, detalhes, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); // Enable manual transaction management for batch safety
            
            for (RegistoAcao log : logs) {
                // Input validation / sanitization boundaries are kept solid
                ps.setString(1, sanitize(log.getSessionId()));
                ps.setString(2, sanitize(log.getUtilizadorEmail()));
                ps.setString(3, sanitize(log.getUtilizadorNome()));
                ps.setString(4, sanitize(log.getAcaoTipo()));
                ps.setString(5, sanitize(log.getDetalhes()));
                ps.setTimestamp(6, Timestamp.valueOf(log.getTimestamp()));
                ps.addBatch();
            }
            
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar registos de auditoria em lote: " + e.getMessage());
        }
    }

    /**
     * Lists all backoffice audit logs sorted by their timestamp in descending order.
     *
     * @return a list of all logged actions.
     */
    public List<RegistoAcao> listarTodos() {
        List<RegistoAcao> lista = new ArrayList<>();
        String sql = "SELECT id, session_id, utilizador_email, utilizador_nome, acao_tipo, detalhes, timestamp FROM registo_acoes_backoffice ORDER BY timestamp DESC";
        
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                lista.add(new RegistoAcao(
                    rs.getInt("id"),
                    rs.getString("session_id"),
                    rs.getString("utilizador_email"),
                    rs.getString("utilizador_nome"),
                    rs.getString("acao_tipo"),
                    rs.getString("detalhes"),
                    rs.getTimestamp("timestamp").toLocalDateTime()
                ));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao listar registos de auditoria: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Basic sanitization helper to strip control/null characters, keeping data clean.
     */
    private String sanitize(String val) {
        if (val == null) return "";
        return val.replace("\0", "").trim();
    }
}
