package pt.uminho.dai.pgu;

import pt.uminho.dai.pgu.data.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MigrarAzure {
    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("INICIANDO MIGRAÇÃO DA BASE DE DADOS AZURE MYSQL EM PRODUÇÃO");
        System.out.println("=============================================================");

        try (Connection conn = DatabaseConnection.obterConexao()) {
            System.out.println("[INFO] Conexão com Azure MySQL estabelecida com sucesso!");
            
            try (Statement stmt = conn.createStatement()) {
                
                // 1. Adicionar a coluna deleted à tabela autocarros se não existir
                System.out.println("[1/3] A migrar tabela 'autocarros' (adicionar coluna 'deleted')...");
                try {
                    stmt.execute("ALTER TABLE autocarros ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE");
                    System.out.println("[SUCESSO] Coluna 'deleted' adicionada à tabela 'autocarros'.");
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1060 || e.getMessage().contains("Duplicate column name")) {
                        System.out.println("[INFO] A coluna 'deleted' já existe na tabela 'autocarros'. Ignorado.");
                    } else {
                        System.err.println("[ERRO] Erro ao adicionar coluna 'deleted': " + e.getMessage());
                    }
                }

                // 2. Remover a Foreign Key de linhas_favoritas -> linhas
                System.out.println("[2/3] A migrar tabela 'linhas_favoritas' (remover foreign key)...");
                try {
                    stmt.execute("ALTER TABLE linhas_favoritas DROP FOREIGN KEY linhas_favoritas_ibfk_2");
                    System.out.println("[SUCESSO] Foreign key 'linhas_favoritas_ibfk_2' removida.");
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1091 || e.getMessage().contains("check that column/key exists") || e.getMessage().contains("Constraint not found")) {
                        System.out.println("[INFO] A foreign key 'linhas_favoritas_ibfk_2' já não existe. Ignorado.");
                    } else {
                        System.err.println("[ERRO] Erro ao remover foreign key: " + e.getMessage());
                    }
                }

                // 3. Adicionar coluna nome_tipo à tabela bilhetes se não existir
                System.out.println("[3/3] A migrar tabela 'bilhetes' (adicionar coluna 'nome_tipo')...");
                try {
                    stmt.execute("ALTER TABLE bilhetes ADD COLUMN nome_tipo VARCHAR(100) NULL AFTER tipo");
                    System.out.println("[SUCESSO] Coluna 'nome_tipo' adicionada à tabela 'bilhetes'.");
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1060 || e.getMessage().contains("Duplicate column name")) {
                        System.out.println("[INFO] A coluna 'nome_tipo' já existe na tabela 'bilhetes'. Ignorado.");
                    } else {
                        System.err.println("[ERRO] Erro ao adicionar coluna 'nome_tipo': " + e.getMessage());
                    }
                }

                System.out.println("=============================================================");
                System.out.println("MIGRAÇÃO CONCLUÍDA COM SUCESSO!");
                System.out.println("=============================================================");
            }
        } catch (SQLException e) {
            System.err.println("[ERRO FATAL] Não foi possível ligar à base de dados no Azure: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
