package br.com.mpgsistemas.revisionalweb.api.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

// DTO serializado como JSONB em CasoRevisional.contrato (dados brutos extraidos via OCR).
// NAO e uma tabela: vive dentro do JSONB do caso. Wrappers em todos numericos (null safety OCR).
@Data
public class DadosContrato {

    // --- Dados de Identificação e Cadastro ---
    private String clienteNome = "";
    private String clienteCpf = "";
    private String auditorCpf = "";
    private String auditorNome = "";
    private String oab = "";

    // --- Dados da Instituição e Contrato ---
    private String instituicao = "";
    private String instituicaoCnpj = "";
    private String contratoNumero = "";
    private String modalidade = "Financiamento de veículos - Pessoa Física";
    private String dataContrato = "";

    // --- Dados do Veículo e Valores Principais ---
    private String veiculoDescricao = "";
    private Double valorVeiculo;
    private Double valorEntrada;
    private Double valorFinanciado;
    private Double valorLiquidoLiberado;
    private Integer prazoMeses;
    private Double valorParcela;
    private String sistemaAmortizacao = "PRICE";

    // --- Taxas de Juros e CET ---
    private Double taxaJurosMensalPct;
    private Double taxaJurosAnualPct;
    private Double cetMensalPct;
    private Double cetAnualPct;

    // --- Encargos, Tarifas e Seguros ---
    private Double iof;
    private Double tarifaCadastro;
    private Double tarifaAvaliacaoBem;
    private Double tarifaRegistroContrato;
    private Double gravame;
    private Double seguro;
    private Double outrosEncargos;
    private String descricaoOutrosEncargos = "";

    // --- Trilha de Auditoria, Segurança e Metadados (OCR) ---
    private String contratoArquivo = "";
    private String hashContrato = "";
    private String hashTextoExtraido = "";
    private String amostraTextoExtraido = "";
    private String observacoes = "";

    // --- Composições ---
    private Map<String, CampoExtraido> camposExtraidos = new HashMap<>();
    private Map<String, String> metadadosExtracao = new HashMap<>();
}
