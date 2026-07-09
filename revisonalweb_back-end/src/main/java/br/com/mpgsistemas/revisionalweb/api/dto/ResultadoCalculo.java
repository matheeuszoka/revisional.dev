package br.com.mpgsistemas.revisionalweb.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// DTO serializado como JSONB em CasoRevisional.resultado (laudo processado)
@Data
public class ResultadoCalculo {

    private String status = "Pendente";
    private Double taxaMensalContratoApuradaPct;
    private Double taxaAnualContratoApuradaPct;
    private Double taxaMensalMercadoPct;
    private Double taxaAnualMercadoPct;
    private Double spreadPontosPercentuaisMes;
    private Double spreadPercentualSobreMercado;
    private Double parcelaContratual;
    private Double parcelaRecalculadaMercado;
    private Double diferencaMensal;
    private Double diferencaTotalNominal;
    private Double totalContratado;
    private Double totalRecalculado;
    private Double cetAnualApuradoPct;
    private String classificacaoRisco = "Não avaliado";
    private String conclusaoTecnica = "";

    // MULTIPLICIDADES INTERNAS: 1 Resultado tem N Premissas, Inconsistências e Recomendações
    private List<String> premissas = new ArrayList<>();
    private List<String> inconsistencias = new ArrayList<>();
    private List<String> recomendacoes = new ArrayList<>();

    // Opcional: A tabela PRICE gerada para esse resultado (1 Resultado para N Linhas Price)
    private List<LinhaPrice> tabelaPrice = new ArrayList<>();
    private Map<String, Object> memoriaCalculo = new HashMap<>();
}
