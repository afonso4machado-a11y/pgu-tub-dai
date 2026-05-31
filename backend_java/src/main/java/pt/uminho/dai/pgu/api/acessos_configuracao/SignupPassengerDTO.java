package pt.uminho.dai.pgu.api.acessos_configuracao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para Registo do Passageiro (Signup) — Zero-Trust Input Validation.
 */
public class SignupPassengerDTO {
    @NotBlank(message = "O nome e obrigatorio.")
    @Size(max = 100, message = "O nome deve ter no maximo 100 caracteres.")
    @Pattern(regexp = "^[A-Za-z0-9\\u00C0-\\u00FF ']+$", message = "O nome contem caracteres invalidos.")
    private String nome;

    @NotBlank(message = "O email e obrigatorio.")
    @Email(message = "O email inserido nao e valido.")
    private String email;

    @NotBlank(message = "A password e obrigatoria.")
    @Size(min = 6, message = "A password deve ter no minimo 6 caracteres.")
    private String password;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
