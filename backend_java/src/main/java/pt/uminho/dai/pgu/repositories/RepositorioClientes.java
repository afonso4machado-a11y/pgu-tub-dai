package pt.uminho.dai.pgu.repositories;
import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.services.*;

import java.sql.*;
import java.util.*;

public class RepositorioClientes {
    private final Map<String, Cliente> clientes = new LinkedHashMap<>();

    public RepositorioClientes() {
        carregarDaBD();
    }

    public RepositorioClientes(boolean semBD) {
        // Construtor para uso em testes — não carrega da BD
    }

    private void carregarDaBD() {
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement("SELECT id, nome, email, password, nif, passe_mensal FROM clientes");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("id");
                String nome = rs.getString("nome");
                String email = rs.getString("email");
                String password = rs.getString("password");
                String nif = rs.getString("nif");
                boolean passeMensal = rs.getBoolean("passe_mensal");
                clientes.put(id, new Cliente(id, nome, email, password, nif, passeMensal));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
        }
    }

    public void guardar(Cliente cliente) {
        clientes.put(cliente.getId(), cliente);
        try (Connection conn = DatabaseConnection.obterConexao();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO clientes (id, nome, email, password, nif, passe_mensal) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nome = VALUES(nome), password = VALUES(password), nif = VALUES(nif), passe_mensal = VALUES(passe_mensal)")) {
            ps.setString(1, cliente.getId());
            ps.setString(2, cliente.getNome());
            ps.setString(3, cliente.getEmail());
            ps.setString(4, cliente.getPassword());
            ps.setString(5, cliente.getNif());
            ps.setBoolean(6, cliente.isPasseMensal());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao guardar cliente: " + e.getMessage());
        }
    }

    public Optional<Cliente> procurarPorId(String id) {
        return Optional.ofNullable(clientes.get(id));
    }

    public Optional<Cliente> procurarPorEmail(String email) {
        return clientes.values().stream()
                .filter(c -> email.equalsIgnoreCase(c.getEmail()))
                .findFirst();
    }

    public Collection<Cliente> listarTodos() {
        return clientes.values();
    }
}