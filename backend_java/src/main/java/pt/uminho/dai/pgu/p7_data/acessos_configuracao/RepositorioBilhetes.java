package pt.uminho.dai.pgu.p7_data.acessos_configuracao;

import pt.uminho.dai.pgu.p7_data.DatabaseConnection;
import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.*;
import pt.uminho.dai.pgu.p2_businesslogic.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p2_businesslogic.analitica_historico.*;
import pt.uminho.dai.pgu.p7_data.acessos_configuracao.*;
import pt.uminho.dai.pgu.p7_data.operacao_tempo_real.*;
import pt.uminho.dai.pgu.p7_data.analitica_historico.*;

import pt.uminho.dai.pgu.p2_businesslogic.acessos_configuracao.Bilhete;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JDBC para persistência de bilhetes comprados via Stripe.
 */
public class RepositorioBilhetes {

    public void guardar(Bilhete bilhete) {
        String sql = """
            INSERT INTO bilhetes
                (id, cliente_id, tipo, nome_tipo, data_compra, data_validade, estado, preco, payment_intent_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bilhete.getId());
            ps.setString(2, bilhete.getClienteId());
            ps.setString(3, bilhete.getTipo());
            ps.setString(4, bilhete.getNomeTipo());
            ps.setTimestamp(5, Timestamp.valueOf(bilhete.getDataCompra()));
            ps.setTimestamp(6, Timestamp.valueOf(bilhete.getDataValidade()));
            ps.setString(7, bilhete.getEstado());
            ps.setBigDecimal(8, bilhete.getPreco());
            ps.setString(9, bilhete.getPaymentIntentId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("[Bilhetes] Erro ao guardar bilhete: " + e.getMessage(), e);
        }
    }

    public List<Bilhete> listarPorCliente(String clienteId) {
        String sql = "SELECT * FROM bilhetes WHERE cliente_id = ? ORDER BY data_compra DESC";
        List<Bilhete> bilhetes = new ArrayList<>();
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, clienteId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                bilhetes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("[Bilhetes] Erro ao listar bilhetes: " + e.getMessage(), e);
        }
        return bilhetes;
    }

    public Optional<Bilhete> procurarPorPaymentIntent(String paymentIntentId) {
        String sql = "SELECT * FROM bilhetes WHERE payment_intent_id = ?";
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentIntentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("[Bilhetes] Erro ao procurar por payment intent: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private Bilhete mapRow(ResultSet rs) throws SQLException {
        Bilhete b = new Bilhete();
        b.setId(rs.getString("id"));
        b.setClienteId(rs.getString("cliente_id"));
        b.setTipo(rs.getString("tipo"));
        b.setNomeTipo(rs.getString("nome_tipo"));
        b.setDataCompra(rs.getTimestamp("data_compra").toLocalDateTime());
        b.setDataValidade(rs.getTimestamp("data_validade").toLocalDateTime());
        b.setEstado(rs.getString("estado"));
        b.setPreco(rs.getBigDecimal("preco"));
        b.setPaymentIntentId(rs.getString("payment_intent_id"));
        return b;
    }
}