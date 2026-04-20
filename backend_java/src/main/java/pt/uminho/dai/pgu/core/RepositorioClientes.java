package pt.uminho.dai.pgu.core;

import java.sql.*;
import java.util.*;

public class RepositorioClientes {
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();

    public RepositorioClientes() {
        carregarDaBD();
    }

    protected RepositorioClientes(boolean semBD) {
        // Construtor para uso em testes — não carrega da BD
    }

    private void carregarDaBD() {
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT id, nome FROM clientes");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String nome = rs.getString("nome");
                clientes.put(id, new Cliente(id, nome));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
        }
    }

    public void guardar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO clientes (id, nome) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE nome = VALUES(nome)")) {
            ps.setString(1, cliente.getId());
            ps.setString(2, cliente.getNome());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar cliente: " + e.getMessage());
        }
    }

    public Optional<Cliente> procurarPorId(String id) {
        return Optional.ofNullable(clientes.get(id));
    }

    public Collection<Cliente> listarTodos() {
        return clientes.values();
    }
}