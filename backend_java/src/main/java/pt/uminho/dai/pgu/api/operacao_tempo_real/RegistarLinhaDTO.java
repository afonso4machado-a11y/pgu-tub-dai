package pt.uminho.dai.pgu.api.operacao_tempo_real;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegistarLinhaDTO {
    @NotBlank(message = "ID da linha é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID da linha contém caracteres inválidos.")
    private String id;

    @NotBlank(message = "Nome da linha é obrigatório.")
    @Pattern(regexp = "^[A-Za-zÀ-ú0-9\\s'\\-]+$", message = "Nome da linha contém caracteres inválidos.")
    private String nome;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}