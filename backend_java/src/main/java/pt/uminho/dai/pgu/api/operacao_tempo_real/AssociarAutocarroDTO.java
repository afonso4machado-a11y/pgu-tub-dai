package pt.uminho.dai.pgu.api.operacao_tempo_real;

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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AssociarAutocarroDTO {
 @NotBlank(message = "ID do autocarro é obrigatório.")
 @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID do autocarro contém caracteres inválidos.")
 private String autocarroId;

 @NotBlank(message = "ID da linha é obrigatório.")
 @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID da linha contém caracteres inválidos.")
 private String linhaId;

 public String getAutocarroId() { return autocarroId; }
 public void setAutocarroId(String autocarroId) { this.autocarroId = autocarroId; }

 public String getLinhaId() { return linhaId; }
 public void setLinhaId(String linhaId) { this.linhaId = linhaId; }
}