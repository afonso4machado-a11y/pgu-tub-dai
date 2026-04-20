package pt.uminho.dai.pgu.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Cliente {
    private final String id;
    private final String nome;
    private final List<Alerta> alertasRecebidos;

    public Cliente(String id, String nome) {
        this.id = Objects.requireNonNull(id);
        this.nome = Objects.requireNonNull(nome);
        this.alertasRecebidos = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void receberAlertas(List<Alerta> alertas) {
        alertasRecebidos.addAll(alertas);
    }

    public List<Alerta> getAlertasRecebidos() {
        return Collections.unmodifiableList(alertasRecebidos);
    }
}
