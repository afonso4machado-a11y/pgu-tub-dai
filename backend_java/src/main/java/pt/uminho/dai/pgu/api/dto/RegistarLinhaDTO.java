package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistarLinhaDTO {
    @NotBlank(message = "ID da linha é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID da linha inválido.")
    private String id;

    @NotBlank(message = "Nome da linha é obrigatório.")
    @Size(min = 2, max = 120, message = "Nome da linha deve ter entre 2 e 120 caracteres.")
    private String nome;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
