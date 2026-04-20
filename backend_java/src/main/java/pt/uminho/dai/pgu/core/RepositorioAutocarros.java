package pt.uminho.dai.pgu.core;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class RepositorioAutocarros {
    private final Map<String, Autocarro> autocarros = new LinkedHashMap<>();

    public RepositorioAutocarros() {
        carregarDaBD();
    }

    protected RepositorioAutocarros(boolean semBD) {
        // Construtor para uso em testes — não carrega da BD
    }

    private void carregarDaBD() {
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT id, capacidade_maxima, matricula, marca, modelo, linha_id, passageiros_atuais, " +
                "total_passageiros_transportados, ultima_leitura FROM autocarros");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                int capacidade = rs.getInt("capacidade_maxima");
                String matricula = rs.getString("matricula");
                String marca = rs.getString("marca");
                String modelo = rs.getString("modelo");
                String linhaId = rs.getString("linha_id");
                int passageiros = rs.getInt("passageiros_atuais");
                int totalTransportados = rs.getInt("total_passageiros_transportados");
                Timestamp ultimaLeituraTs = rs.getTimestamp("ultima_leitura");
                LocalDateTime ultimaLeitura = ultimaLeituraTs != null ? ultimaLeituraTs.toLocalDateTime() : null;

                Autocarro a = new Autocarro(id, capacidade, matricula, marca, modelo);
                a.setLinhaId(linhaId);
                a.setPassageirosAtuais(passageiros);
                a.setTotalPassageirosTransportados(totalTransportados);
                a.setUltimaLeitura(ultimaLeitura);
                autocarros.put(id, a);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar autocarros: " + e.getMessage());
        }
    }

    public void guardar(Autocarro autocarro) {
        autocarros.put(autocarro.getId(), autocarro);
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO autocarros (id, capacidade_maxima, matricula, marca, modelo, linha_id, passageiros_atuais, " +
                "total_passageiros_transportados, ultima_leitura) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE capacidade_maxima = VALUES(capacidade_maxima), " +
                "matricula = VALUES(matricula), marca = VALUES(marca), modelo = VALUES(modelo), " +
                "linha_id = VALUES(linha_id), " +
                "passageiros_atuais = VALUES(passageiros_atuais), " +
                "total_passageiros_transportados = VALUES(total_passageiros_transportados), " +
                "ultima_leitura = VALUES(ultima_leitura)")) {
            ps.setString(1, autocarro.getId());
            ps.setInt(2, autocarro.getCapacidadeMaxima());
            ps.setString(3, autocarro.getMatricula());
            ps.setString(4, autocarro.getMarca());
            ps.setString(5, autocarro.getModelo());
            ps.setString(6, autocarro.getLinhaId());
            ps.setInt(7, autocarro.getPassageirosAtuais());
            ps.setInt(8, autocarro.getTotalPassageirosTransportados());
            ps.setTimestamp(9, autocarro.getUltimaLeitura() != null ?
                Timestamp.valueOf(autocarro.getUltimaLeitura()) : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar autocarro: " + e.getMessage());
        }
    }

    public void atualizarEstado(Autocarro autocarro) {
        autocarros.put(autocarro.getId(), autocarro);
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE autocarros SET linha_id = ?, passageiros_atuais = ?, " +
                "total_passageiros_transportados = ?, ultima_leitura = ? WHERE id = ?")) {
            ps.setString(1, autocarro.getLinhaId());
            ps.setInt(2, autocarro.getPassageirosAtuais());
            ps.setInt(3, autocarro.getTotalPassageirosTransportados());
            ps.setTimestamp(4, autocarro.getUltimaLeitura() != null ?
                Timestamp.valueOf(autocarro.getUltimaLeitura()) : null);
            ps.setString(5, autocarro.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estado do autocarro: " + e.getMessage());
        }
    }

    public Optional<Autocarro> procurarPorId(String id) {
        return Optional.ofNullable(autocarros.get(id));
    }

    public Collection<Autocarro> listarTodos() {
        return autocarros.values();
    }
}