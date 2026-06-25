package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;

// Disparo da analise. Campos opcionais: permite revisar contrato/mercado antes de
// recalcular. usarBcb=true busca a taxa de referencia na serie SGS 25471 quando
// nao houver taxa de mercado informada manualmente.
public record AnaliseRequestDTO(
        DadosContrato contrato,
        ReferenciaMercado mercado,
        Boolean usarBcb
) {
}
