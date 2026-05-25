package pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class Alerta {
 private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

 private final String autocarroId;
 private final TipoAlerta tipo;
 private final String mensagem;
 private final LocalDateTime timestamp;

 public Alerta(String autocarroId, TipoAlerta tipo, String mensagem, LocalDateTime timestamp) {
 this.autocarroId = Objects.requireNonNull(autocarroId);
 this.tipo = Objects.requireNonNull(tipo);
 this.mensagem = Objects.requireNonNull(mensagem);
 this.timestamp = Objects.requireNonNull(timestamp);
 }

 public String getAutocarroId() {
 return autocarroId;
 }

 public TipoAlerta getTipo() {
 return tipo;
 }

 public String getMensagem() {
 return mensagem;
 }

 public LocalDateTime getTimestamp() {
 return timestamp;
 }

 @Override
 public String toString() {
 return "[" + timestamp.format(FORMATADOR) + "] " + tipo + " - " + mensagem;
 }
}