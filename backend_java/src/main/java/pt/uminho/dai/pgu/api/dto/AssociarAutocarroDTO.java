package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AssociarAutocarroDTO {
    @NotBlank(message = "ID do autocarro é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID do autocarro inválido.")
    private String autocarroId;

    public String getAutocarroId() { return autocarroId; }
    public void setAutocarroId(String autocarroId) { this.autocarroId = autocarroId; }
}
