package pt.uminho.dai.pgu.business.operacao_tempo_real;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;

public enum TipoAlerta {
 OCUPACAO_ACIMA_DO_LIMIAR,
 LEITURA_ANOMALA_ENTRADA,
 LEITURA_ANOMALA_SAIDA,
 LEITURA_INCONSISTENTE,
 AUSENCIA_DE_LEITURAS
}