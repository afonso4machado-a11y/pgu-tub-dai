package pt.uminho.dai.pgu.api.analitica_historico;

import pt.uminho.dai.pgu.business.analitica_historico.RegistoAcao;
import pt.uminho.dai.pgu.data.analitica_historico.RepositorioAuditLogs;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller to handle worker audit logs securely.
 * Ensure there are no endpoints for updating, editing or deleting logs, keeping them immutable.
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final RepositorioAuditLogs repositorioAuditLogs = new RepositorioAuditLogs();

    /**
     * POST /api/audit-logs/batch
     * Securely writes a batch of worker logs. Zero-Trust payload validation is active.
     */
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> saveBatch(@Valid @RequestBody List<RegistoAcaoDTO> dtos) {
        try {
            List<RegistoAcao> logs = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            
            for (RegistoAcaoDTO dto : dtos) {
                LocalDateTime timestamp;
                try {
                    timestamp = LocalDateTime.parse(dto.getTimestamp(), formatter);
                } catch (Exception e) {
                    timestamp = LocalDateTime.now();
                }
                
                logs.add(new RegistoAcao(
                    dto.getSessionId(),
                    dto.getUtilizadorEmail(),
                    dto.getUtilizadorNome(),
                    dto.getAcaoTipo(),
                    dto.getDetalhes(),
                    timestamp
                ));
            }
            
            repositorioAuditLogs.guardarEmLote(logs);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "sucesso");
            response.put("mensagem", logs.size() + " registo(s) de auditoria guardado(s) com sucesso.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "erro");
            response.put("mensagem", "Erro ao processar lote de logs: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/audit-logs
     * Lists all logs. Requires a valid institutional email (@uminho.pt) passed through X-Admin-Email header.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listLogs(
            @RequestHeader(value = "X-Admin-Email", required = false) String adminEmail) {
        
        if (adminEmail == null || adminEmail.isBlank() || 
            (!adminEmail.trim().toLowerCase().endsWith("@uminho.pt") && !adminEmail.trim().toLowerCase().endsWith("@um"))) {
            return ResponseEntity.status(403).body(Map.of(
                "status", "erro",
                "mensagem", "Acesso restrito. Apenas administradores com email institucional (@uminho.pt) podem aceder a esta funcionalidade."
            ));
        }

        try {
            List<RegistoAcao> logs = repositorioAuditLogs.listarTodos();
            
            List<Map<String, Object>> logsList = logs.stream().map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", log.getId());
                map.put("sessionId", log.getSessionId());
                map.put("utilizadorEmail", log.getUtilizadorEmail());
                map.put("utilizadorNome", log.getUtilizadorNome());
                map.put("acaoTipo", log.getAcaoTipo());
                map.put("detalhes", log.getDetalhes());
                map.put("timestamp", log.getTimestamp().toString());
                return map;
            }).toList();

            return ResponseEntity.ok(Map.of(
                "status", "sucesso",
                "logs", logsList
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "erro",
                "mensagem", "Erro ao listar registos de auditoria: " + e.getMessage()
            ));
        }
    }
}
