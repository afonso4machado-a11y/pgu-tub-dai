package pt.uminho.dai.pgu.api.acessos_configuracao;

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

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.Bilhete;
import pt.uminho.dai.pgu.data.acessos_configuracao.RepositorioBilhetes;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller REST para pagamentos via Stripe.
 *
 * Endpoints:
 * POST /api/payments/create-intent — cria um PaymentIntent e devolve o clientSecret
 * POST /api/payments/webhook — recebe eventos Stripe (payment_intent.succeeded)
 * GET /api/payments/bilhetes/{clienteId} — lista bilhetes do cliente
 *
 * Variáveis de ambiente necessárias (Azure Key Vault):
 * STRIPE_SECRET_KEY — chave secreta sk_live_... ou sk_test_...
 * STRIPE_WEBHOOK_SECRET — whsec_... obtida no Stripe Dashboard
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

 private final RepositorioBilhetes repositorioBilhetes = new RepositorioBilhetes();
 private final Map<String, List<Long>> rateLimits = new java.util.concurrent.ConcurrentHashMap<>();

 private boolean isRateLimited(String identifier) {
  long now = System.currentTimeMillis();
  List<Long> timestamps = rateLimits.computeIfAbsent(identifier, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
  timestamps.removeIf(t -> now - t > 60000);
  if (timestamps.size() >= 5) {
   return true;
  }
  timestamps.add(now);
  return false;
 }

 /** Preços canónicos (fonte única de verdade — nunca confiar no frontend) */
 private static final Map<String, TicketConfig> TICKET_CATALOG = Map.of(
 "simples", new TicketConfig("Bilhete Simples", new BigDecimal("1.55"), 0),
 "passe", new TicketConfig("Passe Mensal", new BigDecimal("30.00"), 30)
 );

 @PostConstruct
 public void init() {
 String secretKey = pt.uminho.dai.pgu.data.DatabaseConnection.getEnv("STRIPE_SECRET_KEY", "");
 if (secretKey == null || secretKey.isBlank() || secretKey.startsWith("insira_aqui") || secretKey.startsWith("sk_test_example")) {
 System.out.println("[Stripe] STRIPE_SECRET_KEY nao configurada ou e um placeholder. A funcionalidade real de pagamentos estara indisponivel.");
 Stripe.apiKey = "";
 } else {
 Stripe.apiKey = secretKey;
 System.out.println("[Stripe] SDK inicializado com sucesso.");
 }
 }

 // 
 // 1. Criar PaymentIntent (chamado pelo frontend antes de mostrar o form)
 // 

 @PostMapping("/create-intent")
 public ResponseEntity<?> createPaymentIntent(
      HttpServletRequest request,
      @Valid @RequestBody CreateIntentRequest req) {
  
  // Rate-limiting check
  String ip = request.getRemoteAddr();
  if (isRateLimited(req.clienteId()) || isRateLimited(ip)) {
   return ResponseEntity.status(429)
     .body(Map.of("status", "erro", "mensagem", "Muitas tentativas de pagamento. Aguarde um minuto."));
  }

  // Validate client
  RepositorioClientes repoClientes = new RepositorioClientes();
  if (repoClientes.procurarPorId(req.clienteId()).isEmpty()) {
   return ResponseEntity.status(401)
     .body(Map.of("status", "erro", "mensagem", "Cliente inválido ou não encontrado."));
  }

  TicketConfig config = TICKET_CATALOG.get(req.tipoId());
  if (config == null) {
  return ResponseEntity.badRequest()
  .body(Map.of("status", "erro", "mensagem", "Tipo de bilhete invalido."));
  }

  try {
  PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
  .setAmount(config.preco().multiply(new BigDecimal("100")).longValue()) // centavos
  .setCurrency("eur")
  .putMetadata("tipo_id", req.tipoId())
  .putMetadata("nome_tipo", config.nomeTipo())
  .putMetadata("cliente_id", req.clienteId())
  .setAutomaticPaymentMethods(
  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
  .setEnabled(true)
  .build()
  )
  .build();

  PaymentIntent intent = PaymentIntent.create(params);

  return ResponseEntity.ok(Map.of(
  "clientSecret", intent.getClientSecret(),
  "paymentIntentId", intent.getId(),
  "amount", config.preco(),
  "nomeTipo", config.nomeTipo()
  ));
  } catch (StripeException e) {
  System.err.println("[Stripe] Erro ao criar PaymentIntent: " + e.getMessage());
  String msg = "Erro ao inicializar pagamento. Tente novamente.";
  if (e.getMessage() != null) {
  if (e.getMessage().contains("Invalid API Key")) {
  msg = "Erro: A Chave Secreta do Stripe nao e valida.";
  } else {
  // Retorna a mensagem original para facilitar debug
  msg = "Erro Stripe: " + e.getMessage().split(";")[0]; 
  }
  }
  return ResponseEntity.internalServerError()
  .body(Map.of("status", "erro", "mensagem", msg));
  }
  }

  //
  // 1.5. Simular Pagamento Fallback (Mock)
  //
  @PostMapping("/simulate")
  public ResponseEntity<?> simulatePayment(
      HttpServletRequest request,
      @Valid @RequestBody CreateIntentRequest req) {
  
  // Rate-limiting check
  String ip = request.getRemoteAddr();
  if (isRateLimited(req.clienteId()) || isRateLimited(ip)) {
   return ResponseEntity.status(429)
     .body(Map.of("status", "erro", "mensagem", "Muitas tentativas de pagamento. Aguarde um minuto."));
  }

  // Validate client
  RepositorioClientes repoClientes = new RepositorioClientes();
  if (repoClientes.procurarPorId(req.clienteId()).isEmpty()) {
   return ResponseEntity.status(401)
     .body(Map.of("status", "erro", "mensagem", "Cliente inválido ou não encontrado."));
  }

  // Validate ticket type
  TicketConfig config = TICKET_CATALOG.get(req.tipoId());
  if (config == null) {
   return ResponseEntity.badRequest()
     .body(Map.of("status", "erro", "mensagem", "Tipo de bilhete inválido."));
  }

  String mockIntentId = "mock_intent_" + UUID.randomUUID().toString().substring(0, 18);
  
  LocalDateTime now = LocalDateTime.now();
  LocalDateTime validade = config.diasValidade() > 0
      ? now.plusDays(config.diasValidade())
      : now.plusHours(2);
      
  Bilhete bilhete = new Bilhete(
      UUID.randomUUID().toString(),
      req.clienteId(),
      req.tipoId(),
      config.nomeTipo(),
      now,
      validade,
      "Ativo",
      config.preco(),
      mockIntentId
  );

  repositorioBilhetes.guardar(bilhete);
  System.out.println("[Simulado] Bilhete emitido: " + bilhete.getId() + " para cliente " + req.clienteId());

  return ResponseEntity.ok(Map.of(
      "status", "sucesso",
      "ticketId", bilhete.getId(),
      "paymentIntentId", mockIntentId,
      "amount", config.preco(),
      "nomeTipo", config.nomeTipo()
  ));
  }

 // 
 // 2. Webhook Stripe — payment_intent.succeeded → emite bilhete
 // 

 @PostMapping("/webhook")
 public ResponseEntity<String> stripeWebhook(
 HttpServletRequest request,
 @RequestBody String payload) throws IOException {

  String sigHeader = request.getHeader("Stripe-Signature");
  if (sigHeader == null || sigHeader.isBlank()) {
   System.err.println("[Stripe] Cabecalho Stripe-Signature em falta no webhook.");
   return ResponseEntity.status(400).body("Cabecalho de assinatura ausente.");
  }
 String webhookSecret = pt.uminho.dai.pgu.data.DatabaseConnection.getEnv("STRIPE_WEBHOOK_SECRET", "");

 if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.startsWith("insira_aqui") || webhookSecret.startsWith("whsec_example")) {
 System.err.println("[Stripe] STRIPE_WEBHOOK_SECRET nao configurado ou e um placeholder — webhook rejeitado.");
 return ResponseEntity.status(400).body("Webhook secret nao configurado ou invalido.");
 }

 Event event;
 try {
 event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
 } catch (SignatureVerificationException e) {
 System.err.println("[Stripe] Assinatura invalida no webhook: " + e.getMessage());
 return ResponseEntity.status(400).body("Assinatura invalida.");
 }

 if ("payment_intent.succeeded".equals(event.getType())) {
 PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
 .getObject().orElse(null);

 if (intent != null) {
 emitirBilhete(intent);
 }
 }

 return ResponseEntity.ok("OK");
 }

 // 
 // 3. Listar bilhetes de um cliente
 // 

 @GetMapping("/bilhetes/{clienteId}")
 public ResponseEntity<?> listarBilhetes(@PathVariable String clienteId) {
  // Validate client
  RepositorioClientes repoClientes = new RepositorioClientes();
  if (repoClientes.procurarPorId(clienteId).isEmpty()) {
   return ResponseEntity.status(401)
     .body(Map.of("status", "erro", "mensagem", "Cliente inválido ou não encontrado."));
  }
  
 List<Bilhete> bilhetes = repositorioBilhetes.listarPorCliente(clienteId);
 return ResponseEntity.ok(Map.of("status", "sucesso", "bilhetes", bilhetes));
 }

 // 
 // 4. Verificar estado de um PaymentIntent (anti-duplo-pagamento)
 // Usado pelo frontend ao carregar a página para detetar pagamentos
 // já concluídos — mesmo que o utilizador navegue para trás.
 // 

 @GetMapping("/check-intent/{paymentIntentId}")
 public ResponseEntity<?> checkPaymentIntent(@PathVariable String paymentIntentId) {
 // 1. Verificar primeiro na BD local (mais rápido, sem chamar Stripe)
 Optional<Bilhete> bilheteExistente = repositorioBilhetes.procurarPorPaymentIntent(paymentIntentId);
 if (bilheteExistente.isPresent()) {
 Bilhete b = bilheteExistente.get();
 return ResponseEntity.ok(Map.of(
 "status", "sucesso",
 "pago", true,
 "estado", b.getEstado(),
 "nomeTipo", b.getNomeTipo(),
 "preco", b.getPreco(),
 "dataCompra", b.getDataCompra().toString()
 ));
 }

  // Se for intenção simulada e não estiver na BD, não consultar Stripe
  if (paymentIntentId != null && paymentIntentId.startsWith("mock_intent_")) {
   return ResponseEntity.ok(Map.of(
       "status", "sucesso",
       "pago", false,
       "stripeStatus", "canceled"
   ));
  }

 // 2. Consultar Stripe diretamente (fonte de verdade)
 try {
 PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
 boolean succeeded = "succeeded".equals(intent.getStatus());

 if (succeeded) {
 // Emitir bilhete caso o webhook não o tenha feito ainda (resiliência)
 emitirBilhete(intent);
 Map<String, String> meta = intent.getMetadata();
 String nomeTipo = meta.getOrDefault("nome_tipo", "Bilhete");
 BigDecimal preco = new BigDecimal(intent.getAmount()).divide(new BigDecimal("100"));
 return ResponseEntity.ok(Map.of(
 "status", "sucesso",
 "pago", true,
 "estado", "Ativo",
 "nomeTipo", nomeTipo,
 "preco", preco
 ));
 }

 return ResponseEntity.ok(Map.of(
 "status", "sucesso",
 "pago", false,
 "stripeStatus", intent.getStatus()
 ));
 } catch (StripeException e) {
 System.err.println("[Stripe] Erro ao verificar PaymentIntent " + paymentIntentId + ": " + e.getMessage());
 return ResponseEntity.status(503).body(Map.of(
 "status", "erro",
 "mensagem", "Nao foi possivel verificar o estado do pagamento."
 ));
 }
 }

 // 
 // Helpers
 // 

 private void emitirBilhete(PaymentIntent intent) {
 // Idempotência: não duplicar bilhete para o mesmo PaymentIntent
 if (repositorioBilhetes.procurarPorPaymentIntent(intent.getId()).isPresent()) {
 System.out.println("[Stripe] Bilhete ja existe para PaymentIntent " + intent.getId());
 return;
 }

 Map<String, String> meta = intent.getMetadata();
 String tipoId = meta.getOrDefault("tipo_id", "simples");
 String nomeTipo = meta.getOrDefault("nome_tipo", "Bilhete Simples");
 String clienteId = meta.getOrDefault("cliente_id", "");

 TicketConfig config = TICKET_CATALOG.getOrDefault(tipoId,
 TICKET_CATALOG.get("simples"));

 LocalDateTime now = LocalDateTime.now();
 LocalDateTime validade = config.diasValidade() > 0
 ? now.plusDays(config.diasValidade())
 : now.plusHours(2); // Bilhete simples válido 2h

 Bilhete bilhete = new Bilhete(
 UUID.randomUUID().toString(),
 clienteId,
 tipoId,
 nomeTipo,
 now,
 validade,
 "Ativo",
 new BigDecimal(intent.getAmount()).divide(new BigDecimal("100")),
 intent.getId()
 );

 repositorioBilhetes.guardar(bilhete);
 System.out.println("[Stripe] Bilhete emitido: " + bilhete.getId()
 + " para cliente " + clienteId);
 }

 // 
 // DTOs e tipos auxiliares
 // 

 public record CreateIntentRequest(
 @NotBlank String tipoId,
 @NotBlank String clienteId
 ) {}

 private record TicketConfig(String nomeTipo, BigDecimal preco, int diasValidade) {}
}