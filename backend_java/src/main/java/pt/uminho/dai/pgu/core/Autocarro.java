package pt.uminho.dai.pgu.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Autocarro {
    // Limites de memória — prevenir OutOfMemoryError em operação prolongada
    private static final int MAX_HISTORICO_LEITURAS = 500;
    private static final int MAX_HISTORICO_ALERTAS = 200;

    private final String id;
    private final int capacidadeMaxima;
    private String matricula;
    private String marca;
    private String modelo;
    private int passageirosAtuais;
    private int totalPassageirosTransportados;
    private LocalDateTime ultimaLeitura;
    private final List<LeituraContagem> historicoLeituras;
    private final List<Alerta> historicoAlertas;
    private String linhaId;

    public Autocarro(String id, int capacidadeMaxima) {
        this(id, capacidadeMaxima, null, null, null);
    }

    public Autocarro(String id, int capacidadeMaxima, String matricula, String marca, String modelo) {
        if (capacidadeMaxima <= 0) {
            throw new IllegalArgumentException("A capacidade maxima tem de ser superior a zero.");
        }
        this.id = Objects.requireNonNull(id);
        this.capacidadeMaxima = capacidadeMaxima;
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.historicoLeituras = new ArrayList<>();
        this.historicoAlertas = new ArrayList<>();
    }
    public void setTotalPassageirosTransportados(int total) {
    this.totalPassageirosTransportados = total;
    }

    public void setUltimaLeitura(LocalDateTime ultimaLeitura) {
        this.ultimaLeitura = ultimaLeitura;
    }

    public void setPassageirosAtuais(int passageirosAtuais) {
        this.passageirosAtuais = passageirosAtuais;
    }

    public void setLinhaId(String linhaId) {
        this.linhaId = linhaId;
    }

    public String getLinhaId() {
        return linhaId;
    }

    public String getId() {
        return id;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public int getPassageirosAtuais() {
        return passageirosAtuais;
    }

    public int getTotalPassageirosTransportados() {
        return totalPassageirosTransportados;
    }

    public double getTaxaOcupacao() {
        return passageirosAtuais / (double) capacidadeMaxima;
    }

    public LocalDateTime getUltimaLeitura() {
        return ultimaLeitura;
    }

    public List<LeituraContagem> getHistoricoLeituras() {
        return Collections.unmodifiableList(historicoLeituras);
    }

    public List<Alerta> getHistoricoAlertas() {
        return Collections.unmodifiableList(historicoAlertas);
    }

    public List<Alerta> processarLeitura(LeituraContagem leitura, ThresholdsAlerta thresholds) {
        Objects.requireNonNull(leitura);
        Objects.requireNonNull(thresholds);

        this.totalPassageirosTransportados += leitura.getEntradas();

        List<Alerta> alertasGerados = new ArrayList<>();

        if (ultimaLeitura != null) {
            Duration intervalo = Duration.between(ultimaLeitura, leitura.getTimestamp());
            if (!intervalo.isNegative() && intervalo.compareTo(thresholds.getLimiteSemLeituras()) > 0) {
                alertasGerados.add(registarAlerta(
                        TipoAlerta.AUSENCIA_DE_LEITURAS,
                        "O autocarro " + id + " esteve " + intervalo.toMinutes()
                                + " minuto(s) sem leituras do sistema de contagem.",
                        leitura.getTimestamp()));
            }
        }

        if (leitura.getEntradas() >= thresholds.getLeiturasAnomalasSimultaneas()) {
            alertasGerados.add(registarAlerta(
                    TipoAlerta.LEITURA_ANOMALA_ENTRADA,
                    "Detetada leitura anomala no autocarro " + id + ": entraram "
                            + leitura.getEntradas() + " passageiros em simultaneo.",
                    leitura.getTimestamp()));
        }

        if (leitura.getSaidas() >= thresholds.getLeiturasAnomalasSimultaneas()) {
            alertasGerados.add(registarAlerta(
                    TipoAlerta.LEITURA_ANOMALA_SAIDA,
                    "Detetada leitura anomala no autocarro " + id + ": sairam "
                            + leitura.getSaidas() + " passageiros em simultaneo.",
                    leitura.getTimestamp()));
        }

        int ocupacaoCalculada = passageirosAtuais + leitura.getEntradas() - leitura.getSaidas();
        if (ocupacaoCalculada < 0) {
            alertasGerados.add(registarAlerta(
                    TipoAlerta.LEITURA_INCONSISTENTE,
                    "A leitura recebida para o autocarro " + id + " levaria a ocupacao negativa. "
                            + "O valor foi normalizado para zero.",
                    leitura.getTimestamp()));
            ocupacaoCalculada = 0;
        }

        passageirosAtuais = Math.min(ocupacaoCalculada, capacidadeMaxima);
        historicoLeituras.add(leitura);
        // Evitar crescimento ilimitado — manter só as N mais recentes
        if (historicoLeituras.size() > MAX_HISTORICO_LEITURAS) {
            historicoLeituras.subList(0, historicoLeituras.size() - MAX_HISTORICO_LEITURAS).clear();
        }
        ultimaLeitura = leitura.getTimestamp();

        if (getTaxaOcupacao() >= thresholds.getLimiteOcupacao()) {
            int percentagem = (int) Math.round(getTaxaOcupacao() * 100);
            alertasGerados.add(registarAlerta(
                    TipoAlerta.OCUPACAO_ACIMA_DO_LIMIAR,
                    "O autocarro " + id + " atingiu " + percentagem
                            + "% da capacidade (" + passageirosAtuais + "/" + capacidadeMaxima + ").",
                    leitura.getTimestamp()));
        }

        return alertasGerados;
    }

    private Alerta registarAlerta(TipoAlerta tipo, String mensagem, LocalDateTime timestamp) {
        Alerta alerta = new Alerta(id, tipo, mensagem, timestamp);
        historicoAlertas.add(alerta);
        if (historicoAlertas.size() > MAX_HISTORICO_ALERTAS) {
            historicoAlertas.subList(0, historicoAlertas.size() - MAX_HISTORICO_ALERTAS).clear();
        }
        return alerta;
    }

    //Método teste para ja pois nao temos linhas separadas de autocarros ou seja autocarro=linha
    public java.util.Map<Integer, Integer> obterVolumePorHora() {
        java.util.Map<Integer, Integer> volumePorHora = new java.util.TreeMap<>();
        for (LeituraContagem leitura : historicoLeituras) {
            int hora = leitura.getTimestamp().getHour();
            // Conta as entradas para a hora correspondente
            volumePorHora.put(hora, volumePorHora.getOrDefault(hora, 0) + leitura.getEntradas());
        }
        return volumePorHora;
    }

}
