package pt.uminho.dai.pgu.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegistarLeiturasDTO {
    @NotBlank(message = "ID do autocarro é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID do autocarro inválido.")
    private String id;

    @Min(value = 0, message = "Entradas não pode ser negativo.")
    @Max(value = 300, message = "Entradas acima do limite por leitura.")
    private int entradas;

    @Min(value = 0, message = "Saídas não pode ser negativo.")
    @Max(value = 300, message = "Saídas acima do limite por leitura.")
    private int saidas;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getEntradas() { return entradas; }
    public void setEntradas(int entradas) { this.entradas = entradas; }
    public int getSaidas() { return saidas; }
    public void setSaidas(int saidas) { this.saidas = saidas; }
}
