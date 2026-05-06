package pt.uminho.dai.pgu.models;
import pt.uminho.dai.pgu.repositories.*;
import pt.uminho.dai.pgu.services.*;

public enum TipoAlerta {
    OCUPACAO_ACIMA_DO_LIMIAR,
    LEITURA_ANOMALA_ENTRADA,
    LEITURA_ANOMALA_SAIDA,
    LEITURA_INCONSISTENTE,
    AUSENCIA_DE_LEITURAS
}
