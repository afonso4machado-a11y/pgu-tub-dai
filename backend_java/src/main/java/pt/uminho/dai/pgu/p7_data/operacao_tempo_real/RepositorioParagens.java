package pt.uminho.dai.pgu.p7_data.operacao_tempo_real;

import pt.uminho.dai.pgu.p7_data.DatabaseConnection;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;
import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;

import java.sql.*;
import java.util.*;

public class RepositorioParagens {
    private final List<String> paragens = new ArrayList<>();

    public RepositorioParagens() {
        carregarDaBD();
    }

    private void carregarDaBD() {
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT nome FROM paragens ORDER BY nome");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                paragens.add(rs.getString("nome"));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar paragens: " + e.getMessage());
        }
    }

    public List<String> listarTodas() {
        if (paragens.isEmpty()) {
            carregarDaBD(); // Tenta recarregar se estiver vazio
        }
        return new ArrayList<>(paragens);
    }
}