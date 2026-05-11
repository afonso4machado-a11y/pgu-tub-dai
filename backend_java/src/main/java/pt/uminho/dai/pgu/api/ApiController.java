package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.api.dto.RegistarAutocarroDTO;
import pt.uminho.dai.pgu.api.dto.AdminLoginDTO;
import pt.uminho.dai.pgu.api.dto.RegistarLeituraDTO;
import pt.uminho.dai.pgu.api.dto.RegistarLinhaDTO;
import pt.uminho.dai.pgu.api.dto.AdicionarParagemDTO;
import pt.uminho.dai.pgu.api.dto.AssociarAutocarroDTO;
import pt.uminho.dai.pgu.api.services.SistemaService;
import pt.uminho.dai.pgu.models.Alerta;
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
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
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
    public ResponseEntity<Map<String, Object>> registarLeituras(@Valid @RequestBody RegistarLeituraDTO dto) {
        try {
            List<Alerta> alertas = sistemaService.registarLeituras(dto.getId(), dto.getEntradas(), dto.getSaidas());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "sucesso");
            response.put("alertas", alertas);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
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
            e.printStackTrace();
            return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", "Recurso não encontrado."));
        }
    }

    @GetMapping("/autocarros")
    public ResponseEntity<Map<String, Object>> listarAutocarros() {
        try {
            List<Map<String, Object>> lista = sistemaService.listarAutocarros();
            return ResponseEntity.ok(Map.of("status", "sucesso", "autocarros", lista));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", "Erro interno no servidor."));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        try {
            return ResponseEntity.ok(Map.of("status", "sucesso", "dashboard", sistemaService.obterDadosDashboard()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)"Erro interno no servidor."));
        }
    }

    @GetMapping("/historico")
    public ResponseEntity<Map<String, Object>> historico() {
        try {
            return ResponseEntity.ok(Map.of("status", "sucesso", "historico", sistemaService.obterHistoricoPorDia()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)"Erro interno no servidor."));
        }
    }

    @PostMapping("/linhas")
    public ResponseEntity<Map<String, String>> registarLinha(@Valid @RequestBody RegistarLinhaDTO dto) {
        try {
            sistemaService.registarLinha(dto.getId(), dto.getNome());
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Linha resolvida"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
        }
    }

    @PostMapping("/linhas/{id}/paragens")
    public ResponseEntity<Map<String, String>> adicionarParagem(@PathVariable String id,
            @Valid @RequestBody AdicionarParagemDTO dto) {
        try {
            sistemaService.adicionarParagemALinha(dto.getLinhaId() != null ? dto.getLinhaId() : id, dto.getParagem());
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Paragem inserida com sucesso"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
        }
    }

    @PostMapping("/linhas/{id}/autocarros")
    public ResponseEntity<Map<String, String>> associarAuto(@PathVariable String id,
            @Valid @RequestBody AssociarAutocarroDTO dto) {
        try {
            sistemaService.associarAutocarroALinha(dto.getAutocarroId(), dto.getLinhaId() != null ? dto.getLinhaId() : id);
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Associação concluida."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
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
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", (Object)"erro", "mensagem", (Object)"Erro interno no servidor."));
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
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
        }
    }

    @GetMapping("/auth/profile/{id}")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable String id) {
        try {
            Map<String, Object> user = sistemaService.obterPerfilCliente(id);
            return ResponseEntity.ok(Map.of("status", "sucesso", "user", user));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(404).body(Map.of("status", "erro", "mensagem", "Recurso não encontrado."));
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
            e.printStackTrace();
            return ResponseEntity.status(401).body(Map.of("status", "erro", "mensagem", "Não autorizado."));
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
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", "Erro interno no servidor."));
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
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", "Erro interno no servidor."));
        }
    }

    // ═══ LINHAS ENDPOINT ═══

    @GetMapping("/linhas")
    public ResponseEntity<Map<String, Object>> listarLinhas() {
        try {
            List<Map<String, Object>> linhas = sistemaService.listarLinhas();
            return ResponseEntity.ok(Map.of("status", "sucesso", "linhas", linhas));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("status", "erro", "mensagem", "Erro interno no servidor."));
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
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", "Pedido inválido."));
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
