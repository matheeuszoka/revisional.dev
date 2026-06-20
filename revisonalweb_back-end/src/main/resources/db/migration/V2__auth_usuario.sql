-- V2 :: Autenticação — campos de segurança em table_usuario (padrão sigapsi)
-- Login flexível: email, cpf ou oab. Sessão única (token_ativo) + lockout.

ALTER TABLE table_usuario
    ADD COLUMN email               VARCHAR(150),
    ADD COLUMN usuario_role        VARCHAR(30),
    ADD COLUMN data_criacao        TIMESTAMP,
    ADD COLUMN data_ultimo_login   TIMESTAMP,
    ADD COLUMN tentativas_falhas   INTEGER,
    ADD COLUMN conta_bloqueada_ate TIMESTAMP,
    ADD COLUMN forcar_troca_senha  BOOLEAN,
    ADD COLUMN token_ativo         TEXT,
    ADD COLUMN ultimo_ip           VARCHAR(45);

-- Identificadores de login únicos (cpf já era UNIQUE na V1)
ALTER TABLE table_usuario
    ADD CONSTRAINT uk_usuario_email UNIQUE (email),
    ADD CONSTRAINT uk_usuario_oab   UNIQUE (oab);
