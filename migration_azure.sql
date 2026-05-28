-- =============================================================
-- MIGRATION SCRIPT — TUB/PGU Azure MySQL
-- Executar UMA VEZ na base de dados existente no Azure
-- Data: 2026-05-28
-- =============================================================
-- Este script é IDEMPOTENTE: seguro de executar mais do que uma vez.

USE pgu_tub;

-- ─────────────────────────────────────────────────────────────
-- 1. Adicionar coluna 'deleted' à tabela autocarros
--    (para soft delete reversível)
-- ─────────────────────────────────────────────────────────────
ALTER TABLE autocarros
  ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- ─────────────────────────────────────────────────────────────
-- 2. Remover FK de linhas_favoritas → linhas
--    (linhas favoritas podem ser IDs externos/GTFS não registados)
-- ─────────────────────────────────────────────────────────────
-- Primeiro verificar se a FK existe antes de tentar remover
SET @fk_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'linhas_favoritas'
    AND REFERENCED_TABLE_NAME = 'linhas'
);

-- Só remove se existir (compatível com MySQL 8.0+)
-- NOTA: Se esta instrução falhar, verificar o nome exacto da FK com:
--   SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
--   WHERE TABLE_NAME = 'linhas_favoritas' AND CONSTRAINT_TYPE = 'FOREIGN KEY';
-- E substituir 'linhas_favoritas_ibfk_2' pelo nome correcto.
ALTER TABLE linhas_favoritas DROP FOREIGN KEY IF EXISTS linhas_favoritas_ibfk_2;

-- ─────────────────────────────────────────────────────────────
-- 3. Verificar se a tabela bilhetes tem a coluna nome_tipo
--    (adicionada no design actual, pode não existir em BD antigas)
-- ─────────────────────────────────────────────────────────────
ALTER TABLE bilhetes
  ADD COLUMN IF NOT EXISTS nome_tipo VARCHAR(100) NULL AFTER tipo;

-- ─────────────────────────────────────────────────────────────
-- 4. Confirmar estado final
-- ─────────────────────────────────────────────────────────────
SELECT 'Migration concluída.' AS resultado;
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('autocarros', 'bilhetes')
  AND COLUMN_NAME IN ('deleted', 'nome_tipo')
ORDER BY TABLE_NAME, COLUMN_NAME;
