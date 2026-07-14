package br.com.mpgsistemas.revisionalweb.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// DTO serializado como JSONB em CasoRevisional.mercado (dados da API SGS 25471 do BCB)
// ignoreUnknown: JSONB de versões antigas pode ter campos removidos da classe.
@JsonIgnoreProperties(ignoreUnknown = true)
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
