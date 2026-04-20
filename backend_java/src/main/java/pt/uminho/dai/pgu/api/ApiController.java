package pt.uminho.dai.pgu.api;

import pt.uminho.dai.pgu.api.services.SistemaService;
import pt.uminho.dai.pgu.core.Alerta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final SistemaService sistemaService;

    public ApiController(SistemaService sistemaService) {
        this.sistemaService = sistemaService;
    }

    @PostMapping("/autocarros")
    public ResponseEntity<Map<String, String>> registarAutocarro(@RequestBody Map<String, String> payload) {
        try {
            sistemaService.registarAutocarro(
                payload.get("id"), 
                Integer.parseInt(payload.get("capacidade")),
                payload.get("matricula"),
                payload.get("marca"),
                payload.get("modelo")
            );
            return ResponseEntity.ok(Map.of("status", "sucesso", "mensagem", "Autocarro registado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "erro", "mensagem", e.getMessage()));
        }
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
}
