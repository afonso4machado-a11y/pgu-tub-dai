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

public class RepositorioParagens {
 private final List<String> paragens = new ArrayList<>();

 public RepositorioParagens() {
 carregarDaBD();
 }

 private void carregarDaBD() {
 try (Connection conn = DatabaseConnection.obterConexao();
 PreparedStatement ps = conn.prepareStatement("SELECT nome FROM paragens ORDER BY nome");
 ResultSet rs = ps.executeQuery()) {
 while (rs.next()) {
 paragens.add(rs.getString("nome"));
 }
 } catch (SQLException e) {
 System.err.println("Erro ao carregar paragens: " + e.getMessage());
 }
 }

 public List<String> listarTodas() {
 if (paragens.isEmpty()) {
 carregarDaBD(); // Tenta recarregar se estiver vazio
 }
 return new ArrayList<>(paragens);
 }

 public List<Map<String, Object>> listarTodasComCoordenadas() {
     List<Map<String, Object>> lista = new ArrayList<>();
     try (Connection conn = DatabaseConnection.obterConexao();
          PreparedStatement ps = conn.prepareStatement("SELECT nome, latitude, longitude FROM paragens ORDER BY nome");
          ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
             Map<String, Object> map = new HashMap<>();
             map.put("nome", rs.getString("nome"));
             map.put("lat", rs.getObject("latitude"));
             map.put("lng", rs.getObject("longitude"));
             lista.add(map);
         }
     } catch (SQLException e) {
         System.err.println("Erro ao carregar paragens com coordenadas: " + e.getMessage());
     }
     return lista;
 }
}