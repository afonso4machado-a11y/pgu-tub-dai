package pt.uminho.dai.pgu.repositories;
import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.services.*;

import java.sql.*;
import java.util.*;

public class RepositorioLeituras {

    public RepositorioLeituras() {}

    public RepositorioLeituras(boolean semBD) {
        // Construtor para uso em testes
    }

    public void guardar(String autocarroId, LeituraContagem leitura) {
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO leituras (autocarro_id, entradas, saidas, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, autocarroId);
            ps.setInt(2, leitura.getEntradas());
            ps.setInt(3, leitura.getSaidas());
            ps.setTimestamp(4, Timestamp.valueOf(leitura.getTimestamp()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar leitura: " + e.getMessage());
        }
    }

    /**
     * Retorna o total de entradas e saidas por autocarro por dia.
     * Estrutura: { "2026-04-11": { "TUB-101": { "entradas": 120, "saidas": 115 } } }
     */
    public Map<String, Map<String, Map<String, Integer>>> obterHistoricoPorDia() {
        Map<String, Map<String, Map<String, Integer>>> resultado = new LinkedHashMap<>();
        String sql = "SELECT DATE(timestamp) as dia, autocarro_id, " +
                     "SUM(entradas) as total_entradas, SUM(saidas) as total_saidas " +
                     "FROM leituras GROUP BY dia, autocarro_id ORDER BY dia DESC";
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String dia = rs.getString("dia");
                String autocarroId = rs.getString("autocarro_id");
                int entradas = rs.getInt("total_entradas");
                int saidas = rs.getInt("total_saidas");

                resultado.computeIfAbsent(dia, k -> new LinkedHashMap<>())
                         .computeIfAbsent(autocarroId, k -> new LinkedHashMap<>())
                         .put("entradas", entradas);
                resultado.get(dia).get(autocarroId).put("saidas", saidas);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao obter historico por dia: " + e.getMessage());
        }
        return resultado;
    }
}