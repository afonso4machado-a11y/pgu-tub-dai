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
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeveloperTest {

    private static Sistema criarSistemaEmMemoria(ThresholdsAlerta thresholds) {
        return new Sistema(
            new RepositorioAutocarros(true),
            new RepositorioClientes(true),
            new RepositorioLeituras(true),
            new RepositorioAlertas(true),
            new RepositorioClientesAlertas(true),
            thresholds
        );
    }

    @Test
    void deveLancarExcecaoAoAssociarAutocarroALinhaInexistente() {
        Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta());
        sistema.registarAutocarro("BUS-8", 50);

        assertThrows(NoSuchElementException.class, () -> {
            sistema.associarAutocarroALinha("BUS-8", "L-INEXISTENTE");
        });
    }

    @Test
    void deveLancarExcecaoAoObterAutocarroInexistente() {
        Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta());

        assertThrows(NoSuchElementException.class, () -> {
            sistema.obterAutocarro("BUS-INVALIDO");
        });
    }

    @Test
    void deveRegistarEObterAutocarroComSucesso() {
        Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta());

        sistema.registarAutocarro("BUS-6", 100);

        Autocarro autocarro = sistema.obterAutocarro("BUS-6");

        assertNotNull(autocarro);
        assertEquals("BUS-6", autocarro.getId());
        assertEquals(100, autocarro.getCapacidadeMaxima());
    }

    @Test
    void deveGerarAlertaQuandoOcupacaoUltrapassaNoventaPorCento() {
        Sistema sistema = criarSistemaEmMemoria(new ThresholdsAlerta(0.90, 15, Duration.ofMinutes(15)));
        sistema.registarAutocarro("BUS-1", 10);

        List<Alerta> alertas = sistema.receberLeitura("BUS-1", 9, 0, LocalDateTime.now());

        assertEquals(1, alertas.size());

        assertTrue(alertas.stream().anyMatch(alerta -> alerta.getTipo() == TipoAlerta.OCUPACAO_ACIMA_DO_LIMIAR));
    }
}