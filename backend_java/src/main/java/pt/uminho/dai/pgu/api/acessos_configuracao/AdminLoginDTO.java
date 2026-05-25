package pt.uminho.dai.pgu.api.acessos_configuracao;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminLoginDTO {
    @NotBlank(message = "Email é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@uminho\\.pt$", message = "The string did not match the expected pattern. Acesso restrito a @uminho.pt")
    private String email;

    @NotBlank(message = "Password é obrigatória.")
    @Pattern(regexp = "^[A-Za-z0-9_]{8,64}$", message = "Formato inválido. Potencial code injection bloqueado.")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}