CREATE TABLE pessoas (
    id UUID PRIMARY KEY NOT NULL,
    apelido VARCHAR(32) UNIQUE NOT NULL,
    nome VARCHAR(100) NOT NULL,
    nascimento DATE NOT NULL,
    stack TEXT,
    termo_busca TEXT GENERATED ALWAYS AS (LOWER(nome || apelido || stack)) STORED
);

CREATE EXTENSION PG_TRGM;
CREATE INDEX idx_termo_busca ON pessoas USING GIST (termo_busca GIST_TRGM_OPS(SIGLEN=64));