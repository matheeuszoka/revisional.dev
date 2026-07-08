package br.com.mpgsistemas.revisionalweb.api.dto;

/**
 * Criação de escritório pelo SUPER_ADMIN: tenant novo + admin inicial do
 * cliente com senha temporária (troca obrigatória no 1º login).
 */
public record NovoEscritorioDTO(String nome, String cnpj,
                                String adminNome, String adminCpf, String adminEmail,
                                String adminOab, String senhaTemporaria) {
}
