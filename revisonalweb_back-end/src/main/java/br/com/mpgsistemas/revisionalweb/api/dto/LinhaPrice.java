package br.com.mpgsistemas.revisionalweb.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Uma linha da tabela PRICE (uma parcela). Wrappers p/ null safety (OCR).
// ignoreUnknown: JSONB de versões antigas pode ter campos removidos do record.
@JsonIgnoreProperties(ignoreUnknown = true)
public record LinhaPrice(
        Integer numero,
        Double saldoInicial,
        Double juros,
        Double amortizacao,
        Double prestacao,
        Double saldoFinal
) {
}
