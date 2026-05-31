package pt.uminho.dai.pgu.api.acessos_configuracao;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
2.  * DTO para Login do Passageiro — Zero-Trust Input Validation.
3.  */
public class LoginPassengerDTO {
    @NotBlank(message = "O email e obrigatorio.")
    @Email(message = "O email inserido nao e valido.")
    private String email;

    @NotBlank(message = "A password e obrigatoria.")
    @Size(min = 6, message = "A password deve ter no minimo 6 caracteres.")
    private String password;

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
