package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;

import java.time.LocalDateTime;

// Detalhe do caso SEM a entidade auditor (evita recursão Usuario<->casos e lazy init).
// Expõe os JSONB (DTOs simples, seguros para serializar).
public record CasoDetalheDTO(
        Long id,
        String titulo,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        DadosContrato contrato,
        Object mercado,
        Object resultado
) {
    public static CasoDetalheDTO from(CasoRevisional c) {
        return new CasoDetalheDTO(
                c.getId_caso_revisional(),
                c.getTitulo(),
                c.getCriadoEm(),
                c.getAtualizadoEm(),
                c.getContrato(),
                c.getMercado(),
                c.getResultado()
        );
    }
}
