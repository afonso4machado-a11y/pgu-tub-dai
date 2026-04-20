CREATE DATABASE IF NOT EXISTS tub;
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
    nome VARCHAR(100) NOT NULL
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
