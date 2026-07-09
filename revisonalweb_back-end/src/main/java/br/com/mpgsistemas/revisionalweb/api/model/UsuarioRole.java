package br.com.mpgsistemas.revisionalweb.api.model;

// Papéis de acesso. SUPER_ADMIN = MPG/plataforma (enxerga todos os tenants);
// ADMIN = admin do escritório (só o próprio tenant); AUDITOR = advogado/auditor
// (uso normal); VISUALIZADOR = somente leitura (GET).
public enum UsuarioRole {

    ROLE_SUPER_ADMIN,
    ROLE_ADMIN,
    ROLE_AUDITOR,
    ROLE_VISUALIZADOR
}
