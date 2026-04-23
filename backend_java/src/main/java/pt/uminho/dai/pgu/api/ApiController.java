package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.api.dto.RegistarAutocarroDTO;
import pt.uminho.dai.pgu.api.services.SistemaService;
import pt.uminho.dai.pgu.core.Alerta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final SistemaService sistemaService;

    public ApiController(SistemaService sistemaService) {
        this.sistemaService = sistemaService;
    }

    /**
     * 🧱 REGISTO DE AUTOCARRO — Validação Zero-Trust
     * Usa @Valid para acionar as anotações do DTO antes de qualquer lógica de negócio.
     * Se o input falhar, o ExceptionHandler abaixo retorna HTTP 400 com erros detalhados.
     */
    @PostMapping("/autocarros")
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
     * 🧱 Handler global para erros de validação do @Valid.
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
    public ResponseEntity<Map<String, Object>> registarLeituras(@RequestBody Map<String, String> payload) {
        try {
            int entradas = Integer.parseInt(payload.get("entradas"));
            int saidas = Integer.parseInt(payload.get("saidas"));
            List<Alerta> alertas = sistemaService.registarLeituras(payload.get("id"), entradas, saidas);

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

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        try {
            return ResponseEntity.ok(Map.of("status", "sucesso", "dashboard", sistemaService.obterDadosDashboard()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)e.getMessage()));
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
    public ResponseEntity<Map<String, String>> registarLinha(@RequestBody Map<String, String> payload) {
        try {
            sistemaService.registarLinha(payload.get("id"), payload.get("nome"));
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Linha resolvida"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/linhas/{id}/paragens")
    public ResponseEntity<Map<String, String>> adicionarParagem(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            sistemaService.adicionarParagemALinha(id, payload.get("paragem"));
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Paragem inserida com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/linhas/{id}/autocarros")
    public ResponseEntity<Map<String, String>> associarAuto(@PathVariable String id,
            @RequestBody Map<String, String> payload) {
        try {
            sistemaService.associarAutocarroALinha(payload.get("autocarroId"), id);
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
    public ResponseEntity<Map<String, Object>> signup(@RequestBody Map<String, String> payload) {
        try {
            Map<String, Object> user = sistemaService.signupCliente(
                payload.get("nome"), 
                payload.get("email"), 
                payload.get("password")
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
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) {
        try {
            Map<String, Object> user = sistemaService.loginCliente(
                payload.get("email"), 
                payload.get("password")
            );
            return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/auth/admin/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@RequestBody Map<String, String> payload) {
        boolean ok = sistemaService.loginAdmin(payload.get("email"), payload.get("password"));
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

    // ═══ ALERTAS ENDPOINT (Passenger App) ═══

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

    // ═══ LINHAS ENDPOINT ═══

    @GetMapping("/linhas")
    public ResponseEntity<Map<String, Object>> listarLinhas() {
        try {
            List<Map<String, Object>> linhas = sistemaService.listarLinhas();
            return ResponseEntity.ok(Map.of("status", "sucesso", "linhas", linhas));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    // ═══ PROFILE UPDATE ═══

    @PutMapping("/auth/profile/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable String id,
            @RequestBody Map<String, Object> payload) {
        try {
            Map<String, Object> user = sistemaService.atualizarPerfilCliente(id, payload);
            return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    // ═══ HEALTH CHECK (Monitorização) ═══

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
