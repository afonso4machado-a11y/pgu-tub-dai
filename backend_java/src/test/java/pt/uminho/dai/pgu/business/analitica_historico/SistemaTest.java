package pt.uminho.dai.pgu.business.analitica_historico;

import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SistemaTest {

 private static Sistema criarSistemaEmMemoria(ThresholdsAlerta thresholds) {
 return new Sistema(
 new RepositorioAutocarros(true),
 new RepositorioClientes(true),
 new RepositorioLeituras(true),
 new RepositorioAlertas(true),
 new RepositorioClientesAlertas(true),
 thresholds);
 }

 @Test
 void deveGerarAlertaQuandoOcupacaoUltrapassaNoventaPorCento() {
 Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta(0.90, 7, Duration.ofMinutes(15)));
 sistema.registarAutocarro("BUS-1", 10);

 List<Alerta> alertas = sistema.receberLeitura("BUS-1", 9, 0, LocalDateTime.now());

 assertTrue(alertas.stream().anyMatch(alerta -> alerta.getTipo() == TipoAlerta.OCUPACAO_ACIMA_DO_LIMIAR));
 }

 @Test
 void deveGerarAlertaQuandoEntramSeteOuMaisPassageirosEmSimultaneo() {
 Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta());
 sistema.registarAutocarro("BUS-2", 50);

 List<Alerta> alertas = sistema.receberLeitura("BUS-2", 7, 0, LocalDateTime.now());

 assertTrue(alertas.stream().anyMatch(alerta -> alerta.getTipo() == TipoAlerta.LEITURA_ANOMALA_ENTRADA));
 }

 @Test
 void deveGerarAlertaQuandoSaemSeteOuMaisPassageirosEmSimultaneo() {
 Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta());
 sistema.registarAutocarro("BUS-3", 50);
 sistema.receberLeitura("BUS-3", 10, 0, LocalDateTime.now());

 List<Alerta> alertas = sistema.receberLeitura("BUS-3", 0, 7, LocalDateTime.now().plusMinutes(1));

 assertTrue(alertas.stream().anyMatch(alerta -> alerta.getTipo() == TipoAlerta.LEITURA_ANOMALA_SAIDA));
 }

 @Test
 void deveGerarAlertaQuandoExisteAusenciaDeLeiturasPorPeriodoProlongado() {
 Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta(0.90, 7, Duration.ofMinutes(15)));
 sistema.registarAutocarro("BUS-4", 50);
 LocalDateTime instanteInicial = LocalDateTime.now();
 sistema.receberLeitura("BUS-4", 1, 0, instanteInicial);

 List<Alerta> alertas = sistema.receberLeitura("BUS-4", 1, 0, instanteInicial.plusMinutes(20));

 assertTrue(alertas.stream().anyMatch(alerta -> alerta.getTipo() == TipoAlerta.AUSENCIA_DE_LEITURAS));
 }
}