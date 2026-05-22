package pt.uminho.dai.pgu.models;
import pt.uminho.dai.pgu.repositories.*;
import pt.uminho.dai.pgu.services.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cliente {
    private final String id;
    private final String nome;
    private final String email;
    private String password;
    private String nif;
    private boolean passeMensal;
    private final List<Alerta> alertasRecebidos;

    public Cliente(String id, String nome, String email, String password) {
        this(id, nome, email, password, null, false);
    }

    public Cliente(String id, String nome, String email, String password, String nif, boolean passeMensal) {
        this.id = Objects.requireNonNull(id);
        this.nome = Objects.requireNonNull(nome);
        this.email = email;
        this.password = password;
        this.nif = nif;
        this.passeMensal = passeMensal;
        this.alertasRecebidos = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNif() {
        return nif;
    }

    public void setNif(String nif) {
        this.nif = nif;
    }

    public boolean isPasseMensal() {
        return passeMensal;
    }

    public void setPasseMensal(boolean passeMensal) {
        this.passeMensal = passeMensal;
    }

    public void receberAlertas(List<Alerta> alertas) {
        alertasRecebidos.addAll(alertas);
    }

    public List<Alerta> getAlertasRecebidos() {
        return Collections.unmodifiableList(alertasRecebidos);
    }
}
