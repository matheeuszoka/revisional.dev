package br.com.mpgsistemas.revisionalweb.api.model;

import lombok.Data;
import org.springframework.stereotype.Component;

// Singleton injetavel de parametros de dominio (evita magic numbers). NAO e tabela.
@Component
@Data
public class ParametrosSistema {

    private double toleranciaModerada = 1.50;
    private double toleranciaForte = 2.00;
    private int maxIteracoesBissecao = 180;
    private int maxIteracoesXirr = 220;

    // --- Padrões de Domínio ---
    private String sistemaAmortizacaoPadrao = "PRICE";
    private String modalidadePadrao = "Financiamento de veículos - Pessoa Física";

    // --- Integração Banco Central ---
    private String bcbFontePadrao = "BCB SGS 25471";
    private String bcbCodigoModalidade = "25471";
    private String bcbUrlApi = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.25471/dados?formato=json";
    private int bcbTimeoutSegundos = 8;
}
