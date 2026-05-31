package pt.uminho.dai.pgu.api.operacao_tempo_real;

import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
import pt.uminho.dai.pgu.data.*;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.api.acessos_configuracao.*;
import pt.uminho.dai.pgu.api.operacao_tempo_real.*;
import pt.uminho.dai.pgu.api.analitica_historico.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;

public class RegistarLeituraDTO {
  @NotBlank(message = "ID do autocarro é obrigatório.")
  @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "ID do autocarro contém caracteres inválidos. Potencial code injection bloqueado.")
  private String id;

  @Min(value = 0, message = "Entradas não podem ser negativas.")
  @Max(value = 10000, message = "Entradas excedem o máximo permitido (10000).")
  private int entradas;

  @Min(value = 0, message = "Saídas não podem ser negativas.")
  @Max(value = 10000, message = "Saídas excedem o máximo permitido (10000).")
  private int saidas;

  @NotBlank(message = "Tipo de passageiro é obrigatório.")
  @Pattern(regexp = "^(Estudante|Sénior|Passe Normal|Avulso)$", message = "Tipo de passageiro inválido. Escolha entre Estudante, Sénior, Passe Normal ou Avulso.")
  private String tipoPassageiro;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public int getEntradas() { return entradas; }
  public void setEntradas(int entradas) { this.entradas = entradas; }

  public int getSaidas() { return saidas; }
  public void setSaidas(int saidas) { this.saidas = saidas; }

  public String getTipoPassageiro() { return tipoPassageiro; }
  public void setTipoPassageiro(String tipoPassageiro) { this.tipoPassageiro = tipoPassageiro; }
}