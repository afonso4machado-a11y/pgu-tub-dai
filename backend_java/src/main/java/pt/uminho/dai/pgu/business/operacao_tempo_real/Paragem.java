package pt.uminho.dai.pgu.business.operacao_tempo_real;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import java.util.Objects;
public class Paragem {
 private final String nome;
 public Paragem(String nome) { this.nome = Objects.requireNonNull(nome); }
 public String getNome() { return nome; }
}