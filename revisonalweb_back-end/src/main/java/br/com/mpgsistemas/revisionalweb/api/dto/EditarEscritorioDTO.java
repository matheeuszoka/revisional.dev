package br.com.mpgsistemas.revisionalweb.api.dto;

/** Alteração de nome/CNPJ de um escritório existente (SUPER_ADMIN). */
public record EditarEscritorioDTO(String nome, String cnpj) {
}
