CREATE DATABASE IF NOT EXISTS tub;
ALTER DATABASE tub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE tub;

CREATE TABLE IF NOT EXISTS autocarros (
    id VARCHAR(50) PRIMARY KEY,
    capacidade_maxima INT NOT NULL,
    matricula VARCHAR(20) NULL,
    marca VARCHAR(50) NULL,
    modelo VARCHAR(50) NULL,
    linha_id VARCHAR(10) NULL,
    passageiros_atuais INT NOT NULL DEFAULT 0,
    total_passageiros_transportados INT NOT NULL DEFAULT 0,
    ultima_leitura DATETIME NULL
);

CREATE TABLE IF NOT EXISTS clientes (
    id VARCHAR(50) PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NULL,
    password VARCHAR(255) NULL,
    nif VARCHAR(15) NULL,
    passe_mensal BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS leituras (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    autocarro_id VARCHAR(50) NOT NULL,
    entradas INT NOT NULL,
    saidas INT NOT NULL,
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (autocarro_id) REFERENCES autocarros(id)
);

CREATE TABLE IF NOT EXISTS alertas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    autocarro_id VARCHAR(50) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    mensagem TEXT NOT NULL,
    timestamp DATETIME NOT NULL,
    FOREIGN KEY (autocarro_id) REFERENCES autocarros(id)
);

CREATE TABLE IF NOT EXISTS clientes_alertas (
    cliente_id VARCHAR(50) NOT NULL,
    alerta_id BIGINT NOT NULL,
    PRIMARY KEY (cliente_id, alerta_id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    FOREIGN KEY (alerta_id) REFERENCES alertas(id)
);

-- 1. LINHAS (ex: 40H, 07H, 43H)
CREATE TABLE IF NOT EXISTS linhas (
    id VARCHAR(10) PRIMARY KEY,          -- '40H', '07H', '43H'
    nome VARCHAR(100) NULL,
    descricao TEXT NULL
);

-- 2. PARAGENS (pontos de paragem da linha)
CREATE TABLE IF NOT EXISTS paragens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,          -- ex: 'Estação CP', 'U. Minho'
    morada VARCHAR(200) NULL,
    latitude DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL
);

-- 3. PARAGENS POR LINHA (ordem das paragens em cada linha)
CREATE TABLE IF NOT EXISTS linha_paragens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    linha_id VARCHAR(10) NOT NULL,
    paragem_id BIGINT NOT NULL,
    ordem INT NOT NULL,                  -- posição na linha (1ª, 2ª, 3ª paragem...)
    FOREIGN KEY (linha_id) REFERENCES linhas(id),
    FOREIGN KEY (paragem_id) REFERENCES paragens(id)
);

-- 4. VIAGENS (cada "viagem" = uma linha do CSV)
CREATE TABLE IF NOT EXISTS viagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    linha_id VARCHAR(10) NOT NULL,
    tipo_dia ENUM('UTIL', 'SABADO', 'DOMINGO', 'FERIADO') NOT NULL DEFAULT 'UTIL',
    periodo_escolar BOOLEAN NOT NULL DEFAULT TRUE, -- TRUE para Escolar, FALSE para Férias
    FOREIGN KEY (linha_id) REFERENCES linhas(id)
);


-- 5. HORÁRIOS (hora de cada paragem em cada viagem)
CREATE TABLE IF NOT EXISTS horarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    viagem_id BIGINT NOT NULL,
    paragem_id BIGINT NOT NULL,
    hora TIME NOT NULL,                  -- ex: '07:15'
    FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    FOREIGN KEY (paragem_id) REFERENCES paragens(id) ON DELETE CASCADE
);

-- ==============================================
-- OTIMIZAÇÃO PERFORMANCE (@database-admin skill)
-- Índices B-Tree para melhorar os JOINs no Azure MySQL Flexible Server
-- ==============================================

CREATE INDEX idx_leituras_autocarro ON leituras(autocarro_id);
CREATE INDEX idx_alertas_autocarro ON alertas(autocarro_id);
CREATE INDEX idx_clientes_alertas_cliente ON clientes_alertas(cliente_id);
CREATE INDEX idx_linha_paragens_linha ON linha_paragens(linha_id);
CREATE INDEX idx_viagens_linha ON viagens(linha_id);
CREATE INDEX idx_horarios_viagem ON horarios(viagem_id);
CREATE INDEX idx_leituras_timestamp ON leituras(timestamp);
CREATE INDEX idx_alertas_timestamp ON alertas(timestamp);

