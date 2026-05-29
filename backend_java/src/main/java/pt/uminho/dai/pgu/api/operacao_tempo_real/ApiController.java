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

import pt.uminho.dai.pgu.api.acessos_configuracao.AdminLoginDTO;
import pt.uminho.dai.pgu.api.analitica_historico.SistemaService;
import pt.uminho.dai.pgu.business.operacao_tempo_real.Alerta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.concurrent.ConcurrentHashMap;


import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

 private final SistemaService sistemaService;

 public ApiController(SistemaService sistemaService) {
 this.sistemaService = sistemaService;
 }

 /**
 * REGISTO DE AUTOCARRO — Validação Zero-Trust
 * Usa @Valid para acionar as anotações do DTO antes de qualquer lógica de negócio.
 * Se o input falhar, o ExceptionHandler abaixo retorna HTTP 400 com erros detalhados.
 */
 @PostMapping("/autocarros")
 @CacheEvict(value = "dashboardCache", allEntries = true)
 public ResponseEntity<Map<String, String>> registarAutocarro(@Valid @RequestBody RegistarAutocarroDTO dto) {
 try {
 sistemaService.registarAutocarro(
 dto.getId(),
 dto.getCapacidade(),
 dto.getMatricula(),
 dto.getMarca(),
 dto.getModelo()
 );
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Autocarro registado com sucesso!"));
 } catch (Exception e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 /**
 * Handler global para erros de validação do @Valid.
 * Retorna HTTP 400 com lista de todos os campos inválidos e respetivas mensagens.
 */
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
 Map<String, Object> response = new LinkedHashMap<>();
 response.put("status", "erro");
 response.put("mensagem", "Dados de entrada inválidos. Verifique os campos assinalados.");
 
 Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
 .collect(Collectors.toMap(
 FieldError::getField,
 FieldError::getDefaultMessage,
 (existing, replacement) -> existing // manter o primeiro erro por campo
 ));
 
 response.put("erros", fieldErrors);
 return ResponseEntity.badRequest().body(response);
 }

 @PostMapping("/leituras")
 @CacheEvict(value = "dashboardCache", allEntries = true)
 public ResponseEntity<Map<String, Object>> registarLeituras(@Valid @RequestBody RegistarLeituraDTO dto) {
 try {
 List<Alerta> alertas = sistemaService.registarLeituras(dto.getId(), dto.getEntradas(), dto.getSaidas());

 Map<String, Object> response = new HashMap<>();
 response.put("status", "sucesso");
 response.put("alertas", alertas);
 return ResponseEntity.ok(response);
 } catch (Exception e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 @GetMapping("/autocarros/{id}")
 public ResponseEntity<Map<String, Object>> consultarAutocarro(@PathVariable String id) {
 try {
 Map<String, Object> data = sistemaService.consultarAutocarro(id);
 Map<String, Object> response = new HashMap<>(data);
 response.put("status", "sucesso");
 return ResponseEntity.ok(response);
 } catch (Exception e) {
 return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 @GetMapping("/autocarros")
 public ResponseEntity<Map<String, Object>> listarAutocarros() {
 try {
 List<Map<String, Object>> lista = sistemaService.listarAutocarros();
 return ResponseEntity.ok(Map.of("status", "sucesso", "autocarros", lista));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 /** Lista autocarros marcados como eliminados (para visualização + restauro no backoffice) */
 @GetMapping("/autocarros/eliminados")
 public ResponseEntity<Map<String, Object>> listarAutocarrosEliminados() {
 try {
 List<Map<String, Object>> lista = sistemaService.listarAutocarrosEliminados();
 return ResponseEntity.ok(Map.of("status", "sucesso", "autocarros", lista));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 /** Soft delete — autocarro marcado como eliminado, dados preservados */
 @DeleteMapping("/autocarros/{id}")
 @CacheEvict(value = "dashboardCache", allEntries = true)
 public ResponseEntity<Map<String, String>> eliminarAutocarro(@PathVariable String id) {
 try {
 sistemaService.eliminarAutocarro(id);
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Autocarro eliminado (reversível). Use /restaurar para desfazer."));
 } catch (java.util.NoSuchElementException e) {
 return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 } catch (IllegalStateException e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 /** Restaura um autocarro previamente eliminado */
 @PostMapping("/autocarros/{id}/restaurar")
 @CacheEvict(value = "dashboardCache", allEntries = true)
 public ResponseEntity<Map<String, String>> restaurarAutocarro(@PathVariable String id) {
 try {
 sistemaService.restaurarAutocarro(id);
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Autocarro restaurado com sucesso."));
 } catch (java.util.NoSuchElementException e) {
 return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 } catch (IllegalStateException e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

  @GetMapping("/dashboard")
  @Cacheable(value = "dashboardCache", key = "'globalStats'")
  public ResponseEntity<Map<String, Object>> dashboard() {
    try {
      System.out.println("[DB Guard] Cache MISS. Executando leitura pesada no PostgreSQL/PostGIS...");
      
      // Garantia de Thread-Safety na memória partilhada do Spring
      Map<String, Object> response = new ConcurrentHashMap<>();
      Map<String, Object> stats = sistemaService.obterDadosDashboard();
      
      // Converter dados obtidos para uma estrutura thread-safe se necessário
      Map<String, Object> threadSafeStats = new ConcurrentHashMap<>(stats);
      
      response.put("status", "sucesso");
      response.put("dashboard", threadSafeStats);
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      // Retorno de erro sanitizado sem expor detalhes internos do Postgres ou Tomcat
      Map<String, Object> errorMap = new ConcurrentHashMap<>();
      errorMap.put("status", "erro");
      errorMap.put("mensagem", "Falha temporária ao consolidar dados operacionais de frota.");
      return ResponseEntity.status(500).body(errorMap);
    }
  }

 @GetMapping("/historico")
 public ResponseEntity<Map<String, Object>> historico() {
 try {
 return ResponseEntity.ok(Map.of("status", "sucesso", "historico", sistemaService.obterHistoricoPorDia()));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)e.getMessage()));
 }
 }

 @PostMapping("/linhas")
 public ResponseEntity<Map<String, String>> registarLinha(@Valid @RequestBody RegistarLinhaDTO dto) {
 try {
 sistemaService.registarLinha(dto.getId(), dto.getNome());
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Linha resolvida"));
 } catch (Exception e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 @PostMapping("/linhas/{id}/paragens")
 public ResponseEntity<Map<String, String>> adicionarParagem(@PathVariable String id,
 @Valid @RequestBody AdicionarParagemDTO dto) {
 try {
 sistemaService.adicionarParagemALinha(dto.getLinhaId() != null ? dto.getLinhaId() : id, dto.getParagem());
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Paragem inserida com sucesso"));
 } catch (Exception e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 @PostMapping("/linhas/{id}/autocarros")
 @CacheEvict(value = "dashboardCache", allEntries = true)
 public ResponseEntity<Map<String, String>> associarAuto(@PathVariable String id,
 @Valid @RequestBody AssociarAutocarroDTO dto) {
 try {
 sistemaService.associarAutocarroALinha(dto.getAutocarroId(), dto.getLinhaId() != null ? dto.getLinhaId() : id);
 return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Associação concluida."));
 } catch (Exception e) {
 return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 @GetMapping("/correlacao")
 public ResponseEntity<Map<String, Object>> correlacao(
 @RequestParam(defaultValue = "") String dataInicio,
 @RequestParam(defaultValue = "") String dataFim) {
 try {
 Map<String, Object> dados = sistemaService.obterDadosCorrelacao(dataInicio, dataFim);
 Map<String, Object> response = new HashMap<>();
 response.put("status", "sucesso");
 response.put("correlacao", dados);
 return ResponseEntity.ok(response);
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)e.getMessage()));
 }
 }

  @PostMapping("/auth/signup")
  public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupPassengerDTO dto) {
  try {
  Map<String, Object> user = sistemaService.signupCliente(
  dto.getNome(), 
  dto.getEmail(), 
  dto.getPassword()
  );
  return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
  } catch (Exception e) {
  return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
  }
  }

  @GetMapping("/auth/profile/{id}")
  public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String id) {
  try {
  Map<String, Object> user = sistemaService.obterPerfilCliente(id);
  return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
  } catch (Exception e) {
  return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", e.getMessage()));
  }
  }

  @PostMapping("/auth/login")
  public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginPassengerDTO dto) {
  try {
  Map<String, Object> user = sistemaService.loginCliente(
  dto.getEmail(), 
  dto.getPassword()
  );
  return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
  } catch (Exception e) {
  return ResponseEntity.status(401).body(Map.of("status", "erro", "mensagem", e.getMessage()));
  }
  }

 @PostMapping("/auth/admin/login")
 public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody AdminLoginDTO payload) {
 boolean ok = sistemaService.loginAdmin(payload.getEmail(), payload.getPassword());
 if (ok) {
 return ResponseEntity.ok(Map.of("status", "sucesso", "admin", true));
 } else {
 return ResponseEntity.status(401).body(Map.of("status", "erro", "mensagem", "Acesso restrito. Verifique as credenciais @uminho.pt."));
 }
 }

 @GetMapping("/paragens")
 public ResponseEntity<Map<String, Object>> listarParagens() {
 try {
 List<String> paragens = sistemaService.listarParagens();
 return ResponseEntity.ok(Map.of("status", "sucesso", "paragens", paragens));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 // ALERTAS ENDPOINT (Passenger App) 

 @GetMapping("/alertas")
 public ResponseEntity<Map<String, Object>> listarAlertas(
 @RequestParam(defaultValue = "20") int limite) {
 try {
 List<Map<String, Object>> alertas = sistemaService.listarAlertasRecentes(limite);
 return ResponseEntity.ok(Map.of("status", "sucesso", "alertas", alertas));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 // LINHAS ENDPOINT 

 @GetMapping("/linhas")
 public ResponseEntity<Map<String, Object>> listarLinhas() {
 try {
 List<Map<String, Object>> linhas = sistemaService.listarLinhas();
 return ResponseEntity.ok(Map.of("status", "sucesso", "linhas", linhas));
 } catch (Exception e) {
 return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
 }
 }

 // PROFILE UPDATE 

  @PutMapping("/auth/profile/{id}")
  public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable String id,
  @Valid @RequestBody AtualizarPerfilDTO dto) {
  try {
  Map<String, Object> user = sistemaService.atualizarPerfilCliente(id, dto);
  return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
  } catch (Exception e) {
  return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
  }
  }

 // HEALTH CHECK (Monitorização) 

 @GetMapping("/health")
 public ResponseEntity<Map<String, Object>> health() {
 Map<String, Object> health = new HashMap<>();
 health.put("status", "UP");
 health.put("servico", "PGU/TUB Backend");
 health.put("versao", "5.0.0");
 health.put("timestamp", LocalDateTime.now().toString());
 health.put("componentes", Map.of(
 "api", "UP",
 "database", sistemaService.verificarConexaoBD() ? "UP" : "DOWN"
 ));
 return ResponseEntity.ok(health);
 }
}