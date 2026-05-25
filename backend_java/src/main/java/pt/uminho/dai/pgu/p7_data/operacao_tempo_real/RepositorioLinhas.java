package pt.uminho.dai.pgu.p7_data.operacao_tempo_real;

import pt.uminho.dai.pgu.p7_data.DatabaseConnection;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RepositorioLinhas {
    private final Map<String, Linha> linhas = new ConcurrentHashMap<>();

    public RepositorioLinhas() {}

    public void guardar(Linha linha) { 
        linhas.put(linha.getId(), linha); 
    }

    public Optional<Linha> procurarPorId(String id) { 
        return Optional.ofNullable(linhas.get(id)); 
    }

    public Collection<Linha> listarTodas() { 
        return new ArrayList<>(linhas.values()); 
    }
}