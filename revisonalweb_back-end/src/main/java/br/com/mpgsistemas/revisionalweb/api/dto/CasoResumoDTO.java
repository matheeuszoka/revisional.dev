package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;

import java.time.LocalDateTime;

// Resumo para listagem (não devolve os JSONB completos).
public record CasoResumoDTO(
        Long id,
        String titulo,
        String clienteNome,
        String instituicao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        boolean temResultado
) {
    public static CasoResumoDTO from(CasoRevisional c) {
        var contrato = c.getContrato();
        return new CasoResumoDTO(
                c.getId_caso_revisional(),
                c.getTitulo(),
                contrato != null ? contrato.getClienteNome() : null,
                contrato != null ? contrato.getInstituicao() : null,
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                c.getResultado() != null
        );
    }
}
