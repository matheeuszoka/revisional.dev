package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;

import java.time.LocalDateTime;

// Resumo para listagem (não devolve os JSONB completos).
public record CasoResumoDTO(
        Long id,
        String titulo,
        String clienteNome,
        String instituicao,
        Double valorFinanciado,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        boolean temResultado,
        String classificacaoRisco,
        // Nome do escritório (tenant) dono do caso — preenchido só na visão
        // cross-tenant do SUPER_ADMIN; null para os demais papéis.
        String escritorio
) {
    public static CasoResumoDTO from(CasoRevisional c) {
        return from(c, null);
    }

    public static CasoResumoDTO from(CasoRevisional c, String escritorio) {
        var contrato = c.getContrato();
        var resultado = c.getResultado();
        return new CasoResumoDTO(
                c.getId_caso_revisional(),
                c.getTitulo(),
                contrato != null ? contrato.getClienteNome() : null,
                contrato != null ? contrato.getInstituicao() : null,
                contrato != null ? contrato.getValorFinanciado() : null,
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                resultado != null,
                resultado != null ? resultado.getClassificacaoRisco() : null,
                escritorio
        );
    }
}
