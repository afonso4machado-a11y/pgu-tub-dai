package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.api.dto.RegistarAutocarroDTO;
import pt.uminho.dai.pgu.api.dto.RegistarLeiturasDTO;
import pt.uminho.dai.pgu.api.dto.RegistarLinhaDTO;
import pt.uminho.dai.pgu.api.dto.AdicionarParagemDTO;
import pt.uminho.dai.pgu.api.dto.AssociarAutocarroDTO;
import pt.uminho.dai.pgu.api.dto.ClienteSignupDTO;
import pt.uminho.dai.pgu.api.dto.ClienteLoginDTO;
import pt.uminho.dai.pgu.api.dto.AdminLoginDTO;
import pt.uminho.dai.pgu.api.dto.AtualizarPerfilDTO;
import pt.uminho.dai.pgu.api.services.SistemaService;
import pt.uminho.dai.pgu.core.Alerta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
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
    public ResponseEntity<Map<String, Object>> registarLeituras(@Valid @RequestBody RegistarLeiturasDTO payload) {
        try {
            List<Alerta> alertas = sistemaService.registarLeituras(payload.getId(), payload.getEntradas(), payload.getSaidas());

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
    public ResponseEntity<Map<String, String>> registarLinha(@Valid @RequestBody RegistarLinhaDTO payload) {
        try {
            sistemaService.registarLinha(payload.getId(), payload.getNome());
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Linha resolvida"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/linhas/{id}/paragens")
    public ResponseEntity<Map<String, String>> adicionarParagem(@PathVariable String id,
            @Valid @RequestBody AdicionarParagemDTO payload) {
        try {
            sistemaService.adicionarParagemALinha(id, payload.getParagem());
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Paragem inserida com sucesso"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }

    @PostMapping("/linhas/{id}/autocarros")
    public ResponseEntity<Map<String, String>> associarAuto(@PathVariable String id,
            @Valid @RequestBody AssociarAutocarroDTO payload) {
        try {
            sistemaService.associarAutocarroALinha(payload.getAutocarroId(), id);
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
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody ClienteSignupDTO payload) {
        try {
            Map<String, Object> user = sistemaService.signupCliente(
                payload.getNome(),
                payload.getEmail(),
                payload.getPassword()
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
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody ClienteLoginDTO payload) {
        try {
            Map<String, Object> user = sistemaService.loginCliente(
                payload.getEmail(),
                payload.getPassword()
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

    // ═══ ALERTAS ENDPOINT (Passenger App) ═══

    @GetMapping("/alertas")
    public ResponseEntity<Map<String, Object>> listarAlertas(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limite) {
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

    // ═══ PROFILE UPDATE (Hardened — DTO validado para prevenir Stored XSS) ═══

    @PutMapping("/auth/profile/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable @Pattern(regexp = "^[a-f0-9\\-]+$", message = "ID inválido.") String id,
            @Valid @RequestBody AtualizarPerfilDTO payload) {
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

    // ═══ TEMPORÁRIO — Reset da Base de Dados (protegido por password admin) ═══

    @PostMapping("/admin/reset")
    public ResponseEntity<Map<String, Object>> resetDatabase(@RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (!"tub_uminho26".equals(password)) {
            return ResponseEntity.status(403).body(Map.of("status", "erro", "mensagem", "Acesso negado."));
        }
        try {
            sistemaService.resetDadosOperacionais();
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Base de dados operacional limpa com sucesso."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
    }
}
