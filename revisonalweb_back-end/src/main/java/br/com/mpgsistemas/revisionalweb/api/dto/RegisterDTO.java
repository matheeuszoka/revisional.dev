package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;

public record RegisterDTO(
        String nomeCompleto,
        String email,
        String cpf,
        String oab,
        String senha,
        UsuarioRole role
) {
}
