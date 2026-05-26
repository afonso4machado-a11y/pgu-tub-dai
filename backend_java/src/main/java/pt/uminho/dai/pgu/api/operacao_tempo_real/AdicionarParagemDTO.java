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

public class AdicionarParagemDTO {
 @NotBlank(message = "ID da linha é obrigatório.")
 @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID da linha contém caracteres inválidos.")
 private String linhaId;

 @NotBlank(message = "Nome da paragem é obrigatório.")
 @Pattern(regexp = "^[A-Za-zÀ-ú0-9\\s'\\-]+$", message = "Nome da paragem contém caracteres inválidos.")
 private String paragem;

 public String getLinhaId() { return linhaId; }
 public void setLinhaId(String linhaId) { this.linhaId = linhaId; }

 public String getParagem() { return paragem; }
 public void setParagem(String paragem) { this.paragem = paragem; }
}