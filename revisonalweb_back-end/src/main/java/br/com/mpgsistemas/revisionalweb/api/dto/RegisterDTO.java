package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;

public record RegisterDTO(
        String nomeCompleto,
        String email,
        String cpf,
        String oab,
        String senha,
        UsuarioRole role,
        // Multi-tenancy: nome do escritório/empresa. Auto-signup cria o tenant e o
        // primeiro usuário vira ADMIN dele. Se o tenant já existir, anexa a ele.
        String nomeEscritorio,
        String cnpjEscritorio
) {
}
