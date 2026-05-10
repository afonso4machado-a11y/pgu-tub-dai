package pt.uminho.dai.pgu.repositories;
import pt.uminho.dai.pgu.models.*;
import pt.uminho.dai.pgu.services.*;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RepositorioClientes {
    private final Map<String, Cliente> clientesById = new ConcurrentHashMap<>();
    private final Map<String, Cliente> clientesByEmail = new ConcurrentHashMap<>();
    private volatile boolean isLoaded = false;

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
                
                Cliente cliente = new Cliente(id, nome, email, password, nif, passeMensal);
                clientesById.put(id, cliente);
                if (email != null) {
                    clientesByEmail.put(email.toLowerCase(), cliente);
                }
            }
            isLoaded = true;
            System.out.println("[DB] Clientes carregados: " + clientesById.size());
        } catch (SQLException e) {
            System.err.println("Erro ao carregar clientes: " + e.getMessage());
        }
    }

    public void guardar(Cliente cliente) {
        clientesById.put(cliente.getId(), cliente);
        if (cliente.getEmail() != null) {
            clientesByEmail.put(cliente.getEmail().toLowerCase(), cliente);
        }
        
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
        return Optional.ofNullable(clientesById.get(id));
    }

    public Optional<Cliente> procurarPorEmail(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(clientesByEmail.get(email.toLowerCase()));
    }

    public Collection<Cliente> listarTodos() {
        return new ArrayList<>(clientesById.values());
    }
}