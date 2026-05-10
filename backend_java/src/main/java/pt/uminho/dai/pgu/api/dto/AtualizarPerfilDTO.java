package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para atualização do perfil de cliente — Zero-Trust Input Validation.
 * Previne Stored XSS ao forçar regex sobre todos os campos editáveis.
 */
public class AtualizarPerfilDTO {
    @Size(max = 15, message = "NIF deve ter no máximo 15 caracteres.")
    @Pattern(regexp = "^[0-9 ]{0,15}$", message = "NIF contém caracteres inválidos.")
    private String nif;

    private Boolean passeMensal;

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }
    public Boolean getPasseMensal() { return passeMensal; }
    public void setPasseMensal(Boolean passeMensal) { this.passeMensal = passeMensal; }
}
