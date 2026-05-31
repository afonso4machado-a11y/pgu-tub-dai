package pt.uminho.dai.pgu.business.analitica_historico;

import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public class Sistema {
 private final RepositorioAutocarros repositorioAutocarros;
 private final RepositorioClientes repositorioClientes;
 private final RepositorioLeituras repositorioLeituras;
 private final RepositorioAlertas repositorioAlertas;
 private final RepositorioClientesAlertas repositorioClientesAlertas;
 private final RepositorioLinhas repositorioLinhas;
 private final RepositorioParagens repositorioParagens;
 private final RepositorioCorrelacao repositorioCorrelacao;
 private final RepositorioPlaneamento repositorioPlaneamento;
 private final ThresholdsAlerta thresholdsAlerta;

 private Map<String, Object> dashboardCache = null;
 private long lastDashboardCacheTime = 0;
 private static final long DASHBOARD_CACHE_TTL_MS = 30000;

 public Sistema() {
 this(new ThresholdsAlerta());
 }

 public Sistema(ThresholdsAlerta thresholdsAlerta) {
 this.repositorioAutocarros = new RepositorioAutocarros();
 this.repositorioClientes = new RepositorioClientes();
 this.repositorioLeituras = new RepositorioLeituras();
 this.repositorioAlertas = new RepositorioAlertas();
 this.repositorioClientesAlertas = new RepositorioClientesAlertas();
 this.repositorioLinhas = new RepositorioLinhas();
 this.repositorioParagens = new RepositorioParagens();
 this.repositorioCorrelacao = new RepositorioCorrelacao();
 this.repositorioPlaneamento = new RepositorioPlaneamento();
 this.thresholdsAlerta = thresholdsAlerta;
 }

 // Construtor para testes — usa repositórios em memória sem BD
 protected Sistema(RepositorioAutocarros repAutocarros, RepositorioClientes repClientes,
 RepositorioLeituras repLeituras, RepositorioAlertas repAlertas,
 RepositorioClientesAlertas repClientesAlertas, ThresholdsAlerta thresholdsAlerta) {
 this.repositorioAutocarros = repAutocarros;
 this.repositorioClientes = repClientes;
 this.repositorioLeituras = repLeituras;
 this.repositorioAlertas = repAlertas;
 this.repositorioClientesAlertas = repClientesAlertas;
 this.repositorioLinhas = new RepositorioLinhas();
 this.repositorioParagens = new RepositorioParagens();
 this.repositorioCorrelacao = new RepositorioCorrelacao();
 this.repositorioPlaneamento = new RepositorioPlaneamento();
 this.thresholdsAlerta = thresholdsAlerta;
 }

 public void registarAutocarro(String id, int capacidadeMaxima) {
 registarAutocarro(id, capacidadeMaxima, null, null, null);
 }

 public void registarAutocarro(String id, int capacidadeMaxima, String matricula, String marca, String modelo) {
 repositorioAutocarros.guardar(new Autocarro(id, capacidadeMaxima, matricula, marca, modelo));
 }

 public void registarCliente(String id, String nome, String email, String password) {
 if (repositorioClientes.procurarPorEmail(email).isPresent()) {
 throw new IllegalArgumentException("Já existe uma conta com esse email.");
 }
 repositorioClientes.guardar(new Cliente(id, nome, email, password));
 }

 public void registarLinha(String id, String nome) {
 repositorioLinhas.guardar(new Linha(id, nome));
 }

 public Map<String, Object> planearViagem(String origem, String destino) {
     // A hora atual pode ser convertida para formato TIME do MySQL (HH:mm:ss)
     String horaAtual = LocalDateTime.now().toLocalTime().withNano(0).toString();
     return repositorioPlaneamento.encontrarProximaViagem(origem, destino, horaAtual);
 }

 public Optional<Cliente> loginCliente(String email, String password) {
 return repositorioClientes.procurarPorEmail(email)
 .filter(c -> password.equals(c.getPassword()));
 }

 public Optional<Cliente> procurarClientePorEmail(String email) {
 return repositorioClientes.procurarPorEmail(email);
 }

 public Optional<Cliente> procurarClientePorId(String id) {
 return repositorioClientes.procurarPorId(id);
 }

 public boolean loginAdmin(String email, String password) {
  // Sentinel: Falha segura se a variável de ambiente não existir
  String adminPassword = pt.uminho.dai.pgu.data.DatabaseConnection.getEnv("PGU_ADMIN_PASSWORD", "");
  if (adminPassword == null || adminPassword.isBlank()) {
  return false; // Fail-safe: Bloqueia login se a variável não estiver segura no ambiente
  }

 if (email == null || password == null) {
 return false;
 }

 String normalizedEmail = email.trim().toLowerCase();
 return (normalizedEmail.endsWith("@uminho.pt") || normalizedEmail.endsWith("@um"))
 && java.security.MessageDigest.isEqual(
 adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
 password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }

 public void adicionarParagemALinha(String linhaId, String nomeParagem) {
 Linha linha = repositorioLinhas.procurarPorId(linhaId)
 .orElseThrow(() -> new NoSuchElementException("Linha nao encontrada: " + linhaId));
 linha.adicionarParagem(new Paragem(nomeParagem));
 }

 public void associarAutocarroALinha(String autocarroId, String linhaId) {
 Autocarro autocarro = obterAutocarro(autocarroId);
 Linha linha = repositorioLinhas.procurarPorId(linhaId)
 .orElseThrow(() -> new NoSuchElementException("Linha nao encontrada: " + linhaId));
 autocarro.setLinhaId(linha.getId());
 repositorioAutocarros.atualizarEstado(autocarro);
 }

  public List<Alerta> receberLeitura(String autocarroId, int entradas, int saidas) {
    return receberLeitura(autocarroId, entradas, saidas, LocalDateTime.now(), null);
  }

  public List<Alerta> receberLeitura(String autocarroId, int entradas, int saidas, String tipoPassageiro) {
    return receberLeitura(autocarroId, entradas, saidas, LocalDateTime.now(), tipoPassageiro);
  }

  public List<Alerta> receberLeitura(String autocarroId, int entradas, int saidas, LocalDateTime timestamp) {
    return receberLeitura(autocarroId, entradas, saidas, timestamp, null);
  }

  public List<Alerta> receberLeitura(String autocarroId, int entradas, int saidas, LocalDateTime timestamp, String tipoPassageiro) {
    Autocarro autocarro = repositorioAutocarros.procurarPorId(autocarroId)
        .orElseThrow(() -> new NoSuchElementException("Autocarro nao encontrado: " + autocarroId));

    LeituraContagem leitura = new LeituraContagem(entradas, saidas, timestamp, tipoPassageiro);
    List<Alerta> alertas = autocarro.processarLeitura(leitura, thresholdsAlerta);

    repositorioLeituras.guardar(autocarroId, leitura);
    List<Long> alertaIds = repositorioAlertas.guardarTodos(alertas);

    List<String> clienteIds = new ArrayList<>();
    for (Cliente cliente : repositorioClientes.listarTodos()) {
        clienteIds.add(cliente.getId());
    }
    repositorioClientesAlertas.guardarEmLote(clienteIds, alertaIds);

    repositorioAutocarros.atualizarEstado(autocarro);
    notificarClientes(alertas);
    dashboardCache = null; // Invalida cache de dashboard
    return alertas;
  }

 public Autocarro obterAutocarro(String autocarroId) {
 return repositorioAutocarros.procurarPorId(autocarroId)
 .orElseThrow(() -> new NoSuchElementException("Autocarro nao encontrado: " + autocarroId));
 }

 private void notificarClientes(List<Alerta> alertas) {
 if (alertas.isEmpty())
 return;
 for (Cliente cliente : repositorioClientes.listarTodos()) {
 cliente.receberAlertas(alertas);
 }
 }

 public String obterDashboardAnalitico() {
 StringBuilder sb = new StringBuilder();
 sb.append("\n=== DASHBOARD ANALITICO (TUB) ===\n");

 double somaTaxas = 0.0;
 int volumeTotalPassageiros = 0;
 int totalAutocarros = repositorioAutocarros.listarTodos().size();

 for (Autocarro a : repositorioAutocarros.listarTodos()) {
 somaTaxas += a.getTaxaOcupacao();
 volumeTotalPassageiros += a.getTotalPassageirosTransportados();
 }

 double taxaOcupacaoMedia = totalAutocarros == 0 ? 0 : (somaTaxas / totalAutocarros) * 100;
 sb.append(String.format("-> Taxa de Ocupacao Media da Frota: %.2f%%\n", taxaOcupacaoMedia));
 sb.append("-> Volume Total de Passageiros Transportados: ").append(volumeTotalPassageiros).append("\n");

 sb.append("\n--- VOLUME DE PASSAGEIROS POR LINHA E HORA ---\n");
 Map<String, Map<Integer, Integer>> volumePorLinha = new java.util.TreeMap<>();

 for (Autocarro a : repositorioAutocarros.listarTodos()) {
 String linhaId = a.getLinhaId();
 String nomeLinha = "Sem Linha";
 if (linhaId != null) {
 Optional<Linha> linhaOpt = repositorioLinhas.procurarPorId(linhaId);
 if (linhaOpt.isPresent()) {
 nomeLinha = "Linha " + linhaOpt.get().getNome() + " (" + linhaId + ")";
 } else {
 nomeLinha = "Linha " + linhaId;
 }
 }

 volumePorLinha.putIfAbsent(nomeLinha, new java.util.TreeMap<>());
 Map<Integer, Integer> volumeDaLinha = volumePorLinha.get(nomeLinha);

 Map<Integer, Integer> volumeDoAutocarro = a.obterVolumePorHora();
 for (Map.Entry<Integer, Integer> entry : volumeDoAutocarro.entrySet()) {
 volumeDaLinha.put(entry.getKey(), volumeDaLinha.getOrDefault(entry.getKey(), 0) + entry.getValue());
 }
 }

 if (volumePorLinha.isEmpty()) {
 sb.append(" (Sem registos)\n");
 } else {
 for (Map.Entry<String, Map<Integer, Integer>> entryLinha : volumePorLinha.entrySet()) {
 sb.append(" -> ").append(entryLinha.getKey()).append("\n");
 Map<Integer, Integer> volumePorHora = entryLinha.getValue();
 if (volumePorHora.isEmpty()) {
 sb.append(" (Sem registos)\n");
 } else {
 for (Map.Entry<Integer, Integer> entry : volumePorHora.entrySet()) {
 sb.append(String.format(" %02dh00 - %02dh59: %d passageiros\n",
 entry.getKey(), entry.getKey(), entry.getValue()));
 }
 }
 }
 }

 sb.append("\n--- VEICULOS EM LOTAÇAO CRITICA ---\n");
 boolean temCriticos = false;
 for (Autocarro a : repositorioAutocarros.listarTodos()) {
 if (a.getTaxaOcupacao() >= thresholdsAlerta.getLimiteOcupacao()) {
 sb.append(String.format(" [!] %s (Lotacao a %.0f%%)\n",
 a.getId(), a.getTaxaOcupacao() * 100));
 temCriticos = true;
 }
 }
 if (!temCriticos) {
 sb.append(" (Nenhum veiculo em estado critico neste momento)\n");
 }

 sb.append("=================================\n");
 return sb.toString();
 }

 public synchronized Map<String, Object> obterDadosDashboard() {
 long now = System.currentTimeMillis();
 if (dashboardCache != null && (now - lastDashboardCacheTime) < DASHBOARD_CACHE_TTL_MS) {
 return dashboardCache;
 }

 Map<String, Object> dashboard = new HashMap<>();

 double somaTaxas = 0.0;
 int volumeTotalPassageiros = 0;
 int totalAutocarros = repositorioAutocarros.listarTodos().size();

 List<Map<String, Object>> autocarrosCriticos = new ArrayList<>();

 for (Autocarro a : repositorioAutocarros.listarTodos()) {
 somaTaxas += a.getTaxaOcupacao();
 volumeTotalPassageiros += a.getTotalPassageirosTransportados();

 if (a.getTaxaOcupacao() >= thresholdsAlerta.getLimiteOcupacao()) {
 Map<String, Object> critico = new HashMap<>();
 critico.put("id", a.getId());
 critico.put("taxaOcupacao", a.getTaxaOcupacao() * 100);
 autocarrosCriticos.add(critico);
 }
 }

 List<Map<String, Object>> avisos = new ArrayList<>();
 for (Alerta alerta : repositorioAlertas.listarAlertasRecentes(15)) {
 Map<String, Object> aviso = new HashMap<>();
 aviso.put("autocarroId", alerta.getAutocarroId());
 aviso.put("mensagem", alerta.getMensagem());
 aviso.put("tipo", alerta.getTipo().toString());
 aviso.put("timestamp", alerta.getTimestamp().toString());
 avisos.add(aviso);
 }

 double taxaOcupacaoMedia = totalAutocarros == 0 ? 0 : (somaTaxas / totalAutocarros) * 100;

 dashboard.put("taxaOcupacaoMedia", taxaOcupacaoMedia);
 dashboard.put("volumeTotalPassageiros", volumeTotalPassageiros);
 dashboard.put("totalAutocarros", totalAutocarros);
 dashboard.put("autocarrosCriticos", autocarrosCriticos);
 dashboard.put("avisosRecentes", avisos);

 dashboardCache = dashboard;
 lastDashboardCacheTime = now;
 return dashboard;
 }

 public List<Autocarro> obterTodosAutocarros() {
 return new ArrayList<>(repositorioAutocarros.listarTodos());
 }

 /**
  * Soft delete — marca o autocarro como eliminado. Dados históricos preservados.
  * @throws NoSuchElementException se o autocarro não existir
  * @throws IllegalStateException se já estiver eliminado
  */
 public void eliminarAutocarro(String id) {
 Autocarro autocarro = repositorioAutocarros.procurarPorIdIncluindoEliminados(id)
  .orElseThrow(() -> new NoSuchElementException("Autocarro nao encontrado: " + id));
 if (autocarro.isDeleted()) {
  throw new IllegalStateException("Autocarro ja se encontra eliminado: " + id);
 }
 repositorioAutocarros.marcarComoEliminado(id);
 dashboardCache = null; // Invalidar cache
 }

 /**
  * Restaura um autocarro previamente eliminado.
  * @throws NoSuchElementException se o autocarro não existir
  * @throws IllegalStateException se o autocarro não estiver eliminado
  */
 public void restaurarAutocarro(String id) {
 Autocarro autocarro = repositorioAutocarros.procurarPorIdIncluindoEliminados(id)
  .orElseThrow(() -> new NoSuchElementException("Autocarro nao encontrado: " + id));
 if (!autocarro.isDeleted()) {
  throw new IllegalStateException("Autocarro nao esta eliminado: " + id);
 }
 repositorioAutocarros.restaurar(id);
 dashboardCache = null; // Invalidar cache
 }

 /** Retorna a lista de autocarros eliminados (para visualização e restauro) */
 public List<Autocarro> obterAutocarrosEliminados() {
 return new ArrayList<>(repositorioAutocarros.listarEliminados());
 }

 public List<String> obterTodasParagens() {
 return repositorioParagens.listarTodas();
 }

 public List<Map<String, Object>> obterTodasParagensComCoordenadas() {
 return repositorioParagens.listarTodasComCoordenadas();
 }

 public Map<String, Map<String, Map<String, Integer>>> obterHistoricoPorDia() {
 return repositorioLeituras.obterHistoricoPorDia();
 }

 /**
 * UC 4.3 — Motor de Correlação.
 * Cruza dados de contagem de passageiros (leituras) com dados operacionais
 * (viagens GTFS) e bilhética simulada. Produz métricas de Procura vs Oferta.
 */
 public Map<String, Object> obterDadosCorrelacao(String dataInicio, String dataFim) {
 Map<String, Object> resultado = new LinkedHashMap<>();

  // 1. Contagem Real (Procura) 
  List<Map<String, Object>> procuraPorLinha = 
  repositorioCorrelacao.obterProcuraPorLinha(dataInicio, dataFim);

  int totalEntradasGeral = procuraPorLinha.stream()
      .mapToInt(m -> (int) m.get("totalEntradas")).sum();

 // 2. Oferta Planeada 
 List<Map<String, Object>> ofertaPorLinha = 
 repositorioCorrelacao.obterOfertaPlaneada();

 // 3. Distribuição Horária 
 List<Map<String, Object>> procuraPorHora = 
 repositorioCorrelacao.obterProcuraPorHora(dataInicio, dataFim);

  // 4. Bilhética Real (da Base de Dados) 
  Map<String, Integer> bilheticaSimulada = repositorioCorrelacao.obterBilheticaReal(dataInicio, dataFim);
  if (bilheticaSimulada.isEmpty()) {
    bilheticaSimulada.put("Estudante", 0);
    bilheticaSimulada.put("Sénior", 0);
    bilheticaSimulada.put("Passe Normal", 0);
    bilheticaSimulada.put("Zapping", 0);
  }

 // 5. Métricas de Correlação Calculadas 
 double ratioProcuraOferta = 0.0;
 int totalViagens = ofertaPorLinha.stream()
 .mapToInt(m -> (int) m.get("viagensProgramadas")).sum();
 if (totalViagens > 0) {
 ratioProcuraOferta = (double) totalEntradasGeral / totalViagens;
 }

 Map<String, Object> metricas = new LinkedHashMap<>();
 metricas.put("totalPassageirosContados", totalEntradasGeral);
 metricas.put("totalViagensProgramadas", totalViagens);
 metricas.put("ratioProcuraOferta", Math.round(ratioProcuraOferta * 100.0) / 100.0);
 metricas.put("periodoInicio", dataInicio);
 metricas.put("periodoFim", dataFim);

 // Montar resultado final 
 resultado.put("metricas", metricas);
 resultado.put("procuraPorLinha", procuraPorLinha);
 resultado.put("ofertaPorLinha", ofertaPorLinha);
 resultado.put("procuraPorHora", procuraPorHora);
 resultado.put("bilheticaSimulada", bilheticaSimulada);

 return resultado;
 }

 public List<Alerta> obterAlertasRecentes(int limite) {
 return repositorioAlertas.listarAlertasRecentes(limite);
 }

 public List<Linha> obterTodasLinhas() {
 return new ArrayList<>(repositorioLinhas.listarTodas());
 }

 public void atualizarCliente(Cliente cliente) {
 repositorioClientes.guardar(cliente);
 }

 /**
 * Retorna o volume de passageiros (entradas) agrupado por hora para o dia atual.
 * Delega ao RepositorioLeituras que faz GROUP BY HOUR() na BD.
 */
 public List<Map<String, Object>> obterVolumePorHoraHoje() {
 return repositorioLeituras.obterVolumePorHoraHoje();
 }
}