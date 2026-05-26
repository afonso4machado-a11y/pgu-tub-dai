package pt.uminho.dai.pgu.api.acessos_configuracao;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO para atualização do perfil de cliente — Zero-Trust Input Validation.
 * Previne Stored XSS ao forçar regex sobre todos os campos editáveis.
 */
public class AtualizarPerfilDTO {
 @Size(max = 15, message = "NIF deve ter no máximo 15 caracteres.")
 @Pattern(regexp = "^[0-9 ]{0,15}$", message = "NIF contém caracteres inválidos.")
 private String nif;

 private Boolean passeMensal;
 private String tema;
 private Boolean notificacoesAtivas;
 private List<String> linhasFavoritas;

 public String getNif() { return nif; }
 public void setNif(String nif) { this.nif = nif; }
 public Boolean getPasseMensal() { return passeMensal; }
 public void setPasseMensal(Boolean passeMensal) { this.passeMensal = passeMensal; }
 public String getTema() { return tema; }
 public void setTema(String tema) { this.tema = tema; }
 public Boolean getNotificacoesAtivas() { return notificacoesAtivas; }
 public void setNotificacoesAtivas(Boolean notificacoesAtivas) { this.notificacoesAtivas = notificacoesAtivas; }
 public List<String> getLinhasFavoritas() { return linhasFavoritas; }
 public void setLinhasFavoritas(List<String> linhasFavoritas) { this.linhasFavoritas = linhasFavoritas; }
}