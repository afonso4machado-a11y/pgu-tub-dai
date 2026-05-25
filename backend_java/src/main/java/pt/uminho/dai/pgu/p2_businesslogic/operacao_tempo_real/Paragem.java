package pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import java.util.Objects;
public class Paragem {
    private final String nome;
    public Paragem(String nome) { this.nome = Objects.requireNonNull(nome); }
    public String getNome() { return nome; }
}