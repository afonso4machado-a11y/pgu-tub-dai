package pt.uminho.dai.pgu.models;
import pt.uminho.dai.pgu.repositories.*;
import pt.uminho.dai.pgu.services.*;

import java.time.LocalDateTime;
import java.util.Objects;

public final class LeituraContagem {
    private final int entradas;
    private final int saidas;
    private final LocalDateTime timestamp;

    public LeituraContagem(int entradas, int saidas, LocalDateTime timestamp) {
        if (entradas < 0 || saidas < 0) {
            throw new IllegalArgumentException("As leituras nao podem ser negativas.");
        }
        this.entradas = entradas;
        this.saidas = saidas;
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    public int getEntradas() {
        return entradas;
    }

    public int getSaidas() {
        return saidas;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
