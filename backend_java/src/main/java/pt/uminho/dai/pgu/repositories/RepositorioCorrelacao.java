package pt.uminho.dai.pgu.repositories;
import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.services.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositório especializado para operações de correlação e análise massiva de dados.
 * Move a lógica SQL da camada de domínio (Sistema) para a camada de persistência.
 */
public class RepositorioCorrelacao {

    public List<Map<String, Object>> obterProcuraPorLinha(String dataInicio, String dataFim) {
        List<Map<String, Object>> res = new ArrayList<>();
        String sql =
            "SELECT a.linha_id, " +
            "SUM(l.entradas) as total_entradas, " +
            "SUM(l.saidas) as total_saidas, " +
            "COUNT(DISTINCT DATE(l.timestamp)) as dias_com_dados, " +
            "COUNT(*) as total_leituras " +
            "FROM leituras l " +
            "JOIN autocarros a ON l.autocarro_id = a.id " +
            "WHERE DATE(l.timestamp) BETWEEN ? AND ? " +
            "AND a.linha_id IS NOT NULL " +
            "GROUP BY a.linha_id " +
            "ORDER BY total_entradas DESC";

        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataInicio);
            ps.setString(2, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("linhaId", rs.getString("linha_id"));
                    entry.put("totalEntradas", rs.getInt("total_entradas"));
                    entry.put("totalSaidas", rs.getInt("total_saidas"));
                    entry.put("diasComDados", rs.getInt("dias_com_dados"));
                    entry.put("totalLeituras", rs.getInt("total_leituras"));
                    res.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro na correlação (procura): " + e.getMessage());
        }
        return res;
    }

    public List<Map<String, Object>> obterOfertaPlaneada() {
        List<Map<String, Object>> res = new ArrayList<>();
        String sql =
            "SELECT v.linha_id, v.tipo_dia, " +
            "COUNT(*) as viagens_programadas " +
            "FROM viagens v " +
            "GROUP BY v.linha_id, v.tipo_dia " +
            "ORDER BY v.linha_id";

        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("linhaId", rs.getString("linha_id"));
                entry.put("tipoDia", rs.getString("tipo_dia"));
                entry.put("viagensProgramadas", rs.getInt("viagens_programadas"));
                res.add(entry);
            }
        } catch (SQLException e) {
            System.err.println("Erro na correlação (oferta): " + e.getMessage());
        }
        return res;
    }

    public List<Map<String, Object>> obterProcuraPorHora(String dataInicio, String dataFim) {
        List<Map<String, Object>> res = new ArrayList<>();
        String sql =
            "SELECT HOUR(l.timestamp) as hora, " +
            "SUM(l.entradas) as entradas, " +
            "SUM(l.saidas) as saidas " +
            "FROM leituras l " +
            "WHERE DATE(l.timestamp) BETWEEN ? AND ? " +
            "GROUP BY hora " +
            "ORDER BY hora";

        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dataInicio);
            ps.setString(2, dataFim);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("hora", rs.getInt("hora"));
                    entry.put("entradas", rs.getInt("entradas"));
                    entry.put("saidas", rs.getInt("saidas"));
                    res.add(entry);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro na correlação (horária): " + e.getMessage());
        }
        return res;
    }
}
