-- Proposta inicial de persistencia para a futura plataforma.
-- Ainda nao deve ser executada nesta etapa.
-- Banco recomendado futuramente: PostgreSQL.

CREATE TABLE usuarios (
    id VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(200),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);

CREATE TABLE triagens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL REFERENCES usuarios(id),
    perfil VARCHAR(30),
    score_ansiedade INTEGER,
    score_depressao INTEGER,
    realizado_em TIMESTAMP NOT NULL
);

CREATE TABLE interacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL REFERENCES usuarios(id),
    autor VARCHAR(30) NOT NULL,
    texto TEXT NOT NULL,
    registrado_em TIMESTAMP NOT NULL
);

CREATE TABLE avaliacoes_risco (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL REFERENCES usuarios(id),
    nivel_risco VARCHAR(30) NOT NULL,
    metodo VARCHAR(100),
    categoria VARCHAR(150),
    confianca DECIMAL(5,4),
    justificativa TEXT,
    registrado_em TIMESTAMP NOT NULL
);

CREATE TABLE protocolos_intervencao (
    id BIGSERIAL PRIMARY KEY,
    usuario_id VARCHAR(100) NOT NULL REFERENCES usuarios(id),
    protocolo VARCHAR(100) NOT NULL,
    registrado_em TIMESTAMP NOT NULL
);

CREATE TABLE preferencias_usuario (
    usuario_id VARCHAR(100) PRIMARY KEY REFERENCES usuarios(id),
    conteudo VARCHAR(50),
    psicologo VARCHAR(50),
    local_atendimento VARCHAR(50),
    atualizado_em TIMESTAMP NOT NULL
);

-- Futuramente:
-- eventos_fisiologicos
-- leituras_wearable
-- checkins_evento
-- recomendacoes_conteudo
-- relatorios