-- Seed de linhas TUB adicionais (inserção idempotente)
INSERT INTO linhas (id, nome, descricao) VALUES
 ('L2',  'Ponte de Prado — Bom Jesus', 'Linha interligando Ponte de Prado à zona do Bom Jesus.'),
 ('L3',  'Avenida Central — Ruães', 'Linha ligando Avenida Central a Ruães.'),
 ('L5',  'Dume — Quinta da Capela', 'Serviço entre Dume e Quinta da Capela.'),
 ('L6',  'Av. Gen. Norton de Matos — Gondizalves/Semelhe', 'Linha servindo a Av. Gen. Norton de Matos e a zona de Gondizalves/Semelhe.'),
 ('L7',  'Celeirós — S. Vítor', 'Linha já existente de Celeirós a São Vítor.'),
 ('L8',  'Rua 25 de Abril — Sete Fontes', 'Ligação entre Rua 25 de Abril e Sete Fontes.'),
 ('L9',  'Ruães — Nogueira (Barral)', 'Linha de Ruães até Nogueira (Barral).'),
 ('L12', 'Av. da Liberdade — Lageosa/Pedralva via Gualtar', 'Linha entre Av. da Liberdade e Lageosa/Pedralva via Gualtar.'),
 ('L13', 'Av. Gen. Norton de Matos — Lageosa/Pedralva', 'Linha entre Av. Gen. Norton de Matos e Lageosa/Pedralva.'),
 ('L14', 'Praça Conde de Agrolongo — Priscos', 'Linha de Praça Conde de Agrolongo a Priscos.'),
 ('L18', 'Rua do Raio — Pinheiro do Bicho via Esporões', 'Linha de Rua do Raio a Pinheiro do Bicho via Esporões.'),
 ('L19', 'Areal — Boavista', 'Linha ligando Areal a Boavista.'),
 ('L20', 'Av. da Liberdade — Escudeiros via Ponte Nova', 'Linha entre Av. da Liberdade e Escudeiros via Ponte Nova.'),
 ('L40', 'Gualtar — Real', 'Linha do Hospital de Gualtar até Real.'),
 ('L43', 'Estação — Universidade', 'Linha entre a Estação CP e a Universidade de Minho.')
ON DUPLICATE KEY UPDATE nome = VALUES(nome), descricao = VALUES(descricao);

-- Tabela de bilhetes comprados via Stripe
CREATE TABLE IF NOT EXISTS bilhetes (
    id VARCHAR(50) PRIMARY KEY,                        -- UUID gerado no backend
    cliente_id VARCHAR(50) NOT NULL,
    tipo VARCHAR(50) NOT NULL,                         -- 'simples' | 'passe'
    nome_tipo VARCHAR(100) NOT NULL,                   -- 'Bilhete Simples' | 'Passe Mensal'
    data_compra DATETIME NOT NULL,
    data_validade DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Ativo',       -- 'Ativo', 'Utilizado', 'Expirado'
    preco DECIMAL(10, 2) NOT NULL,
    payment_intent_id VARCHAR(200) NOT NULL UNIQUE,    -- ID do Stripe PaymentIntent
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE INDEX idx_bilhetes_cliente ON bilhetes(cliente_id);
CREATE INDEX idx_bilhetes_payment_intent ON bilhetes(payment_intent_id);

-- Configurações e definições específicas de cada cliente
CREATE TABLE IF NOT EXISTS definicoes_cliente (
    cliente_id VARCHAR(50) PRIMARY KEY,
    tema VARCHAR(20) NOT NULL DEFAULT 'dark',
    notificacoes_ativas BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);

-- Linhas preferidas do cliente
CREATE TABLE IF NOT EXISTS linhas_favoritas (
    cliente_id VARCHAR(50) NOT NULL,
    linha_id VARCHAR(10) NOT NULL,
    PRIMARY KEY (cliente_id, linha_id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE CASCADE
);
