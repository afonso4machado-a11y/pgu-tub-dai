package pt.uminho.dai.pgu.data.operacao_tempo_real;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RepositorioAlertas {
 private boolean semBD = false;

 public RepositorioAlertas() {}

 public RepositorioAlertas(boolean semBD) {
 this.semBD = semBD;
 }

 public List<Long> guardarTodos(List<Alerta> alertas) {
 List<Long> ids = new ArrayList<>();
 if (alertas.isEmpty()) return ids;
 if (semBD) {
 for (int i = 0; i < alertas.size(); i++) {
 ids.add((long) (i + 1));
 }
 return ids;
 }
 try (Connection conn = DatabaseConnection.obterConexao();
 PreparedStatement ps = conn.prepareStatement(
 "INSERT INTO alertas (autocarro_id, tipo, mensagem, timestamp) VALUES (?, ?, ?, ?)",
 Statement.RETURN_GENERATED_KEYS)) {
 for (Alerta alerta : alertas) {
 ps.setString(1, alerta.getAutocarroId());
 ps.setString(2, alerta.getTipo().name());
 ps.setString(3, alerta.getMensagem());
 ps.setTimestamp(4, Timestamp.valueOf(alerta.getTimestamp()));
 ps.addBatch();
 }
 ps.executeBatch();
 try (ResultSet rs = ps.getGeneratedKeys()) {
 while (rs.next()) ids.add(rs.getLong(1));
 }
 } catch (SQLException e) {
 System.err.println("Erro ao guardar alertas: " + e.getMessage());
 }
 return ids;
 }

 public List<Alerta> listarAlertasRecentes(int limit) {
 List<Alerta> recentes = new ArrayList<>();
 if (semBD) return recentes;
 try (Connection conn = DatabaseConnection.obterConexao();
 PreparedStatement ps = conn.prepareStatement(
 "SELECT autocarro_id, tipo, mensagem, timestamp FROM alertas ORDER BY timestamp DESC LIMIT ?")) {
 ps.setInt(1, limit);
 try (ResultSet rs = ps.executeQuery()) {
 while(rs.next()) {
 String aId = rs.getString("autocarro_id");
 TipoAlerta t = TipoAlerta.valueOf(rs.getString("tipo"));
 String m = rs.getString("mensagem");
 java.time.LocalDateTime ts = rs.getTimestamp("timestamp").toLocalDateTime();
 recentes.add(new Alerta(aId, t, m, ts));
 }
 }
 } catch (SQLException e) {
 System.err.println("Erro ao listar alertas: " + e.getMessage());
 }
 return recentes;
 }
}