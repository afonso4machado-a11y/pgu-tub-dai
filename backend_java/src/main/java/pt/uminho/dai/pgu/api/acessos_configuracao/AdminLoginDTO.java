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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminLoginDTO {
 @NotBlank(message = "Email é obrigatório.")
 @Pattern(regexp = "^(?:[A-Za-z0-9._%+-]+@uminho\\.pt|[A-Za-z0-9._%+-]+@um(?:\\.pt)?|tub_uminho26)$", message = "The string did not match the expected pattern. Acesso restrito a @uminho.pt ou tub_uminho26")
 private String email;

 @NotBlank(message = "Password é obrigatória.")
 @Pattern(regexp = "^[A-Za-z0-9_]{8,64}$", message = "Formato inválido. Potencial code injection bloqueado.")
 private String password;

 public String getEmail() { return email; }
 public void setEmail(String email) { this.email = email; }
 public String getPassword() { return password; }
 public void setPassword(String password) { this.password = password; }
}