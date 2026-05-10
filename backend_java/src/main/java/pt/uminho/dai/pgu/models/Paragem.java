package pt.uminho.dai.pgu.models;
import pt.uminho.dai.pgu.repositories.*;
import pt.uminho.dai.pgu.services.*;
import java.util.Objects;
public class Paragem {
    private final String nome;
    public Paragem(String nome) { this.nome = Objects.requireNonNull(nome); }
    public String getNome() { return nome; }
}
