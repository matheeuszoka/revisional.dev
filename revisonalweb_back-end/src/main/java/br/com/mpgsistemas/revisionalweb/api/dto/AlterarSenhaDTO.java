package br.com.mpgsistemas.revisionalweb.api.dto;

/** Troca da própria senha (exige a senha atual). Limpa forcar_troca_senha. */
public record AlterarSenhaDTO(
        String senhaAtual,
        String novaSenha
) {
}
