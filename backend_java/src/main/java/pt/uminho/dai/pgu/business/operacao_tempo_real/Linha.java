package pt.uminho.dai.pgu.business.operacao_tempo_real;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
public class Linha {
 private final String id;
 private final String nome;
 private final List<Paragem> paragens;
 public Linha(String id, String nome) {
 this.id = Objects.requireNonNull(id);
 this.nome = Objects.requireNonNull(nome);
 this.paragens = new ArrayList<>();
 }
 public String getId() { return id; }
 public String getNome() { return nome; }
 public void adicionarParagem(Paragem paragem) { paragens.add(Objects.requireNonNull(paragem)); }
 public List<Paragem> getParagens() { return Collections.unmodifiableList(paragens); }
}