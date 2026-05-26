package pt.uminho.dai.pgu.data.operacao_tempo_real;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import pt.uminho.dai.pgu.data.acessos_configuracao.*;
import pt.uminho.dai.pgu.data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.data.analitica_historico.*;
import pt.uminho.dai.pgu.business.acessos_configuracao.*;
import pt.uminho.dai.pgu.business.operacao_tempo_real.*;
import pt.uminho.dai.pgu.business.analitica_historico.*;
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