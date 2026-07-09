package br.com.mpgsistemas.revisionalweb.api.dto;

import lombok.Data;

// DTO serializado como JSONB em CasoRevisional.mercado (dados da API SGS 25471 do BCB)
@Data
public class ReferenciaMercado {

    private String fonte = "BCB SGS 25471";
    private String codigoModalidade = "25471";
    private String dataReferencia;
    private Double taxaMensalPct;
    private Double taxaAnualPct;
    private Double taxaMensalInstituicaoPct;
    private Double taxaAnualInstituicaoPct;
    private String url;
    private String observacao;
}
