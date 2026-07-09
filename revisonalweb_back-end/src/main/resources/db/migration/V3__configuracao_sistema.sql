-- V3 :: Parâmetros operacionais editáveis (OCR + IA) via tela de admin.
-- Linha única (id=1). A chave da IA é gravada CIFRADA (AES-GCM) em open_router_api_key_enc.
-- Constantes periciais (tolerâncias/iterações) NÃO ficam aqui: permanecem fixas no código.

CREATE TABLE table_configuracao_sistema (
    id_configuracao_sistema       BIGINT PRIMARY KEY,
    ocr_idioma                    VARCHAR(20),
    ocr_dpi                       INTEGER,
    ocr_max_paginas               INTEGER,
    ocr_tessdata_path             VARCHAR(255),
    open_router_base_url          VARCHAR(255),
    open_router_model             VARCHAR(120),
    open_router_api_key_enc       TEXT,
    open_router_timeout_segundos  INTEGER,
    open_router_max_chars         INTEGER,
    open_router_referer           VARCHAR(255),
    open_router_titulo            VARCHAR(120),
    atualizado_em                 TIMESTAMP
);

-- Semente: linha única; valores são preenchidos no primeiro boot a partir dos
-- defaults de ambiente (ConfiguracaoService). Mantemos a linha presente desde já.
INSERT INTO table_configuracao_sistema (id_configuracao_sistema) VALUES (1);
