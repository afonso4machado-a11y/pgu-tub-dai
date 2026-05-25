package pt.uminho.dai.pgu.api.analitica_historico;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.Alerta;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.Autocarro;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.Sistema;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SistemaService {

 private final Sistema sistema;

 public SistemaService(Sistema sistema) {
 this.sistema = sistema;
 }

 public void registarAutocarro(String id, int capacidade, String matricula, String marca, String modelo) throws Exception {
 sistema.registarAutocarro(id, capacidade, matricula, marca, modelo);
 }

 public List<Alerta> registarLeituras(String id, int entradas, int saidas) throws Exception {
 return sistema.receberLeitura(id, entradas, saidas);
 }

 public Map<String, Object> consultarAutocarro(String id) throws Exception {
 Autocarro a = sistema.obterAutocarro(id);
 return Map.of(
 "id", a.getId(),
 "matricula", a.getMatricula() != null ? a.getMatricula() : "N/A",
 "marca", a.getMarca() != null ? a.getMarca() : "N/A",
 "modelo", a.getModelo() != null ? a.getModelo() : "N/A",
 "passageirosAtuais", a.getPassageirosAtuais(),
 "capacidadeMaxima", a.getCapacidadeMaxima(),
 "taxaOcupacao", a.getTaxaOcupacao() * 100,
 "numeroAlertas", a.getHistoricoAlertas().size()
 );
 }

 public List<Map<String, Object>> listarAutocarros() {
 return sistema.obterTodosAutocarros().stream().map(a -> {
 Map<String, Object> map = new HashMap<>();
 map.put("id", a.getId());
 map.put("matricula", a.getMatricula());
 map.put("marca", a.getMarca());
 map.put("modelo", a.getModelo());
 map.put("linhaId", a.getLinhaId() != null ? a.getLinhaId() : "N/A");
 map.put("ocupacao", a.getTaxaOcupacao() * 100);
 map.put("passageirosAtuais", a.getPassageirosAtuais());
 map.put("capacidadeMaxima", a.getCapacidadeMaxima());
 map.put("totalPassageirosTransportados", a.getTotalPassageirosTransportados());
 map.put("ultimaLeitura", a.getUltimaLeitura() != null ? a.getUltimaLeitura().toString() : "N/A");
 return map;
 }).toList();
 }

 public Map<String, Object> obterDadosDashboard() throws Exception {
 Map<String, Object> dashboard = new java.util.LinkedHashMap<>(sistema.obterDadosDashboard());

 // Simulação vs Real: volumePorHora 
 boolean demoMode = "1".equals(System.getenv("PGU_DEMO_MODE"))
 || "true".equalsIgnoreCase(System.getenv("PGU_DEMO_MODE"));

 List<Map<String, Object>> volumePorHora;
 if (demoMode) {
 // Dados fictícios com picos realistas (manhã + fim de tarde)
 int[][] picos = {
 {6, 45}, {7, 280}, {8, 520}, {9, 390}, {10, 210}, {11, 180},
 {12, 320}, {13, 285}, {14, 180}, {15, 170}, {16, 245},
 {17, 480}, {18, 390}, {19, 190}, {20, 75}, {21, 35}
 };
 volumePorHora = new java.util.ArrayList<>();
 for (int[] p : picos) {
 Map<String, Object> row = new java.util.LinkedHashMap<>();
 row.put("hora", p[0]);
 row.put("passageiros", p[1]);
 volumePorHora.add(row);
 }
 } else {
 // Dados reais: GROUP BY HOUR() na BD para o dia atual
 volumePorHora = sistema.obterVolumePorHoraHoje();
 }

 dashboard.put("volumePorHora", volumePorHora);
 return dashboard;
 }

 public java.util.Map<String, java.util.Map<String, java.util.Map<String, Integer>>> obterHistoricoPorDia() throws Exception {
 return sistema.obterHistoricoPorDia();
 }

 public void registarLinha(String id, String nome) throws Exception {
 sistema.registarLinha(id, nome);
 }

 public void adicionarParagemALinha(String idLinha, String idParagem) throws Exception {
 sistema.adicionarParagemALinha(idLinha, idParagem);
 }

 public void associarAutocarroALinha(String autocarroId, String linhaId) throws Exception {
 sistema.associarAutocarroALinha(autocarroId, linhaId);
 }

 public Map<String, Object> obterDadosCorrelacao(String dataInicio, String dataFim) throws Exception {
 if (dataInicio == null || dataInicio.isEmpty() || dataFim == null || dataFim.isEmpty()) {
 java.time.LocalDate hoje = java.time.LocalDate.now();
 dataFim = hoje.toString();
 dataInicio = hoje.minusDays(30).toString();
 }
 return sistema.obterDadosCorrelacao(dataInicio, dataFim);
 }

 public Map<String, Object> signupCliente(String nome, String email, String password) throws Exception {
 String id = java.util.UUID.randomUUID().toString();
 sistema.registarCliente(id, nome, email, password);
 return Map.of("id", id, "nome", nome, "email", email);
 }

 public Map<String, Object> loginCliente(String email, String password) throws Exception {
  return sistema.loginCliente(email, password)
  .map(c -> {
      List<Bilhete> bilhetes = new RepositorioBilhetes().listarPorCliente(c.getId());
      List<Map<String, Object>> compras = bilhetes.stream().map(b -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", b.getId());
          map.put("tipo", b.getTipo());
          map.put("nomeTipo", b.getNomeTipo());
          map.put("dataCompra", b.getDataCompra().toString());
          map.put("dataValidade", b.getDataValidade().toString());
          map.put("estado", b.getEstado());
          map.put("preco", b.getPreco());
          return map;
      }).toList();

      Map<String, Object> map = new HashMap<>();
      map.put("id", c.getId());
      map.put("nome", c.getNome());
      map.put("email", c.getEmail());
      map.put("nif", c.getNif() != null ? c.getNif() : "--- --- ---");
      map.put("passeMensal", c.isPasseMensal());
      map.put("definicoes", Map.of(
          "tema", c.getTema(),
          "notificacoesAtivas", c.isNotificacoesAtivas()
      ));
      map.put("linhasFavoritas", c.getLinhasFavoritas());
      map.put("compras", compras);
      return map;
  })
  .orElseThrow(() -> new Exception("Credenciais incorretas. Não foi possível iniciar sessão. Sugerimos que crie uma conta nova caso ainda não o tenha feito."));
  }

  public Map<String, Object> obterPerfilCliente(String id) throws Exception {
  return sistema.procurarClientePorId(id)
  .map(c -> {
      List<Bilhete> bilhetes = new RepositorioBilhetes().listarPorCliente(c.getId());
      List<Map<String, Object>> compras = bilhetes.stream().map(b -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", b.getId());
          map.put("tipo", b.getTipo());
          map.put("nomeTipo", b.getNomeTipo());
          map.put("dataCompra", b.getDataCompra().toString());
          map.put("dataValidade", b.getDataValidade().toString());
          map.put("estado", b.getEstado());
          map.put("preco", b.getPreco());
          return map;
      }).toList();

      Map<String, Object> map = new HashMap<>();
      map.put("id", c.getId());
      map.put("nome", c.getNome());
      map.put("email", c.getEmail());
      map.put("nif", c.getNif() != null ? c.getNif() : "--- --- ---");
      map.put("passeMensal", c.isPasseMensal());
      map.put("definicoes", Map.of(
          "tema", c.getTema(),
          "notificacoesAtivas", c.isNotificacoesAtivas()
      ));
      map.put("linhasFavoritas", c.getLinhasFavoritas());
      map.put("compras", compras);
      return map;
  })
  .orElseThrow(() -> new Exception("Cliente não encontrado"));
  }

 public boolean loginAdmin(String email, String password) {
 return sistema.loginAdmin(email, password);
 }

 public List<String> listarParagens() {
 return sistema.obterTodasParagens();
 }

 public List<Map<String, Object>> listarAlertasRecentes(int limite) {
 return sistema.obterAlertasRecentes(limite).stream().map(a -> {
 Map<String, Object> map = new java.util.HashMap<>();
 map.put("autocarroId", a.getAutocarroId());
 map.put("tipo", a.getTipo().toString());
 map.put("mensagem", a.getMensagem());
 map.put("timestamp", a.getTimestamp().toString());
 return map;
 }).toList();
 }

 public List<Map<String, Object>> listarLinhas() {
 return sistema.obterTodasLinhas().stream().map(l -> {
 Map<String, Object> map = new java.util.HashMap<>();
 map.put("id", l.getId());
 map.put("nome", l.getNome());
 return map;
 }).toList();
 }

  public Map<String, Object> atualizarPerfilCliente(String id, AtualizarPerfilDTO dto) throws Exception {
  var cliente = sistema.procurarClientePorId(id)
  .orElseThrow(() -> new Exception("Cliente não encontrado"));
  
  if (dto.getNif() != null) {
  cliente.setNif(dto.getNif());
  }
  if (dto.getPasseMensal() != null) {
  cliente.setPasseMensal(dto.getPasseMensal());
  }
  if (dto.getTema() != null) {
  cliente.setTema(dto.getTema());
  }
  if (dto.getNotificacoesAtivas() != null) {
  cliente.setNotificacoesAtivas(dto.getNotificacoesAtivas());
  }
  if (dto.getLinhasFavoritas() != null) {
  cliente.setLinhasFavoritas(dto.getLinhasFavoritas());
  }
  // Persistir alterações
  sistema.atualizarCliente(cliente);
  
  List<Bilhete> bilhetes = new RepositorioBilhetes().listarPorCliente(cliente.getId());
  List<Map<String, Object>> compras = bilhetes.stream().map(b -> {
      Map<String, Object> map = new HashMap<>();
      map.put("id", b.getId());
      map.put("tipo", b.getTipo());
      map.put("nomeTipo", b.getNomeTipo());
      map.put("dataCompra", b.getDataCompra().toString());
      map.put("dataValidade", b.getDataValidade().toString());
      map.put("estado", b.getEstado());
      map.put("preco", b.getPreco());
      return map;
  }).toList();

  Map<String, Object> map = new HashMap<>();
  map.put("id", cliente.getId());
  map.put("nome", cliente.getNome());
  map.put("email", cliente.getEmail());
  map.put("nif", cliente.getNif() != null ? cliente.getNif() : "--- --- ---");
  map.put("passeMensal", cliente.isPasseMensal());
  map.put("definicoes", Map.of(
      "tema", cliente.getTema(),
      "notificacoesAtivas", cliente.isNotificacoesAtivas()
  ));
  map.put("linhasFavoritas", cliente.getLinhasFavoritas());
  map.put("compras", compras);
  return map;
  }

 public boolean verificarConexaoBD() {
 try {
 var conn = pt.uminho.dai.pgu.p7_data.DatabaseConnection.obterConexao();
 boolean valid = conn.isValid(2);
 conn.close();
 return valid;
 } catch (Exception e) {
 return false;
 }
 }
}