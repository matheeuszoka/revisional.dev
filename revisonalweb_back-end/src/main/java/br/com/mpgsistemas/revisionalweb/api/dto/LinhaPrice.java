package br.com.mpgsistemas.revisionalweb.api.dto;

// Uma linha da tabela PRICE (uma parcela). Wrappers p/ null safety (OCR).
public record LinhaPrice(
        Integer numero,
        Double saldoInicial,
        Double juros,
        Double amortizacao,
        Double prestacao,
        Double saldoFinal
) {
}
