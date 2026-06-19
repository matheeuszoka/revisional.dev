package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "table_parametros_sistema")
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
