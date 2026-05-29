package pt.uminho.dai.pgu.data.operacao_tempo_real;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;

import java.sql.*;
import java.util.*;

public class RepositorioLeituras {
  private boolean semBD = false;

  public RepositorioLeituras() {}

  public RepositorioLeituras(boolean semBD) {
    this.semBD = semBD;
  }

  public void guardar(String autocarroId, LeituraContagem leitura) {
    if (semBD) return;
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
 if (semBD) return resultado;
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

 /**
 * Retorna o total de entradas por hora do dia atual.
 * Estrutura: [ { "hora": 8, "passageiros": 320 }, ... ]
 */
 public List<Map<String, Object>> obterVolumePorHoraHoje() {
 List<Map<String, Object>> resultado = new ArrayList<>();
 if (semBD) return resultado;
 String sql = "SELECT HOUR(timestamp) as hora, SUM(entradas) as passageiros " +
 "FROM leituras WHERE timestamp >= CURDATE() AND timestamp < CURDATE() + INTERVAL 1 DAY " +
 "GROUP BY HOUR(timestamp) ORDER BY hora ASC";
 try (Connection conn = DatabaseConnection.obterConexao();
 PreparedStatement ps = conn.prepareStatement(sql);
 ResultSet rs = ps.executeQuery()) {
 while (rs.next()) {
 Map<String, Object> row = new LinkedHashMap<>();
 row.put("hora", rs.getInt("hora"));
 row.put("passageiros", rs.getInt("passageiros"));
 resultado.add(row);
 }
 } catch (SQLException e) {
 System.err.println("Erro ao obter volume por hora: " + e.getMessage());
 }
 return resultado;
 }
}