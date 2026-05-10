package pt.uminho.dai.pgu.models;
import pt.uminho.dai.pgu.repositories.*;
import pt.uminho.dai.pgu.services.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
public class Linha {
    private final String id;
    private final String nome;
    private final List<Paragem> paragens;
    public Linha(String id, String nome) {
        this.id = Objects.requireNonNull(id);
        this.nome = Objects.requireNonNull(nome);
        this.paragens = new ArrayList<>();
    }
    public String getId() { return id; }
    public String getNome() { return nome; }
    public void adicionarParagem(Paragem paragem) { paragens.add(Objects.requireNonNull(paragem)); }
    public List<Paragem> getParagens() { return Collections.unmodifiableList(paragens); }
}
