package pt.uminho.dai.pgu.business.operacao_tempo_real;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;

import java.time.Duration;

public class ThresholdsAlerta {
 private final double limiteOcupacao;
 private final int leiturasAnomalasSimultaneas;
 private final Duration limiteSemLeituras;

 public ThresholdsAlerta() {
 this(0.90, 7, Duration.ofMinutes(15));
 }

 public ThresholdsAlerta(double limiteOcupacao, int leiturasAnomalasSimultaneas, Duration limiteSemLeituras) {
 if (limiteOcupacao <= 0 || limiteOcupacao > 1) {
 throw new IllegalArgumentException("O limite de ocupacao deve estar entre 0 e 1.");
 }
 if (leiturasAnomalasSimultaneas <= 0) {
 throw new IllegalArgumentException("O limite de leituras anomalas deve ser superior a zero.");
 }
 this.limiteOcupacao = limiteOcupacao;
 this.leiturasAnomalasSimultaneas = leiturasAnomalasSimultaneas;
 this.limiteSemLeituras = limiteSemLeituras;
 }

 public double getLimiteOcupacao() {
 return limiteOcupacao;
 }

 public int getLeiturasAnomalasSimultaneas() {
 return leiturasAnomalasSimultaneas;
 }

 public Duration getLimiteSemLeituras() {
 return limiteSemLeituras;
 }
}