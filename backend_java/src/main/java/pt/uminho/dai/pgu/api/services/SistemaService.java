package pt.uminho.dai.pgu.api.services;

import pt.uminho.dai.pgu.core.Alerta;
import pt.uminho.dai.pgu.core.Autocarro;
import pt.uminho.dai.pgu.core.Sistema;
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
            map.put("ultimaLeitura", a.getUltimaLeitura() != null ? a.getUltimaLeitura().toString() : "N/A");
            return map;
        }).toList();
    }

    public Map<String, Object> obterDadosDashboard() throws Exception {
        return sistema.obterDadosDashboard();
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
}
