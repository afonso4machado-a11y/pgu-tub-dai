package pt.uminho.dai.pgu.core;

import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.repositories.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

public class Sistema {
    private final RepositorioAutocarros repositorioAutocarros;
    private final RepositorioClientes repositorioClientes;
    private final RepositorioLeituras repositorioLeituras;
    private final RepositorioAlertas repositorioAlertas;
    private final RepositorioClientesAlertas repositorioClientesAlertas;
    private final RepositorioLinhas repositorioLinhas;
    private final RepositorioParagens repositorioParagens;
    private final RepositorioCorrelacao repositorioCorrelacao;
    private final ThresholdsAlerta thresholdsAlerta;

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
        this.thresholdsAlerta = thresholdsAlerta;
    }

    public void registarAutocarro(String id, int capacidadeMaxima) {
        registarAutocarro(id, capacidadeMaxima, null, null, null);
    }

    public void registarAutocarro(String id, int capacidadeMaxima, String matricula, String marca, String modelo) {
        repositorioAutocarros.guardar(new Autocarro(id, capacidadeMaxima, matricula, marca, modelo));
    }

    public void registarCliente(String id, String nome, String email, String password) {
        repositorioClientes.guardar(new Cliente(id, nome, email, password));
    }

    public void registarLinha(String id, String nome) {
        repositorioLinhas.guardar(new Linha(id, nome));
    }

    public java.util.Optional<Cliente> loginCliente(String email, String password) {
        return repositorioClientes.procurarPorEmail(email)
                .filter(c -> password.equals(c.getPassword()));
    }

    public java.util.Optional<Cliente> procurarClientePorId(String id) {
        return repositorioClientes.procurarPorId(id);
    }

    public boolean loginAdmin(String email, String password) {
        // 🛡️ DEPRECATED: Esta classe não deve ser usada. Use pt.uminho.dai.pgu.services.Sistema
        // A autenticação deve ser feita via variáveis de ambiente (Zero-Trust)
        String adminPassword = System.getenv("PGU_ADMIN_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            return false;
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
        return receberLeitura(autocarroId, entradas, saidas, LocalDateTime.now());
    }

    public List<Alerta> receberLeitura(String autocarroId, int entradas, int saidas, LocalDateTime timestamp) {
        Autocarro autocarro = repositorioAutocarros.procurarPorId(autocarroId)
                .orElseThrow(() -> new NoSuchElementException("Autocarro nao encontrado: " + autocarroId));

        LeituraContagem leitura = new LeituraContagem(entradas, saidas, timestamp);
        List<Alerta> alertas = autocarro.processarLeitura(leitura, thresholdsAlerta);

        repositorioLeituras.guardar(autocarroId, leitura);
        List<Long> alertaIds = repositorioAlertas.guardarTodos(alertas);

        // ⚡ OPTIMIZAÇÃO: Distribuição de alertas em thread separada para não bloquear a resposta HTTP
        if (!alertaIds.isEmpty()) {
            java.util.List<String> clienteIds = repositorioClientes.listarTodos().stream()
                .map(Cliente::getId).toList();
            // Executa em background (desacoplado da thread HTTP)
            new Thread(() -> {
                try {
                    repositorioClientesAlertas.guardarEmLote(clienteIds, alertaIds);
                } catch (Exception e) {
                    System.err.println("[ASYNC] Erro ao distribuir alertas: " + e.getMessage());
                }
            }, "alerta-distributor").start();
        }

        repositorioAutocarros.atualizarEstado(autocarro);
        notificarClientes(alertas);
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

    /**
     * Dashboard textual para consola (debugging).
     * ⚡ OPTIMIZADO: 1 única cópia de listarTodos(), 1 único loop.
     */
    public String obterDashboardAnalitico() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== DASHBOARD ANALITICO (TUB) ===\n");

        // ⚡ 1 única cópia da coleção (antes eram 4)
        java.util.Collection<Autocarro> frota = repositorioAutocarros.listarTodos();
        int totalAutocarros = frota.size();
        double somaTaxas = 0.0;
        int volumeTotalPassageiros = 0;
        java.util.List<Autocarro> criticos = new java.util.ArrayList<>();

        for (Autocarro a : frota) {
            somaTaxas += a.getTaxaOcupacao();
            volumeTotalPassageiros += a.getTotalPassageirosTransportados();
            if (a.getTaxaOcupacao() >= thresholdsAlerta.getLimiteOcupacao()) {
                criticos.add(a);
            }
        }

        double taxaOcupacaoMedia = totalAutocarros == 0 ? 0 : (somaTaxas / totalAutocarros) * 100;
        sb.append(String.format("-> Taxa de Ocupacao Media da Frota: %.2f%%\n", taxaOcupacaoMedia));
        sb.append("-> Volume Total de Passageiros Transportados: ").append(volumeTotalPassageiros).append("\n");

        sb.append("\n--- VOLUME DE PASSAGEIROS POR LINHA E HORA ---\n");
        for (Autocarro a : frota) {
            sb.append(" -> Veiculo/Linha: ").append(a.getId()).append("\n");
            java.util.Map<Integer, Integer> volumePorHora = a.obterVolumePorHora();
            if (volumePorHora.isEmpty()) {
                sb.append("    (Sem registos)\n");
            } else {
                for (java.util.Map.Entry<Integer, Integer> entry : volumePorHora.entrySet()) {
                    sb.append(String.format("    %02dh00 - %02dh59: %d passageiros\n",
                            entry.getKey(), entry.getKey(), entry.getValue()));
                }
            }
        }

        sb.append("\n--- VEICULOS EM LOTA\u00c7AO CRITICA ---\n");
        if (criticos.isEmpty()) {
            sb.append(" (Nenhum veiculo em estado critico neste momento)\n");
        } else {
            for (Autocarro a : criticos) {
                sb.append(String.format(" [!] %s (Lotacao a %.0f%%)\n",
                        a.getId(), a.getTaxaOcupacao() * 100));
            }
        }

        sb.append("=================================\n");
        return sb.toString();
    }

    /**
     * Dashboard JSON para o frontend.
     * ⚡ OPTIMIZADO: 1 única cópia, 1 único loop (antes: 2 cópias, 2 loops).
     */
    public java.util.Map<String, Object> obterDadosDashboard() {
        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();

        // ⚡ 1 única cópia snapshot da coleção
        java.util.Collection<Autocarro> frota = repositorioAutocarros.listarTodos();
        int totalAutocarros = frota.size();

        double somaTaxas = 0.0;
        int volumeTotalPassageiros = 0;
        java.util.List<java.util.Map<String, Object>> autocarrosCriticos = new java.util.ArrayList<>();

        // ⚡ 1 único loop para calcular tudo
        for (Autocarro a : frota) {
            somaTaxas += a.getTaxaOcupacao();
            volumeTotalPassageiros += a.getTotalPassageirosTransportados();

            if (a.getTaxaOcupacao() >= thresholdsAlerta.getLimiteOcupacao()) {
                autocarrosCriticos.add(java.util.Map.of(
                    "id", a.getId(),
                    "taxaOcupacao", a.getTaxaOcupacao() * 100
                ));
            }
        }

        java.util.List<java.util.Map<String, Object>> avisos = new java.util.ArrayList<>();
        for (Alerta alerta : repositorioAlertas.listarAlertasRecentes(15)) {
            avisos.add(java.util.Map.of(
                "autocarroId", alerta.getAutocarroId(),
                "mensagem", alerta.getMensagem(),
                "tipo", alerta.getTipo().toString(),
                "timestamp", alerta.getTimestamp().toString()
            ));
        }

        double taxaOcupacaoMedia = totalAutocarros == 0 ? 0 : (somaTaxas / totalAutocarros) * 100;

        dashboard.put("taxaOcupacaoMedia", taxaOcupacaoMedia);
        dashboard.put("volumeTotalPassageiros", volumeTotalPassageiros);
        dashboard.put("totalAutocarros", totalAutocarros);
        dashboard.put("autocarrosCriticos", autocarrosCriticos);
        dashboard.put("avisosRecentes", avisos);

        return dashboard;
    }

    public List<Autocarro> obterTodosAutocarros() {
        return new java.util.ArrayList<>(repositorioAutocarros.listarTodos());
    }

    public List<String> obterTodasParagens() {
        return repositorioParagens.listarTodas();
    }

    public java.util.Map<String, java.util.Map<String, java.util.Map<String, Integer>>> obterHistoricoPorDia() {
        return repositorioLeituras.obterHistoricoPorDia();
    }

    /**
     * UC 4.3 — Motor de Correlação.
     * Cruza dados de contagem de passageiros (leituras) com dados operacionais
     * (viagens GTFS) e bilhética simulada. Produz métricas de Procura vs Oferta.
     */
    public java.util.Map<String, Object> obterDadosCorrelacao(String dataInicio, String dataFim) {
        java.util.Map<String, Object> resultado = new java.util.LinkedHashMap<>();

        // ── 1. Contagem Real (Procura) ──
        java.util.List<java.util.Map<String, Object>> procuraPorLinha = 
            repositorioCorrelacao.obterProcuraPorLinha(dataInicio, dataFim);

        // ── 2. Oferta Planeada ──
        java.util.List<java.util.Map<String, Object>> ofertaPorLinha = 
            repositorioCorrelacao.obterOfertaPlaneada();

        // ── 3. Distribuição Horária ──
        java.util.List<java.util.Map<String, Object>> procuraPorHora = 
            repositorioCorrelacao.obterProcuraPorHora(dataInicio, dataFim);

        // ── 4. Bilhética Simulada ──
        java.util.Map<String, Integer> bilheticaSimulada = new java.util.LinkedHashMap<>();
        int totalEntradasGeral = procuraPorLinha.stream()
            .mapToInt(m -> (int) m.get("totalEntradas")).sum();

        if (totalEntradasGeral > 0) {
            bilheticaSimulada.put("Estudante", (int)(totalEntradasGeral * 0.35));
            bilheticaSimulada.put("Sénior", (int)(totalEntradasGeral * 0.20));
            bilheticaSimulada.put("Passe Normal", (int)(totalEntradasGeral * 0.30));
            bilheticaSimulada.put("Zapping", (int)(totalEntradasGeral * 0.15));
        }

        // ── 5. Métricas de Correlação Calculadas ──
        double ratioProcuraOferta = 0.0;
        int totalViagens = ofertaPorLinha.stream()
            .mapToInt(m -> (int) m.get("viagensProgramadas")).sum();
        if (totalViagens > 0) {
            ratioProcuraOferta = (double) totalEntradasGeral / totalViagens;
        }

        java.util.Map<String, Object> metricas = new java.util.LinkedHashMap<>();
        metricas.put("totalPassageirosContados", totalEntradasGeral);
        metricas.put("totalViagensProgramadas", totalViagens);
        metricas.put("ratioProcuraOferta", Math.round(ratioProcuraOferta * 100.0) / 100.0);
        metricas.put("periodoInicio", dataInicio);
        metricas.put("periodoFim", dataFim);

        // ── Montar resultado final ──
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
        return new java.util.ArrayList<>(repositorioLinhas.listarTodas());
    }

    public void atualizarCliente(Cliente cliente) {
        repositorioClientes.guardar(cliente);
    }
}