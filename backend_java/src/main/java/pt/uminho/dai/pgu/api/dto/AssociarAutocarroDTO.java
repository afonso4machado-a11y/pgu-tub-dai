package pt.uminho.dai.pgu.api.dto;

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
