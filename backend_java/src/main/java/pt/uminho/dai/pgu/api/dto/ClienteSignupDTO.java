package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ClienteSignupDTO {
    @NotBlank(message = "Nome é obrigatório.")
    @Size(min = 2, max = 120, message = "Nome deve ter entre 2 e 120 caracteres.")
    @Pattern(regexp = "^[A-Za-zÀ-ú\\s'\\-]+$", message = "Nome contém caracteres inválidos.")
    private String nome;

    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    private String email;

    @NotBlank(message = "Password é obrigatória.")
    @Size(min = 10, max = 128, message = "Password deve ter entre 10 e 128 caracteres.")
    private String password;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
