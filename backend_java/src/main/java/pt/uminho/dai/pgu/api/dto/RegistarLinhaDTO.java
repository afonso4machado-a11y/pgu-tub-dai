package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegistarLinhaDTO {
    @NotBlank(message = "ID da linha é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID da linha contém caracteres inválidos.")
    private String id;

    @NotBlank(message = "Nome da linha é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ú0-9\\s'\\-]+$", message = "Nome da linha contém caracteres inválidos.")
    private String nome;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
