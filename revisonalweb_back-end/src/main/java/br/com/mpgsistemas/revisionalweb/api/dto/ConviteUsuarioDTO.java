package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;

/**
 * Convite de membro pelo admin do escritório. O tenant vem do JWT do admin
 * (nunca do payload). senhaTemporaria: o convidado troca no primeiro login
 * (forcar_troca_senha).
 */
public record ConviteUsuarioDTO(
        String nomeCompleto,
        String email,
        String cpf,
        String oab,
        String senhaTemporaria,
        UsuarioRole role
) {
}
