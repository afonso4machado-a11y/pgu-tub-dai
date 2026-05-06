package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteLoginDTO {
    @NotBlank(message = "Email é obrigatório.")
    @Email(message = "Email inválido.")
    private String email;

    @NotBlank(message = "Password é obrigatória.")
    @Size(min = 8, max = 128, message = "Password com tamanho inválido.")
    private String password;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
