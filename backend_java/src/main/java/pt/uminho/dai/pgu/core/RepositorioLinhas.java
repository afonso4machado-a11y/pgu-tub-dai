package pt.uminho.dai.pgu.core;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
public class RepositorioLinhas {
    private final Map<String, Linha> linhas = new LinkedHashMap<>();
    public RepositorioLinhas() {}
    public void guardar(Linha linha) { linhas.put(linha.getId(), linha); }
    public Optional<Linha> procurarPorId(String id) { return Optional.ofNullable(linhas.get(id)); }
    public Collection<Linha> listarTodas() { return linhas.values(); }
}
