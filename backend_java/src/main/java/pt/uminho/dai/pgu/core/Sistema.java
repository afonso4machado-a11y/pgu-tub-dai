package pt.uminho.dai.pgu.core;

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
        this.thresholdsAlerta = thresholdsAlerta;
    }

    public void registarAutocarro(String id, int capacidadeMaxima) {
        registarAutocarro(id, capacidadeMaxima, null, null, null);
    }

    public void registarAutocarro(String id, int capacidadeMaxima, String matricula, String marca, String modelo) {
        repositorioAutocarros.guardar(new Autocarro(id, capacidadeMaxima, matricula, marca, modelo));
    }

    public void registarCliente(String id, String nome) {
        repositorioClientes.guardar(new Cliente(id, nome));
    }

    public void registarLinha(String id, String nome) {
        repositorioLinhas.guardar(new Linha(id, nome));
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

        for (Cliente cliente : repositorioClientes.listarTodos()) {
            repositorioClientesAlertas.guardar(cliente.getId(), alertaIds);
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
        for (Autocarro a : repositorioAutocarros.listarTodos()) {
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

    public java.util.Map<String, Object> obterDadosDashboard() {
        java.util.Map<String, Object> dashboard = new java.util.HashMap<>();

        double somaTaxas = 0.0;
        int volumeTotalPassageiros = 0;
        int totalAutocarros = repositorioAutocarros.listarTodos().size();

        java.util.List<java.util.Map<String, Object>> autocarrosCriticos = new java.util.ArrayList<>();

        for (Autocarro a : repositorioAutocarros.listarTodos()) {
            somaTaxas += a.getTaxaOcupacao();
            volumeTotalPassageiros += a.getTotalPassageirosTransportados();

            if (a.getTaxaOcupacao() >= thresholdsAlerta.getLimiteOcupacao()) {
                java.util.Map<String, Object> critico = new java.util.HashMap<>();
                critico.put("id", a.getId());
                critico.put("taxaOcupacao", a.getTaxaOcupacao() * 100);
                autocarrosCriticos.add(critico);
            }
        }

        java.util.List<java.util.Map<String, Object>> avisos = new java.util.ArrayList<>();
        for (Alerta alerta : repositorioAlertas.listarAlertasRecentes(15)) {
            java.util.Map<String, Object> aviso = new java.util.HashMap<>();
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

        return dashboard;
    }

    public List<Autocarro> obterTodosAutocarros() {
        return new java.util.ArrayList<>(repositorioAutocarros.listarTodos());
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

        // ── 1. Contagem Real (Procura) — dados das leituras por linha ──
        java.util.List<java.util.Map<String, Object>> procuraPorLinha = new java.util.ArrayList<>();
        String sqlProcura =
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

        try (java.sql.Connection conn = DatabaseConnection.obterConexao();
             java.sql.PreparedStatement ps = conn.prepareStatement(sqlProcura)) {
            ps.setString(1, dataInicio);
            ps.setString(2, dataFim);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("linhaId", rs.getString("linha_id"));
                    entry.put("totalEntradas", rs.getInt("total_entradas"));
                    entry.put("totalSaidas", rs.getInt("total_saidas"));
                    entry.put("diasComDados", rs.getInt("dias_com_dados"));
                    entry.put("totalLeituras", rs.getInt("total_leituras"));
                    procuraPorLinha.add(entry);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Erro na correlação (procura): " + e.getMessage());
        }

        // ── 2. Oferta Planeada — viagens programadas (GTFS) por linha ──
        java.util.List<java.util.Map<String, Object>> ofertaPorLinha = new java.util.ArrayList<>();
        String sqlOferta =
            "SELECT v.linha_id, v.tipo_dia, " +
            "COUNT(*) as viagens_programadas " +
            "FROM viagens v " +
            "GROUP BY v.linha_id, v.tipo_dia " +
            "ORDER BY v.linha_id";

        try (java.sql.Connection conn = DatabaseConnection.obterConexao();
             java.sql.PreparedStatement ps = conn.prepareStatement(sqlOferta);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                entry.put("linhaId", rs.getString("linha_id"));
                entry.put("tipoDia", rs.getString("tipo_dia"));
                entry.put("viagensProgramadas", rs.getInt("viagens_programadas"));
                ofertaPorLinha.add(entry);
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Erro na correlação (oferta): " + e.getMessage());
        }

        // ── 3. Distribuição Horária da Procura ──
        java.util.List<java.util.Map<String, Object>> procuraPorHora = new java.util.ArrayList<>();
        String sqlHoraria =
            "SELECT HOUR(l.timestamp) as hora, " +
            "SUM(l.entradas) as entradas, " +
            "SUM(l.saidas) as saidas " +
            "FROM leituras l " +
            "WHERE DATE(l.timestamp) BETWEEN ? AND ? " +
            "GROUP BY hora " +
            "ORDER BY hora";

        try (java.sql.Connection conn = DatabaseConnection.obterConexao();
             java.sql.PreparedStatement ps = conn.prepareStatement(sqlHoraria)) {
            ps.setString(1, dataInicio);
            ps.setString(2, dataFim);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> entry = new java.util.LinkedHashMap<>();
                    entry.put("hora", rs.getInt("hora"));
                    entry.put("entradas", rs.getInt("entradas"));
                    entry.put("saidas", rs.getInt("saidas"));
                    procuraPorHora.add(entry);
                }
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Erro na correlação (horária): " + e.getMessage());
        }

        // ── 4. Bilhética Simulada — Validações por tipo ──
        // Nota: dados simulados até integração real com API de bilhética (3.3)
        java.util.Map<String, Integer> bilheticaSimulada = new java.util.LinkedHashMap<>();
        bilheticaSimulada.put("Estudante", 0);
        bilheticaSimulada.put("Sénior", 0);
        bilheticaSimulada.put("Passe Normal", 0);
        bilheticaSimulada.put("Zapping", 0);

        // Distribuir as entradas reais pelos perfis de forma proporcional (simulação)
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
}