package pt.uminho.dai.pgu.core;
import java.util.Objects;
public class Paragem {
    private final String nome;
    public Paragem(String nome) { this.nome = Objects.requireNonNull(nome); }
    public String getNome() { return nome; }
}
