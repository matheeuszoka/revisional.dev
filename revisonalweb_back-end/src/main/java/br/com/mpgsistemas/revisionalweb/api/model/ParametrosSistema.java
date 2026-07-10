package br.com.mpgsistemas.revisionalweb.api.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Singleton injetavel de parametros de dominio (evita magic numbers). NAO e tabela.
// Campos com @Value aceitam override por variavel de ambiente; os demais sao fixos de dominio.
@Component
@Data
public class ParametrosSistema {

    private double toleranciaModerada = 1.50;
    private double toleranciaForte = 2.00;
    private int maxIteracoesBissecao = 180;
    private int maxIteracoesXirr = 220;

    // --- Auditoria pericial (espelha audit.py) ---
    // Score = max(0, min(100, 100 - falhas*penalidadeFalha - avisos*penalidadeAviso))
    private int auditPenalidadeFalha = 8;
    private int auditPenalidadeAviso = 4;
    private double auditConfiancaMinimaExtracao = 0.70;
    private double auditSpreadMaxPctSobreMercado = 50.0;
    private int auditMaxInconsistencias = 8;
    @Value("${app.versao:2.0.0}")
    private String softwareVersao;

    // --- Padrões de Domínio ---
    private String sistemaAmortizacaoPadrao = "PRICE";
    private String modalidadePadrao = "Financiamento de veículos - Pessoa Física";

    // --- Integração Banco Central ---
    private String bcbFontePadrao = "BCB SGS 25471";
    private String bcbCodigoModalidade = "25471";
    private String bcbUrlApi = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.25471/dados?formato=json";
    private int bcbTimeoutSegundos = 8;

    // --- OCR (Tesseract via Tess4J) ---
    @Value("${ocr.tessdata-path:}")
    private String ocrTessdataPath;
    @Value("${ocr.idioma:por}")
    private String ocrIdioma;
    @Value("${ocr.dpi:300}")
    private int ocrDpi;
    @Value("${ocr.max-paginas:25}")
    private int ocrMaxPaginas;
    // Page Segmentation Mode do Tesseract. Sem setar explicitamente, o Tess4J deixa o
    // engine num default que cola as células de tabela ("Valordoveiculoavista r$9000000");
    // PSM 3 (auto) lê o quadro-resumo de CDC quase perfeito. Validado com contrato real.
    @Value("${ocr.psm:3}")
    private int ocrPsm;

    // --- Extração por IA (OpenRouter; opcional). apiKey vazia => IA desativada. ---
    @Value("${openrouter.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;
    @Value("${openrouter.api-key:}")
    private String openRouterApiKey;
    @Value("${openrouter.model:google/gemini-2.0-flash-001}")
    private String openRouterModel;
    @Value("${openrouter.timeout-segundos:45}")
    private int openRouterTimeoutSegundos;
    @Value("${openrouter.referer:}")
    private String openRouterReferer;
    @Value("${openrouter.titulo:}")
    private String openRouterTitulo;
    @Value("${openrouter.max-chars:14000}")
    private int openRouterMaxChars;
    // Contrato real tem 20-60k chars: o texto é fatiado em janelas de max-chars e a IA
    // roda por janela (merge por confiança). max-chunks limita custo/tempo por documento.
    @Value("${openrouter.max-chunks:6}")
    private int openRouterMaxChunks;
    @Value("${openrouter.sobreposicao-chars:800}")
    private int openRouterSobreposicaoChars;
}
