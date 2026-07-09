package br.com.mpgsistemas.revisionalweb.api.config;

import br.com.mpgsistemas.revisionalweb.api.dto.FonteNormativa;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Matriz normativa estática (port de norms.py / NORMATIVE_SOURCES). Usada pela
 * auditoria (AuditPackage.sources), pelos laudos PDF e pelo endpoint GET /api/normas.
 */
@Component
public class MatrizNormativa {

    private static final List<FonteNormativa> FONTES = List.of(
            new FonteNormativa(
                    "Taxa média de mercado - veículos PF",
                    "Banco Central do Brasil - SGS 25471",
                    "https://dadosabertos.bcb.gov.br/dataset/25471-taxa-media-mensal-de-juros-das-operacoes-de-credito-com-recursos-livres---pessoas-fisicas---a",
                    "Parâmetro técnico de comparação para aquisição de veículos por pessoa física; não é teto legal automático."),
            new FonteNormativa(
                    "Taxas por instituição e modalidade",
                    "Banco Central do Brasil - Taxas de juros de operações de crédito",
                    "https://www.bcb.gov.br/estatisticas/reporttxjuros/",
                    "Fonte para conferência manual da taxa média praticada por instituição/modalidade quando disponível."),
            new FonteNormativa(
                    "Custo Efetivo Total - CET",
                    "Resolução CMN nº 4.881/2020",
                    "https://www.bcb.gov.br/content/estabilidadefinanceira/especialnor/Resolu%C3%A7%C3%A3o4881.pdf",
                    "Auditoria da informação prévia do CET e composição por juros, tarifas, tributos, seguros e demais despesas vinculadas."),
            new FonteNormativa(
                    "Tarifas bancárias",
                    "Resolução CMN nº 3.919/2010",
                    "https://www.bcb.gov.br/pre/normativos/res/2010/pdf/res_3919_v4_p.pdf",
                    "Verificação de previsão contratual, autorização/solicitação e correspondência entre tarifa e serviço."),
            new FonteNormativa(
                    "Cédula de Crédito Bancário",
                    "Lei nº 10.931/2004",
                    "https://www.planalto.gov.br/ccivil_03/_ato2004-2006/2004/lei/l10.931.htm",
                    "Qualificação do instrumento contratual, requisitos de liquidez, certeza e exigibilidade, conforme caso concreto."),
            new FonteNormativa(
                    "Proteção contratual do consumidor",
                    "Código de Defesa do Consumidor - Lei nº 8.078/1990",
                    "https://www.planalto.gov.br/ccivil_03/leis/l8078compilado.htm",
                    "Base de análise de informação adequada, transparência, onerosidade excessiva e práticas abusivas."),
            new FonteNormativa(
                    "IOF",
                    "Decreto nº 6.306/2007",
                    "https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2007/decreto/d6306.htm",
                    "Verificação do valor de IOF informado e sua presença no CET/valor financiado."),
            new FonteNormativa(
                    "Juros remuneratórios e taxa média",
                    "STJ - Súmula 530",
                    "https://ww2.stj.jus.br/docs_internet/revista/eletronica/stj-revista-sumulas-2017_44_capSumulas530-536.pdf",
                    "Orientação sobre utilização da taxa média de mercado quando ausente ou abusiva a taxa pactuada."),
            new FonteNormativa(
                    "Juros superiores a 12% ao ano",
                    "STJ - Súmula 382",
                    "https://ww2.stj.jus.br/docs_internet/revista/eletronica/stj-revista-sumulas-2013_35_capSumula382.pdf",
                    "Advertência técnica: taxa acima de 12% ao ano, isoladamente, não caracteriza abusividade."),
            new FonteNormativa(
                    "Financiamento de veículo, busca e apreensão e mora",
                    "Decreto-Lei nº 911/1969",
                    "https://www.planalto.gov.br/ccivil_03/decreto-lei/1965-1988/del0911.htm",
                    "Elemento contextual para estratégia processual quando houver alienação fiduciária e risco de busca e apreensão.")
    );

    public List<FonteNormativa> fontes() {
        return FONTES;
    }
}
