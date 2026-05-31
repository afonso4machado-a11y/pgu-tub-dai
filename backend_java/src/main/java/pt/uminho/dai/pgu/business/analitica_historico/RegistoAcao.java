package pt.uminho.dai.pgu.business.analitica_historico;

import java.time.LocalDateTime;

/**
 * Domain model representing a backoffice worker action audit log.
 */
public class RegistoAcao {
    private int id;
    private String sessionId;
    private String utilizadorEmail;
    private String utilizadorNome;
    private String acaoTipo;
    private String detalhes;
    private LocalDateTime timestamp;

    public RegistoAcao() {
    }

    public RegistoAcao(int id, String sessionId, String utilizadorEmail, String utilizadorNome, String acaoTipo, String detalhes, LocalDateTime timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.utilizadorEmail = utilizadorEmail;
        this.utilizadorNome = utilizadorNome;
        this.acaoTipo = acaoTipo;
        this.detalhes = detalhes;
        this.timestamp = timestamp;
    }

    public RegistoAcao(String sessionId, String utilizadorEmail, String utilizadorNome, String acaoTipo, String detalhes, LocalDateTime timestamp) {
        this.sessionId = sessionId;
        this.utilizadorEmail = utilizadorEmail;
        this.utilizadorNome = utilizadorNome;
        this.acaoTipo = acaoTipo;
        this.detalhes = detalhes;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

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

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
