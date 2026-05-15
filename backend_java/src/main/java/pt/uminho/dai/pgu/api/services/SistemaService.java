package pt.uminho.dai.pgu.api.services;

import pt.uminho.dai.pgu.models.Alerta;
import pt.uminho.dai.pgu.models.Autocarro;
import pt.uminho.dai.pgu.services.Sistema;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SistemaService {

    private final Sistema sistema;

    public SistemaService(Sistema sistema) {
        this.sistema = sistema;
    }

    public void registarAutocarro(String id, int capacidade, String matricula, String marca, String modelo) throws Exception {
        sistema.registarAutocarro(id, capacidade, matricula, marca, modelo);
    }

    public List<Alerta> registarLeituras(String id, int entradas, int saidas) throws Exception {
        return sistema.receberLeitura(id, entradas, saidas);
    }

    public Map<String, Object> consultarAutocarro(String id) throws Exception {
        Autocarro a = sistema.obterAutocarro(id);
        return Map.of(
                "id", a.getId(),
                "matricula", a.getMatricula() != null ? a.getMatricula() : "N/A",
                "marca", a.getMarca() != null ? a.getMarca() : "N/A",
                "modelo", a.getModelo() != null ? a.getModelo() : "N/A",
                "passageirosAtuais", a.getPassageirosAtuais(),
                "capacidadeMaxima", a.getCapacidadeMaxima(),
                "taxaOcupacao", a.getTaxaOcupacao() * 100,
                "numeroAlertas", a.getHistoricoAlertas().size()
        );
    }

    public List<Map<String, Object>> listarAutocarros() {
        return sistema.obterTodosAutocarros().stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("matricula", a.getMatricula());
            map.put("marca", a.getMarca());
            map.put("modelo", a.getModelo());
            map.put("linhaId", a.getLinhaId() != null ? a.getLinhaId() : "N/A");
            map.put("ocupacao", a.getTaxaOcupacao() * 100);
            map.put("passageirosAtuais", a.getPassageirosAtuais());
            map.put("capacidadeMaxima", a.getCapacidadeMaxima());
            map.put("totalPassageirosTransportados", a.getTotalPassageirosTransportados());
            map.put("ultimaLeitura", a.getUltimaLeitura() != null ? a.getUltimaLeitura().toString() : "N/A");
            return map;
        }).toList();
    }

    public Map<String, Object> obterDadosDashboard() throws Exception {
        Map<String, Object> dashboard = new java.util.LinkedHashMap<>(sistema.obterDadosDashboard());

        // ── Simulação vs Real: volumePorHora ──
        boolean demoMode = "1".equals(System.getenv("PGU_DEMO_MODE"))
                        || "true".equalsIgnoreCase(System.getenv("PGU_DEMO_MODE"));

        List<Map<String, Object>> volumePorHora;
        if (demoMode) {
            // Dados fictícios com picos realistas (manhã + fim de tarde)
            int[][] picos = {
                {6, 45}, {7, 280}, {8, 520}, {9, 390}, {10, 210}, {11, 180},
                {12, 320}, {13, 285}, {14, 180}, {15, 170}, {16, 245},
                {17, 480}, {18, 390}, {19, 190}, {20, 75}, {21, 35}
            };
            volumePorHora = new java.util.ArrayList<>();
            for (int[] p : picos) {
                Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("hora", p[0]);
                row.put("passageiros", p[1]);
                volumePorHora.add(row);
            }
        } else {
            // Dados reais: GROUP BY HOUR() na BD para o dia atual
            volumePorHora = sistema.obterVolumePorHoraHoje();
        }

        dashboard.put("volumePorHora", volumePorHora);
        return dashboard;
    }

    public java.util.Map<String, java.util.Map<String, java.util.Map<String, Integer>>> obterHistoricoPorDia() throws Exception {
        return sistema.obterHistoricoPorDia();
    }

    public void registarLinha(String id, String nome) throws Exception {
        sistema.registarLinha(id, nome);
    }

    public void adicionarParagemALinha(String idLinha, String idParagem) throws Exception {
        sistema.adicionarParagemALinha(idLinha, idParagem);
    }

    public void associarAutocarroALinha(String autocarroId, String linhaId) throws Exception {
        sistema.associarAutocarroALinha(autocarroId, linhaId);
    }

    public Map<String, Object> obterDadosCorrelacao(String dataInicio, String dataFim) throws Exception {
        if (dataInicio == null || dataInicio.isEmpty() || dataFim == null || dataFim.isEmpty()) {
            java.time.LocalDate hoje = java.time.LocalDate.now();
            dataFim = hoje.toString();
            dataInicio = hoje.minusDays(30).toString();
        }
        return sistema.obterDadosCorrelacao(dataInicio, dataFim);
    }

    public Map<String, Object> signupCliente(String nome, String email, String password) throws Exception {
        String id = java.util.UUID.randomUUID().toString();
        sistema.registarCliente(id, nome, email, password);
        return Map.of("id", id, "nome", nome, "email", email);
    }

    public Map<String, Object> loginCliente(String email, String password) throws Exception {
        return sistema.loginCliente(email, password)
                .map(c -> Map.of(
                    "id", (Object)c.getId(), 
                    "nome", (Object)c.getNome(), 
                    "email", (Object)c.getEmail(),
                    "nif", c.getNif() != null ? c.getNif() : "--- --- ---",
                    "passeMensal", c.isPasseMensal()
                ))
                .orElseThrow(() -> new Exception("Credenciais inválidas"));
    }

    public Map<String, Object> obterPerfilCliente(String id) throws Exception {
        return sistema.procurarClientePorId(id)
                .map(c -> Map.of(
                    "id", (Object)c.getId(), 
                    "nome", (Object)c.getNome(), 
                    "email", (Object)c.getEmail(),
                    "nif", c.getNif() != null ? c.getNif() : "--- --- ---",
                    "passeMensal", c.isPasseMensal()
                ))
                .orElseThrow(() -> new Exception("Cliente não encontrado"));
    }

    public boolean loginAdmin(String email, String password) {
        return sistema.loginAdmin(email, password);
    }

    public List<String> listarParagens() {
        return sistema.obterTodasParagens();
    }

    public List<Map<String, Object>> listarAlertasRecentes(int limite) {
        return sistema.obterAlertasRecentes(limite).stream().map(a -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("autocarroId", a.getAutocarroId());
            map.put("tipo", a.getTipo().toString());
            map.put("mensagem", a.getMensagem());
            map.put("timestamp", a.getTimestamp().toString());
            return map;
        }).toList();
    }

    public List<Map<String, Object>> listarLinhas() {
        return sistema.obterTodasLinhas().stream().map(l -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", l.getId());
            map.put("nome", l.getNome());
            return map;
        }).toList();
    }

    public Map<String, Object> atualizarPerfilCliente(String id, Map<String, Object> dados) throws Exception {
        var cliente = sistema.procurarClientePorId(id)
                .orElseThrow(() -> new Exception("Cliente não encontrado"));
        
        if (dados.containsKey("nif")) {
            cliente.setNif((String) dados.get("nif"));
        }
        if (dados.containsKey("passeMensal")) {
            cliente.setPasseMensal((Boolean) dados.get("passeMensal"));
        }
        // Persistir alterações
        sistema.atualizarCliente(cliente);
        
        return Map.of(
            "id", cliente.getId(),
            "nome", cliente.getNome(),
            "email", cliente.getEmail(),
            "nif", cliente.getNif() != null ? cliente.getNif() : "--- --- ---",
            "passeMensal", cliente.isPasseMensal()
        );
    }

    public boolean verificarConexaoBD() {
        try {
            var conn = pt.uminho.dai.pgu.repositories.DatabaseConnection.obterConexao();
            boolean valid = conn.isValid(2);
            conn.close();
            return valid;
        } catch (Exception e) {
            return false;
        }
    }
}
