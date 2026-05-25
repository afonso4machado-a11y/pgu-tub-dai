package pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de domínio para um Bilhete comprado via Stripe.
 */
public class Bilhete {

 private String id;
 private String clienteId;
 private String tipo; // 'simples' | 'passe'
 private String nomeTipo; // 'Bilhete Simples' | 'Passe Mensal'
 private LocalDateTime dataCompra;
 private LocalDateTime dataValidade;
 private String estado; // 'Ativo', 'Utilizado', 'Expirado'
 private BigDecimal preco;
 private String paymentIntentId;

 public Bilhete() {}

 public Bilhete(String id, String clienteId, String tipo, String nomeTipo,
 LocalDateTime dataCompra, LocalDateTime dataValidade,
 String estado, BigDecimal preco, String paymentIntentId) {
 this.id = id;
 this.clienteId = clienteId;
 this.tipo = tipo;
 this.nomeTipo = nomeTipo;
 this.dataCompra = dataCompra;
 this.dataValidade = dataValidade;
 this.estado = estado;
 this.preco = preco;
 this.paymentIntentId = paymentIntentId;
 }

 public String getId() { return id; }
 public void setId(String id) { this.id = id; }

 public String getClienteId() { return clienteId; }
 public void setClienteId(String clienteId) { this.clienteId = clienteId; }

 public String getTipo() { return tipo; }
 public void setTipo(String tipo) { this.tipo = tipo; }

 public String getNomeTipo() { return nomeTipo; }
 public void setNomeTipo(String nomeTipo) { this.nomeTipo = nomeTipo; }

 public LocalDateTime getDataCompra() { return dataCompra; }
 public void setDataCompra(LocalDateTime dataCompra) { this.dataCompra = dataCompra; }

 public LocalDateTime getDataValidade() { return dataValidade; }
 public void setDataValidade(LocalDateTime dataValidade) { this.dataValidade = dataValidade; }

 public String getEstado() { return estado; }
 public void setEstado(String estado) { this.estado = estado; }

 public BigDecimal getPreco() { return preco; }
 public void setPreco(BigDecimal preco) { this.preco = preco; }

 public String getPaymentIntentId() { return paymentIntentId; }
 public void setPaymentIntentId(String paymentIntentId) { this.paymentIntentId = paymentIntentId; }
}