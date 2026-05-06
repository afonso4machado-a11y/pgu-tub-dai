package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AdicionarParagemDTO {
    @NotBlank(message = "Paragem é obrigatória.")
    @Size(min = 2, max = 120, message = "Nome da paragem deve ter entre 2 e 120 caracteres.")
    @Pattern(regexp = "^[A-Za-zÀ-ú0-9\\s'\\-]+$", message = "Nome da paragem inválido.")
    private String paragem;

    public String getParagem() { return paragem; }
    public void setParagem(String paragem) { this.paragem = paragem; }
}
