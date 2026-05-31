package pt.uminho.dai.pgu.data.operacao_tempo_real;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RepositorioAutocarros {
 private final Map<String, Autocarro> autocarros = new ConcurrentHashMap<>();
 private boolean semBD = false;

 /**
  * true se a coluna `deleted` já existe na BD.
  * Detectado no arranque via INFORMATION_SCHEMA.
  * Permite que o código funcione com e sem a coluna na BD do Azure.
  */
 private boolean deletedColumnExists = false;

 public RepositorioAutocarros() {
  garantirColunaDeleted(); // auto-migration no arranque
  carregarDaBD();
 }

 public RepositorioAutocarros(boolean semBD) {
  this.semBD = semBD;
 }

 /**
  * Garante que a coluna `deleted` existe na tabela autocarros.
  * Se não existir, tenta adicioná-la via ALTER TABLE automaticamente.
  * Idempotente — seguro de chamar múltiplas vezes.
  */
 private void garantirColunaDeleted() {
  try (Connection conn = DatabaseConnection.obterConexao()) {
   String dbName = DatabaseConnection.getEnv("DB_NAME", "tub");
   // Verificar via INFORMATION_SCHEMA
   try (PreparedStatement ps = conn.prepareStatement(
    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
    "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'autocarros' AND COLUMN_NAME = 'deleted'")) {
    ps.setString(1, dbName);
    try (ResultSet rs = ps.executeQuery()) {
     rs.next();
     deletedColumnExists = rs.getInt(1) > 0;
    }
   }

   if (!deletedColumnExists) {
    System.out.println("[DB] Coluna 'deleted' nao encontrada — a adicionar automaticamente...");
    try (Statement st = conn.createStatement()) {
     st.execute("ALTER TABLE autocarros ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE");
     deletedColumnExists = true;
     System.out.println("[DB] Coluna 'deleted' adicionada com sucesso.");
    } catch (SQLException e) {
     System.err.println("[DB] Nao foi possivel adicionar coluna 'deleted': " + e.getMessage());
     System.err.println("[DB] A funcionalidade de eliminacao ficara limitada ao estado em memoria.");
     deletedColumnExists = false;
    }
   } else {
    System.out.println("[DB] Coluna 'deleted' confirmada na BD.");
   }
  } catch (Exception e) {
   System.err.println("[DB] Erro ao verificar coluna 'deleted': " + e.getMessage());
   deletedColumnExists = false;
  }
 }

 private void carregarDaBD() {
  // SQL dinâmico conforme a coluna existe ou não
  String sql = deletedColumnExists
   ? "SELECT id, capacidade_maxima, matricula, marca, modelo, linha_id, passageiros_atuais, " +
     "total_passageiros_transportados, ultima_leitura, deleted FROM autocarros"
   : "SELECT id, capacidade_maxima, matricula, marca, modelo, linha_id, passageiros_atuais, " +
     "total_passageiros_transportados, ultima_leitura FROM autocarros";

  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(sql);
   ResultSet rs = ps.executeQuery()) {
   while (rs.next()) {
    String id = rs.getString("id");
    int capacidade = rs.getInt("capacidade_maxima");
    String matricula = rs.getString("matricula");
    String marca = rs.getString("marca");
    String modelo = rs.getString("modelo");
    String linhaId = rs.getString("linha_id");
    int passageiros = rs.getInt("passageiros_atuais");
    int totalTransportados = rs.getInt("total_passageiros_transportados");
    Timestamp ultimaLeituraTs = rs.getTimestamp("ultima_leitura");
    LocalDateTime ultimaLeitura = ultimaLeituraTs != null ? ultimaLeituraTs.toLocalDateTime() : null;
    boolean deleted = deletedColumnExists && rs.getBoolean("deleted");

    Autocarro a = new Autocarro(id, capacidade, matricula, marca, modelo);
    a.setLinhaId(linhaId);
    a.setPassageirosAtuais(passageiros);
    a.setTotalPassageirosTransportados(totalTransportados);
    a.setUltimaLeitura(ultimaLeitura);
    a.setDeleted(deleted);
    autocarros.put(id, a);
   }
   System.out.println("[DB] Autocarros carregados: " + autocarros.size());
  } catch (SQLException e) {
   System.err.println("Erro ao carregar autocarros: " + e.getMessage());
  }
 }

 public void guardar(Autocarro autocarro) {
  autocarros.put(autocarro.getId(), autocarro);
  if (semBD) return;
  try (Connection conn = DatabaseConnection.obterConexao()) {
   // SQL diferente consoante a coluna deleted existe ou não
   String sql = deletedColumnExists
    ? "INSERT INTO autocarros (id, capacidade_maxima, matricula, marca, modelo, linha_id, " +
      "passageiros_atuais, total_passageiros_transportados, ultima_leitura, deleted) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE) " +
      "ON DUPLICATE KEY UPDATE capacidade_maxima = VALUES(capacidade_maxima), " +
      "matricula = VALUES(matricula), marca = VALUES(marca), modelo = VALUES(modelo), " +
      "linha_id = VALUES(linha_id), passageiros_atuais = VALUES(passageiros_atuais), " +
      "total_passageiros_transportados = VALUES(total_passageiros_transportados), " +
      "ultima_leitura = VALUES(ultima_leitura)"
    : "INSERT INTO autocarros (id, capacidade_maxima, matricula, marca, modelo, linha_id, " +
      "passageiros_atuais, total_passageiros_transportados, ultima_leitura) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
      "ON DUPLICATE KEY UPDATE capacidade_maxima = VALUES(capacidade_maxima), " +
      "matricula = VALUES(matricula), marca = VALUES(marca), modelo = VALUES(modelo), " +
      "linha_id = VALUES(linha_id), passageiros_atuais = VALUES(passageiros_atuais), " +
      "total_passageiros_transportados = VALUES(total_passageiros_transportados), " +
      "ultima_leitura = VALUES(ultima_leitura)";

   try (PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, autocarro.getId());
    ps.setInt(2, autocarro.getCapacidadeMaxima());
    ps.setString(3, autocarro.getMatricula());
    ps.setString(4, autocarro.getMarca());
    ps.setString(5, autocarro.getModelo());
    ps.setString(6, autocarro.getLinhaId());
    ps.setInt(7, autocarro.getPassageirosAtuais());
    ps.setInt(8, autocarro.getTotalPassageirosTransportados());
    ps.setTimestamp(9, autocarro.getUltimaLeitura() != null ?
     Timestamp.valueOf(autocarro.getUltimaLeitura()) : null);
    ps.executeUpdate();
   }
  } catch (SQLException e) {
   System.err.println("Erro ao guardar autocarro: " + e.getMessage());
  }
 }

 public void atualizarEstado(Autocarro autocarro) {
  autocarros.put(autocarro.getId(), autocarro);
  if (semBD) return;
  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(
   "UPDATE autocarros SET linha_id = ?, passageiros_atuais = ?, " +
   "total_passageiros_transportados = ?, ultima_leitura = ? WHERE id = ?")) {
   ps.setString(1, autocarro.getLinhaId());
   ps.setInt(2, autocarro.getPassageirosAtuais());
   ps.setInt(3, autocarro.getTotalPassageirosTransportados());
   ps.setTimestamp(4, autocarro.getUltimaLeitura() != null ?
   Timestamp.valueOf(autocarro.getUltimaLeitura()) : null);
   ps.setString(5, autocarro.getId());
   ps.executeUpdate();
  } catch (SQLException e) {
   System.err.println("Erro ao atualizar estado do autocarro: " + e.getMessage());
  }
 }

 /** Marca o autocarro como eliminado (soft delete) — dados históricos preservados */
 public void marcarComoEliminado(String id) {
  Autocarro a = autocarros.get(id);
  if (a != null) a.setDeleted(true);
  if (semBD) return;
  if (!deletedColumnExists) {
   System.out.println("[DB] Eliminacao em memoria apenas (coluna 'deleted' nao existe na BD).");
   return;
  }
  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(
   "UPDATE autocarros SET deleted = TRUE WHERE id = ?")) {
   ps.setString(1, id);
   ps.executeUpdate();
  } catch (SQLException e) {
   System.err.println("Erro ao eliminar autocarro: " + e.getMessage());
  }
 }

 /** Restaura um autocarro eliminado */
 public void restaurar(String id) {
  Autocarro a = autocarros.get(id);
  if (a != null) a.setDeleted(false);
  if (semBD) return;
  if (!deletedColumnExists) {
   System.out.println("[DB] Restauro em memoria apenas (coluna 'deleted' nao existe na BD).");
   return;
  }
  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(
   "UPDATE autocarros SET deleted = FALSE WHERE id = ?")) {
   ps.setString(1, id);
   ps.executeUpdate();
  } catch (SQLException e) {
   System.err.println("Erro ao restaurar autocarro: " + e.getMessage());
  }
 }

 /** Procura por ID incluindo eliminados (para operações de restauro) */
 public Optional<Autocarro> procurarPorIdIncluindoEliminados(String id) {
  return Optional.ofNullable(autocarros.get(id));
 }

 /** Apenas autocarros activos (não eliminados) */
 public Optional<Autocarro> procurarPorId(String id) {
  return Optional.ofNullable(autocarros.get(id))
   .filter(a -> !a.isDeleted());
 }

 /** Lista apenas autocarros activos */
 public Collection<Autocarro> listarTodos() {
  return autocarros.values().stream()
   .filter(a -> !a.isDeleted())
   .collect(Collectors.toList());
 }

 /** Lista apenas autocarros eliminados */
 public List<Autocarro> listarEliminados() {
  return autocarros.values().stream()
   .filter(Autocarro::isDeleted)
   .collect(Collectors.toList());
 }
}