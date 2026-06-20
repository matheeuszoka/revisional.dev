package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;

// Criação/edição de caso. contrato é opcional (pode ser preenchido depois via OCR).
public record CasoRequestDTO(
        String titulo,
        DadosContrato contrato
) {
}
