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
import java.util.concurrent.ConcurrentHashMap;

/**
 * RepositorioLinhas — persiste linhas na BD do Azure MySQL.
 *
 * Anteriormente era apenas in-memory (dados perdidos ao reiniciar o servidor).
 * Agora carrega da BD no arranque e persiste cada guardar() imediatamente.
 */
public class RepositorioLinhas {
 private final Map<String, Linha> linhas = new ConcurrentHashMap<>();

 public RepositorioLinhas() {
  carregarDaBD();
 }

 /** Carrega todas as linhas da BD para o cache em memória. */
 private void carregarDaBD() {
  String sql = "SELECT id, nome FROM linhas";
  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(sql);
   ResultSet rs = ps.executeQuery()) {
   while (rs.next()) {
    String id = rs.getString("id");
    String nome = rs.getString("nome");
    linhas.put(id, new Linha(id, nome));
   }
   System.out.println("[DB] Linhas carregadas: " + linhas.size());
  } catch (SQLException e) {
   System.err.println("[DB] Erro ao carregar linhas: " + e.getMessage());
  }
 }

 /**
  * Guarda uma linha em memória E na BD (INSERT OR UPDATE).
  * Usa ON DUPLICATE KEY UPDATE para ser idempotente.
  */
 public void guardar(Linha linha) {
  linhas.put(linha.getId(), linha);
  String sql = "INSERT INTO linhas (id, nome) VALUES (?, ?) " +
         "ON DUPLICATE KEY UPDATE nome = VALUES(nome)";
  try (Connection conn = DatabaseConnection.obterConexao();
   PreparedStatement ps = conn.prepareStatement(sql)) {
   ps.setString(1, linha.getId());
   ps.setString(2, linha.getNome());
   ps.executeUpdate();
  } catch (SQLException e) {
   System.err.println("[DB] Erro ao guardar linha: " + e.getMessage());
  }
 }

 public Optional<Linha> procurarPorId(String id) {
  return Optional.ofNullable(linhas.get(id));
 }

 public Collection<Linha> listarTodas() {
  return new ArrayList<>(linhas.values());
 }
}