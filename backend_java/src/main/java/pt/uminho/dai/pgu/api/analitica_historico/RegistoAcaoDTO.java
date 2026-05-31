package pt.uminho.dai.pgu.api.analitica_historico;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for validating incoming Audit Log requests.
 */
public class RegistoAcaoDTO {

    @NotBlank(message = "O ID de sessão é obrigatório.")
    @Size(max = 255, message = "O ID de sessão não pode exceder 255 caracteres.")
    private String sessionId;

    @NotBlank(message = "O email do utilizador é obrigatório.")
    @Email(message = "O email deve ser um endereço de email válido.")
    @Size(max = 255, message = "O email do utilizador não pode exceder 255 caracteres.")
    private String utilizadorEmail;

    @NotBlank(message = "O nome do utilizador é obrigatório.")
    @Size(max = 255, message = "O nome do utilizador não pode exceder 255 caracteres.")
    private String utilizadorNome;

    @NotBlank(message = "O tipo de ação é obrigatório.")
    @Size(max = 100, message = "O tipo de ação não pode exceder 100 caracteres.")
    private String acaoTipo;

    @NotBlank(message = "Os detalhes são obrigatórios.")
    private String detalhes;

    @NotBlank(message = "O timestamp é obrigatório.")
    private String timestamp;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUtilizadorEmail() { return utilizadorEmail; }
    public void setUtilizadorEmail(String utilizadorEmail) { this.utilizadorEmail = utilizadorEmail; }

    public String getUtilizadorNome() { return utilizadorNome; }
    public void setUtilizadorNome(String utilizadorNome) { this.utilizadorNome = utilizadorNome; }

    public String getAcaoTipo() { return acaoTipo; }
    public void setAcaoTipo(String acaoTipo) { this.acaoTipo = acaoTipo; }

    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
