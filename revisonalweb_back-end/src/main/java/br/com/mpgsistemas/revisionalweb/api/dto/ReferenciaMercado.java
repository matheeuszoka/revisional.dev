package br.com.mpgsistemas.revisionalweb.api.dto;

import lombok.Data;


@Data
public record ReferenciaMercado {

    private String fonte = "BCB SGS 25471";
    private String codigoModalidade = "25471";
    private String dataReferencia;
    private Double taxaMensalPct;
    private Double taxaAnualPct;
    private Double taxaMensalInstituicaoPct;
    private String observacao;
}
