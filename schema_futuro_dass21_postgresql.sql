-- Extensao proposta para persistencia da DASS-21.
-- Nao executar antes da definicao final do banco/autenticacao.

CREATE TABLE triagens_dass21 (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL REFERENCES usuarios(id),

    instrumento VARCHAR(30) NOT NULL DEFAULT 'DASS-21',
    versao VARCHAR(120) NOT NULL,

    resposta_01 SMALLINT NOT NULL CHECK (resposta_01 BETWEEN 0 AND 3),
    resposta_02 SMALLINT NOT NULL CHECK (resposta_02 BETWEEN 0 AND 3),
    resposta_03 SMALLINT NOT NULL CHECK (resposta_03 BETWEEN 0 AND 3),
    resposta_04 SMALLINT NOT NULL CHECK (resposta_04 BETWEEN 0 AND 3),
    resposta_05 SMALLINT NOT NULL CHECK (resposta_05 BETWEEN 0 AND 3),
    resposta_06 SMALLINT NOT NULL CHECK (resposta_06 BETWEEN 0 AND 3),
    resposta_07 SMALLINT NOT NULL CHECK (resposta_07 BETWEEN 0 AND 3),
    resposta_08 SMALLINT NOT NULL CHECK (resposta_08 BETWEEN 0 AND 3),
    resposta_09 SMALLINT NOT NULL CHECK (resposta_09 BETWEEN 0 AND 3),
    resposta_10 SMALLINT NOT NULL CHECK (resposta_10 BETWEEN 0 AND 3),
    resposta_11 SMALLINT NOT NULL CHECK (resposta_11 BETWEEN 0 AND 3),
    resposta_12 SMALLINT NOT NULL CHECK (resposta_12 BETWEEN 0 AND 3),
    resposta_13 SMALLINT NOT NULL CHECK (resposta_13 BETWEEN 0 AND 3),
    resposta_14 SMALLINT NOT NULL CHECK (resposta_14 BETWEEN 0 AND 3),
    resposta_15 SMALLINT NOT NULL CHECK (resposta_15 BETWEEN 0 AND 3),
    resposta_16 SMALLINT NOT NULL CHECK (resposta_16 BETWEEN 0 AND 3),
    resposta_17 SMALLINT NOT NULL CHECK (resposta_17 BETWEEN 0 AND 3),
    resposta_18 SMALLINT NOT NULL CHECK (resposta_18 BETWEEN 0 AND 3),
    resposta_19 SMALLINT NOT NULL CHECK (resposta_19 BETWEEN 0 AND 3),
    resposta_20 SMALLINT NOT NULL CHECK (resposta_20 BETWEEN 0 AND 3),
    resposta_21 SMALLINT NOT NULL CHECK (resposta_21 BETWEEN 0 AND 3),

    subtotal_depressao SMALLINT NOT NULL,
    subtotal_ansiedade SMALLINT NOT NULL,
    subtotal_estresse SMALLINT NOT NULL,

    score_depressao SMALLINT NOT NULL,
    score_ansiedade SMALLINT NOT NULL,
    score_estresse SMALLINT NOT NULL,

    versao_algoritmo VARCHAR(40) NOT NULL DEFAULT 'DASS21_SCORING_V1',
    realizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_triagens_dass21_usuario_data
    ON triagens_dass21(usuario_id, realizado_em DESC);
