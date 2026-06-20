package br.com.mpgsistemas.revisionalweb.api.model;

// Papéis de acesso. ADMIN = suporte/gestão; AUDITOR = advogado/auditor (uso normal);
// VISUALIZADOR = somente leitura (GET).
public enum UsuarioRole {

    ROLE_ADMIN,
    ROLE_AUDITOR,
    ROLE_VISUALIZADOR
}
