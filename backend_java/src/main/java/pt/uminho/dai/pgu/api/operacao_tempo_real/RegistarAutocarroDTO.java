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

import jakarta.validation.constraints.*;

/**
 * DTO de validação "Zero-Trust" para o registo de autocarros.
 * Todas as constraints são verificadas no servidor antes de qualquer
 * operação de persistência, independentemente da validação do frontend.
 *
 * @see pt.uminho.dai.pgu.api.operacao_tempo_real.ApiController#registarAutocarro
 */
public class RegistarAutocarroDTO {

 @NotBlank(message = "O identificador é obrigatório.")
 @Size(min = 3, max = 20, message = "O identificador deve ter entre 3 e 20 caracteres.")
 @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "O identificador só pode conter letras, números e hífenes.")
 private String id;

 @NotNull(message = "A capacidade (lotação) é obrigatória.")
 @Min(value = 1, message = "A capacidade deve ser no mínimo 1.")
 @Max(value = 200, message = "A capacidade máxima permitida é 200.")
 private Integer capacidade;

 @NotBlank(message = "A matrícula é obrigatória.")
 @Pattern(regexp = "^[A-Z0-9]{2}-[A-Z0-9]{2}-[A-Z0-9]{2}$",
 message = "Formato de matrícula inválido. Use XX-XX-XX (ex: 23-AB-45).")
 private String matricula;

 @NotBlank(message = "A marca é obrigatória.")
 @Size(min = 2, max = 30, message = "A marca deve ter entre 2 e 30 caracteres.")
 @Pattern(regexp = "^[A-Za-zÀ-ú\\s'\\-]+$", message = "A marca contém caracteres inválidos.")
 private String marca;

 @NotBlank(message = "O modelo é obrigatório.")
 @Size(min = 2, max = 30, message = "O modelo deve ter entre 2 e 30 caracteres.")
 @Pattern(regexp = "^[A-Za-zÀ-ú0-9\\s'\\-]+$", message = "O modelo contém caracteres inválidos.")
 private String modelo;

 // Getters & Setters 

 public String getId() { return id; }
 public void setId(String id) { this.id = id; }

 public Integer getCapacidade() { return capacidade; }
 public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

 public String getMatricula() { return matricula; }
 public void setMatricula(String matricula) { this.matricula = matricula; }

 public String getMarca() { return marca; }
 public void setMarca(String marca) { this.marca = marca; }

 public String getModelo() { return modelo; }
 public void setModelo(String modelo) { this.modelo = modelo; }
}