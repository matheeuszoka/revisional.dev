package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;

import java.time.LocalDateTime;

/** Visão segura do usuário para o painel de membros (nunca expõe senha/token). */
public record UsuarioResponseDTO(
        Long id,
        String nomeCompleto,
        String email,
        String cpf,
        String oab,
        UsuarioRole role,
        boolean ativo,
        Boolean forcarTrocaSenha,
        LocalDateTime dataCriacao,
        LocalDateTime dataUltimoLogin
) {
    public static UsuarioResponseDTO from(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId_usuario(),
                u.getNomeCompleto(),
                u.getEmail(),
                u.getCpf(),
                u.getOab(),
                u.getUsuarioRole(),
                u.isAtivo(),
                u.getForcarTrocaSenha(),
                u.getDataCriacao(),
                u.getDataUltimoLogin()
        );
    }
}
