package pt.uminho.dai.pgu.data.operacao_tempo_real;

import pt.uminho.dai.pgu.data.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class RepositorioPlaneamento {

    public Map<String, Object> encontrarProximaViagem(String origem, String destino, String horaAtual) {
        String sql = "SELECT " +
                     "    v.linha_id, " +
                     "    h1.hora AS partida, " +
                     "    h2.hora AS chegada, " +
                     "    ROUND(TIME_TO_SEC(TIMEDIFF(h2.hora, h1.hora)) / 60) AS tempo_minutos " +
                     "FROM viagens v " +
                     "JOIN horarios h1 ON v.id = h1.viagem_id " +
                     "JOIN paragens p1 ON h1.paragem_id = p1.id " +
                     "JOIN horarios h2 ON v.id = h2.viagem_id " +
                     "JOIN paragens p2 ON h2.paragem_id = p2.id " +
                     "WHERE p1.nome LIKE ? " +
                     "  AND p2.nome LIKE ? " +
                     "  AND h1.hora >= ? " +
                     "  AND h1.hora < h2.hora " +
                     "ORDER BY h1.hora ASC " +
                     "LIMIT 1";

        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Utilizamos % para permitir correspondência parcial (e.g. "U.Minho" -> "Universidade do Minho" se for LIKE)
            // Mas o ideal é igualdade ou %texto%. Vamos tentar %texto% para maior tolerância a espaços
            ps.setString(1, "%" + origem.trim() + "%");
            ps.setString(2, "%" + destino.trim() + "%");
            ps.setString(3, horaAtual);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("linha_id", rs.getString("linha_id"));
                    result.put("partida", rs.getString("partida"));
                    result.put("chegada", rs.getString("chegada"));
                    result.put("tempo_minutos", rs.getInt("tempo_minutos"));
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
