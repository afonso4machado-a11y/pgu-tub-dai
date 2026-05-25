package pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cliente {
 private final String id;
 private final String nome;
 private final String email;
 private String password;
 private String nif;
 private boolean passeMensal;
 private final List<Alerta> alertasRecebidos;
 private String tema = "dark";
 private boolean notificacoesAtivas = true;
 private final List<String> linhasFavoritas = new ArrayList<>();

 public Cliente(String id, String nome, String email, String password) {
 this(id, nome, email, password, null, false);
 }

 public Cliente(String id, String nome, String email, String password, String nif, boolean passeMensal) {
 this.id = Objects.requireNonNull(id);
 this.nome = Objects.requireNonNull(nome);
 this.email = email;
 this.password = password;
 this.nif = nif;
 this.passeMensal = passeMensal;
 this.alertasRecebidos = new ArrayList<>();
 // Default favorites for new accounts/fallback
 this.linhasFavoritas.add("L7");
 this.linhasFavoritas.add("L43");
 }

 public String getId() {
 return id;
 }

 public String getNome() {
 return nome;
 }

 public String getEmail() {
 return email;
 }

 public String getPassword() {
 return password;
 }

 public String getNif() {
 return nif;
 }

 public void setNif(String nif) {
 this.nif = nif;
 }

 public boolean isPasseMensal() {
 return passeMensal;
 }

 public void setPasseMensal(boolean passeMensal) {
 this.passeMensal = passeMensal;
 }

 public void receberAlertas(List<Alerta> alertas) {
 alertasRecebidos.addAll(alertas);
 }

 public List<Alerta> getAlertasRecebidos() {
  return Collections.unmodifiableList(alertasRecebidos);
  }

  public String getTema() { return tema; }
  public void setTema(String tema) { this.tema = tema; }
  public boolean isNotificacoesAtivas() { return notificacoesAtivas; }
  public void setNotificacoesAtivas(boolean notificacoesAtivas) { this.notificacoesAtivas = notificacoesAtivas; }
  public List<String> getLinhasFavoritas() { return new ArrayList<>(linhasFavoritas); }
  public void setLinhasFavoritas(List<String> linhasFavoritas) {
      this.linhasFavoritas.clear();
      if (linhasFavoritas != null) {
          this.linhasFavoritas.addAll(linhasFavoritas);
      }
  }
}