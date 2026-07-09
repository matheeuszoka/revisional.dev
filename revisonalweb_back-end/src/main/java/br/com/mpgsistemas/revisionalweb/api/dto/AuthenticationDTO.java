package br.com.mpgsistemas.revisionalweb.api.dto;

// login = email, cpf ou oab
public record AuthenticationDTO(String login, String senha) {
}
