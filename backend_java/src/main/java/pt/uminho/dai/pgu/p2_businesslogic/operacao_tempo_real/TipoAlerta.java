package pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;

public enum TipoAlerta {
 OCUPACAO_ACIMA_DO_LIMIAR,
 LEITURA_ANOMALA_ENTRADA,
 LEITURA_ANOMALA_SAIDA,
 LEITURA_INCONSISTENTE,
 AUSENCIA_DE_LEITURAS
}