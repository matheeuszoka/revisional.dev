package br.com.mpgsistemas.revisionalweb.api.dto;

public record RegisterDTO(
        String nomeCompleto,
        String email,
        String cpf,
        String oab,
        String senha,
        // Multi-tenancy: nome do escritório/empresa. Auto-signup SÓ cria escritório
        // novo (1º usuário = ADMIN). Membros extras entram por convite do admin.
        String nomeEscritorio,
        String cnpjEscritorio
) {
}
