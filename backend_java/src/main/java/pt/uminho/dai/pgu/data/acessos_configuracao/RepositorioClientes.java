package pt.uminho.dai.pgu.data.acessos_configuracao;

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

public class RepositorioClientes {
 private final Map<String, Cliente> clientesById = new ConcurrentHashMap<>();
 private final Map<String, Cliente> clientesByEmail = new ConcurrentHashMap<>();
 private volatile boolean isLoaded = false;
 private boolean semBD = false;

 public RepositorioClientes() {
 carregarDaBD();
 }

 public RepositorioClientes(boolean semBD) {
 this.semBD = semBD;
 }

 private void carregarDaBD() {
 try (Connection conn = DatabaseConnection.obterConexao();
 PreparedStatement ps = conn.prepareStatement("SELECT id, nome, email, password, nif, passe_mensal FROM clientes");
 ResultSet rs = ps.executeQuery()) {
 while (rs.next()) {
 String id = rs.getString("id");
 String nome = rs.getString("nome");
 String email = rs.getString("email");
 String password = rs.getString("password");
 String nif = rs.getString("nif");
 boolean passeMensal = rs.getBoolean("passe_mensal");
 
 Cliente cliente = new Cliente(id, nome, email, password, nif, passeMensal);
 clientesById.put(id, cliente);
 if (email != null) {
 clientesByEmail.put(email.toLowerCase(), cliente);
 }
 }

 // Carregar definições/configurações da base de dados
 try (PreparedStatement psDef = conn.prepareStatement("SELECT cliente_id, tema, notificacoes_ativas FROM definicoes_cliente")) {
 try (ResultSet rsDef = psDef.executeQuery()) {
 while (rsDef.next()) {
 String cid = rsDef.getString("cliente_id");
 String tema = rsDef.getString("tema");
 boolean notif = rsDef.getBoolean("notificacoes_ativas");
 Cliente c = clientesById.get(cid);
 if (c != null) {
 c.setTema(tema);
 c.setNotificacoesAtivas(notif);
 }
 }
 }
 }

 // Carregar linhas favoritas da base de dados
 try (PreparedStatement psFav = conn.prepareStatement("SELECT cliente_id, linha_id FROM linhas_favoritas")) {
 try (ResultSet rsFav = psFav.executeQuery()) {
 Map<String, List<String>> favs = new HashMap<>();
 while (rsFav.next()) {
 String cid = rsFav.getString("cliente_id");
 String lid = rsFav.getString("linha_id");
 favs.computeIfAbsent(cid, k -> new ArrayList<>()).add(lid);
 }
 for (Map.Entry<String, List<String>> entry : favs.entrySet()) {
 Cliente c = clientesById.get(entry.getKey());
 if (c != null) {
 c.setLinhasFavoritas(entry.getValue());
 }
 }
 }
 }

 isLoaded = true;
 System.out.println("[DB] Clientes carregados: " + clientesById.size());
 } catch (SQLException e) {
 System.err.println("Erro ao carregar clientes: " + e.getMessage());
 }
 }

 public void guardar(Cliente cliente) {
 clientesById.put(cliente.getId(), cliente);
 if (cliente.getEmail() != null) {
 clientesByEmail.put(cliente.getEmail().toLowerCase(), cliente);
 }
 
 if (semBD) return;
 try (Connection conn = DatabaseConnection.obterConexao()) {
 // 1. Inserir ou atualizar dados básicos do cliente
 try (PreparedStatement ps = conn.prepareStatement(
 "INSERT INTO clientes (id, nome, email, password, nif, passe_mensal) VALUES (?, ?, ?, ?, ?, ?) " +
 "ON DUPLICATE KEY UPDATE nome = VALUES(nome), password = VALUES(password), nif = VALUES(nif), passe_mensal = VALUES(passe_mensal)")) {
 ps.setString(1, cliente.getId());
 ps.setString(2, cliente.getNome());
 ps.setString(3, cliente.getEmail());
 ps.setString(4, cliente.getPassword());
 ps.setString(5, cliente.getNif());
 ps.setBoolean(6, cliente.isPasseMensal());
 ps.executeUpdate();
 }

 // 2. Inserir ou atualizar definições do cliente
 try (PreparedStatement psDef = conn.prepareStatement(
 "INSERT INTO definicoes_cliente (cliente_id, tema, notificacoes_ativas) VALUES (?, ?, ?) " +
 "ON DUPLICATE KEY UPDATE tema = VALUES(tema), notificacoes_ativas = VALUES(notificacoes_ativas)")) {
 psDef.setString(1, cliente.getId());
 psDef.setString(2, cliente.getTema());
 psDef.setBoolean(3, cliente.isNotificacoesAtivas());
 psDef.executeUpdate();
 }

 // 3. Persistir linhas favoritas do cliente
 try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM linhas_favoritas WHERE cliente_id = ?")) {
 psDel.setString(1, cliente.getId());
 psDel.executeUpdate();
 }
 List<String> favs = cliente.getLinhasFavoritas();
 if (favs != null && !favs.isEmpty()) {
 try (PreparedStatement psIns = conn.prepareStatement("INSERT INTO linhas_favoritas (cliente_id, linha_id) VALUES (?, ?)")) {
 for (String lid : favs) {
 psIns.setString(1, cliente.getId());
 psIns.setString(2, lid);
 psIns.addBatch();
 }
 psIns.executeBatch();
 }
 }
 } catch (SQLException e) {
 System.err.println("Erro ao guardar cliente: " + e.getMessage());
 }
 }

 public Optional<Cliente> procurarPorId(String id) {
 return Optional.ofNullable(clientesById.get(id));
 }

 public Optional<Cliente> procurarPorEmail(String email) {
 if (email == null) return Optional.empty();
 return Optional.ofNullable(clientesByEmail.get(email.toLowerCase()));
 }

 public Collection<Cliente> listarTodos() {
 return new ArrayList<>(clientesById.values());
 }
}