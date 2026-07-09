package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.LinhaPrice;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;
import br.com.mpgsistemas.revisionalweb.api.model.ParametrosSistema;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Motor matematico pericial. NAO usa libs financeiras de terceiros: precisao e
 * memoria de calculo reproduziveis para laudo. Recebe DTOs, devolve DTO; nao
 * conhece banco nem HTTP (Clean Architecture).
 *
 * Portado do nucleo Python (revisional_web/financial.py).
 */
@Service
public class CalculadoraFinanceiraService {

    private static final double EPS = 1e-12;

    private final ParametrosSistema params;

    public CalculadoraFinanceiraService(ParametrosSistema params) {
        this.params = params;
    }

    // ---------------------------------------------------------------------
    // Conversao de taxa
    // ---------------------------------------------------------------------

    /** Taxa mensal (decimal) -> taxa anual efetiva (decimal). */
    public double mensalParaAnual(double mensalDecimal) {
        return Math.pow(1.0 + mensalDecimal, 12) - 1.0;
    }

    /** Taxa anual (decimal) -> taxa mensal equivalente (decimal). */
    public double anualParaMensal(double anualDecimal) {
        return Math.pow(1.0 + anualDecimal, 1.0 / 12.0) - 1.0;
    }

    // ---------------------------------------------------------------------
    // PRICE
    // ---------------------------------------------------------------------

    /** PMT = PV * i / (1 - (1+i)^-n). */
    public double pmtPrice(double principal, double iMensal, int n) {
        if (principal < 0 || n <= 0) {
            throw new IllegalArgumentException("Principal e prazo devem ser positivos.");
        }
        if (Math.abs(iMensal) < EPS) {
            return principal / n;
        }
        return principal * iMensal / (1.0 - Math.pow(1.0 + iMensal, -n));
    }

    /**
     * Engenharia reversa da taxa mensal efetiva pela PRICE via bissecao.
     * Deterministica para auditoria/reproducao em pericia. Retorna null quando
     * dados insuficientes ou nao ha raiz.
     */
    public Double apurarTaxaMensal(Double principal, Double parcela, Integer n) {
        if (principal == null || parcela == null || n == null) return null;
        if (principal <= 0 || parcela <= 0 || n <= 0) return null;
        // Soma das parcelas nao cobre o principal -> taxa nula.
        if (parcela * n <= principal) return 0.0;

        double lo = 0.0;
        double hi = 1.0;
        // Expande o teto enquanto a parcela calculada nao alcanca a contratada.
        while (f(principal, parcela, n, hi) < 0 && hi < 10) {
            hi *= 2;
        }
        if (hi >= 10 && f(principal, parcela, n, hi) < 0) return null;

        for (int i = 0; i < params.getMaxIteracoesBissecao(); i++) {
            double mid = (lo + hi) / 2;
            if (f(principal, parcela, n, mid) > 0) hi = mid;
            else lo = mid;
        }
        return (lo + hi) / 2;
    }

    private double f(double principal, double parcela, int n, double r) {
        return pmtPrice(principal, r, n) - parcela;
    }

    /** Tabela PRICE completa (uma LinhaPrice por parcela). */
    public List<LinhaPrice> tabelaPrice(double principal, double iMensal, int n) {
        double parcela = pmtPrice(principal, iMensal, n);
        double saldo = principal;
        List<LinhaPrice> linhas = new ArrayList<>(n);
        for (int k = 1; k <= n; k++) {
            double saldoInicial = saldo;
            double juros = saldo * iMensal;
            double amortizacao = parcela - juros;
            saldo = Math.max(0.0, saldo - amortizacao);
            linhas.add(new LinhaPrice(k, saldoInicial, juros, amortizacao, parcela, saldo));
        }
        return linhas;
    }

    // ---------------------------------------------------------------------
    // CET (XIRR / VPL)
    // ---------------------------------------------------------------------

    /**
     * CET anual estimado por fluxo de caixa em dias corridos. Entrada positiva no
     * dia 0; parcelas negativas nos vencimentos estimados. Retorna a taxa anual
     * (decimal) que zera o VPL, ou null se nao houver raiz/dados.
     */
    public Double cetAnual(Double principal, Double parcela, Integer n) {
        if (principal == null || parcela == null || n == null || n <= 0) return null;
        if (principal <= 0 || parcela <= 0) return null;

        double[][] fluxos = new double[n + 1][2];
        fluxos[0] = new double[]{0, principal};
        for (int i = 1; i <= n; i++) {
            fluxos[i] = new double[]{Math.round(i * 365.0 / 12.0), -parcela};
        }

        double lo = -0.9999;
        double hi = 10.0;
        double fLo = npv(fluxos, lo);
        double fHi = npv(fluxos, hi);
        int tentativas = 0;
        while (fLo * fHi > 0 && hi < 1000 && tentativas < 40) {
            hi *= 2;
            fHi = npv(fluxos, hi);
            tentativas++;
        }
        if (fLo * fHi > 0) return null;

        for (int i = 0; i < params.getMaxIteracoesXirr(); i++) {
            double mid = (lo + hi) / 2;
            double fMid = npv(fluxos, mid);
            if (Math.abs(fMid) < 1e-8) return mid;
            if (fLo * fMid <= 0) {
                hi = mid;
            } else {
                lo = mid;
                fLo = fMid;
            }
        }
        return (lo + hi) / 2;
    }

    private double npv(double[][] fluxos, double taxa) {
        double total = 0.0;
        for (double[] fluxo : fluxos) {
            double dias = fluxo[0];
            double valor = fluxo[1];
            total += valor / Math.pow(1.0 + taxa, dias / 365.0);
        }
        return total;
    }

    // ---------------------------------------------------------------------
    // Analise completa do contrato (orquestra o nucleo acima)
    // ---------------------------------------------------------------------

    public ResultadoCalculo analisar(DadosContrato contrato, ReferenciaMercado mercado) {
        ResultadoCalculo r = new ResultadoCalculo();
        r.setParcelaContratual(contrato.getValorParcela());
        r.getPremissas().add("Sistema de amortizacao presumido: PRICE, salvo se o contrato comprovar outro metodo.");
        r.getPremissas().add("A taxa media BCB e parametro tecnico de mercado; nao e teto legal automatico nem substitui prova do caso concreto.");
        r.getPremissas().add("A conclusao juridica exige analise do instrumento integral, CET, tarifas, seguros, autorizacao de servicos e perfil de risco da operacao.");

        Double principal = primeiroPositivo(contrato.getValorFinanciado(), contrato.getValorLiquidoLiberado());
        Integer n = contrato.getPrazoMeses();
        Double parcela = contrato.getValorParcela();

        if (principal == null || principal <= 0)
            r.getInconsistencias().add("Valor financiado ou valor liquido liberado nao informado/invalido.");
        if (n == null || n <= 0)
            r.getInconsistencias().add("Prazo em meses nao informado ou invalido.");
        if (parcela == null || parcela <= 0)
            r.getInconsistencias().add("Valor da parcela nao informado ou invalido.");

        // --- Taxa contratual (mensal decimal) ---
        Double contratoM = null;
        if (contrato.getTaxaJurosMensalPct() != null) {
            contratoM = contrato.getTaxaJurosMensalPct() / 100.0;
            r.getPremissas().add("Taxa mensal contratual utilizada conforme campo validado pelo operador.");
        } else if (contrato.getTaxaJurosAnualPct() != null) {
            contratoM = anualParaMensal(contrato.getTaxaJurosAnualPct() / 100.0);
            r.getPremissas().add("Taxa mensal equivalente derivada da taxa anual contratual.");
        } else if (principal != null && parcela != null && n != null) {
            contratoM = apurarTaxaMensal(principal, parcela, n);
            r.getPremissas().add("Taxa mensal contratual apurada por engenharia reversa da prestacao, prazo e principal financiado.");
        } else {
            r.getInconsistencias().add("Taxa de juros contratual nao pode ser apurada.");
        }

        if (contratoM != null) {
            r.setTaxaMensalContratoApuradaPct(contratoM * 100);
            r.setTaxaAnualContratoApuradaPct(mensalParaAnual(contratoM) * 100);
        }

        // --- Taxa de mercado (mensal decimal) ---
        Double mercadoM = null;
        if (mercado != null && mercado.getTaxaMensalInstituicaoPct() != null) {
            mercadoM = mercado.getTaxaMensalInstituicaoPct() / 100.0;
            r.getPremissas().add("Taxa de comparacao preferencial: media por instituicao informada/validada pelo operador.");
        } else if (mercado != null && mercado.getTaxaMensalPct() != null) {
            mercadoM = mercado.getTaxaMensalPct() / 100.0;
            r.getPremissas().add("Taxa de comparacao: media BCB SGS 25471 para aquisicao de veiculos PF.");
        } else if (mercado != null && mercado.getTaxaAnualPct() != null) {
            mercadoM = anualParaMensal(mercado.getTaxaAnualPct() / 100.0);
            r.getPremissas().add("Taxa de comparacao derivada da taxa anual de mercado informada.");
        } else {
            r.getInconsistencias().add("Taxa de mercado nao informada ou indisponivel. Atualize pelo BCB ou informe taxa validada.");
        }

        if (mercadoM != null) {
            r.setTaxaMensalMercadoPct(mercadoM * 100);
            r.setTaxaAnualMercadoPct(mensalParaAnual(mercadoM) * 100);
        }

        // --- CET apurado ---
        Double cet = cetAnual(principal, parcela, n);
        if (cet != null) r.setCetAnualApuradoPct(cet * 100);

        // --- Recalculo pela taxa de mercado + diferencas ---
        if (principal != null && n != null && mercadoM != null) {
            double parcelaMkt = pmtPrice(principal, mercadoM, n);
            r.setParcelaRecalculadaMercado(parcelaMkt);
            r.setTotalRecalculado(parcelaMkt * n);
            if (parcela != null) {
                r.setDiferencaMensal(parcela - parcelaMkt);
                r.setDiferencaTotalNominal((parcela - parcelaMkt) * n);
                r.setTotalContratado(parcela * n);
            }
        }

        // --- Spread + classificacao de risco ---
        if (contratoM != null && mercadoM != null && mercadoM > 0) {
            r.setSpreadPontosPercentuaisMes((contratoM - mercadoM) * 100);
            r.setSpreadPercentualSobreMercado((contratoM / mercadoM - 1.0) * 100);
            double ratio = contratoM / mercadoM;
            r.getMemoriaCalculo().put("ratio_taxa_contratual_sobre_referencia", ratio);
            if (ratio >= params.getToleranciaForte()) {
                r.setClassificacaoRisco("Indicio tecnico forte de onerosidade excessiva");
                r.getRecomendacoes().add("Priorizar notificacao extrajudicial e analise de acao revisional com pedido de exibicao da memoria de calculo e demonstrativo do CET.");
            } else if (ratio >= params.getToleranciaModerada()) {
                r.setClassificacaoRisco("Indicio tecnico moderado de onerosidade excessiva");
                r.getRecomendacoes().add("Solicitar composicao do CET, criterio de risco, autorizacao de tarifas/seguros e planilha de evolucao do saldo.");
            } else if (ratio > 1.0) {
                r.setClassificacaoRisco("Taxa acima da media, sem conclusao isolada de abusividade");
                r.getRecomendacoes().add("Prosseguir com auditoria de CET, tarifas, seguros, IOF, capitalizacao e clareza da pactuacao.");
            } else {
                r.setClassificacaoRisco("Sem indicio matematico de juros acima da referencia informada");
                r.getRecomendacoes().add("Manter analise de CET, tarifas, seguros e validade documental, ainda que a taxa esteja dentro da referencia.");
            }
        }

        // --- Consistencia: taxa anual contratual vs. equivalente da mensal ---
        if (contrato.getTaxaJurosMensalPct() != null && contrato.getTaxaJurosAnualPct() != null) {
            double equiv = mensalParaAnual(contrato.getTaxaJurosMensalPct() / 100.0) * 100;
            r.getMemoriaCalculo().put("taxa_anual_equivalente_calculada_pct", equiv);
            if (Math.abs(equiv - contrato.getTaxaJurosAnualPct()) > 0.8) {
                r.getInconsistencias().add(String.format(
                        "Taxa anual contratual (%.2f%%) diverge da taxa efetiva anual equivalente a mensal (%.2f%%).",
                        contrato.getTaxaJurosAnualPct(), equiv));
            }
        }

        // --- Consistencia: CET informado vs. estimado ---
        if (contrato.getCetAnualPct() == null) {
            r.getInconsistencias().add("CET anual nao informado/identificado. Exige conferencia do contrato e do demonstrativo previo do CET.");
        } else if (r.getCetAnualApuradoPct() != null
                && Math.abs(contrato.getCetAnualPct() - r.getCetAnualApuradoPct()) > 1.25) {
            r.getInconsistencias().add(String.format(
                    "CET anual informado (%.2f%%) diverge do CET estimado pelos fluxos (%.2f%%).",
                    contrato.getCetAnualPct(), r.getCetAnualApuradoPct()));
        }

        // --- Tabela PRICE (memoria de calculo visivel) ---
        if (principal != null && n != null && contratoM != null) {
            r.setTabelaPrice(tabelaPrice(principal, contratoM, n));
        }

        r.getMemoriaCalculo().put("principal_utilizado", principal);
        r.getMemoriaCalculo().put("prazo_meses", n);
        r.getMemoriaCalculo().put("parcela_contratual", parcela);
        r.getMemoriaCalculo().put("taxa_contratual_mensal_decimal", contratoM);
        r.getMemoriaCalculo().put("taxa_referencia_mensal_decimal", mercadoM);
        r.getMemoriaCalculo().put("tolerancia_moderada", params.getToleranciaModerada());
        r.getMemoriaCalculo().put("tolerancia_forte", params.getToleranciaForte());
        r.getMemoriaCalculo().put("formula_price", "PMT = PV * i / (1 - (1+i)^-n)");

        r.setStatus(r.getInconsistencias().isEmpty() ? "Concluído" : "Concluído com ressalvas");
        if ("Não avaliado".equals(r.getClassificacaoRisco())) {
            r.setClassificacaoRisco("Dados insuficientes");
        }
        r.setConclusaoTecnica(montarConclusao(r));
        return r;
    }

    private String montarConclusao(ResultadoCalculo r) {
        String c = r.getClassificacaoRisco();
        if (c.startsWith("Indicio tecnico forte"))
            return "Ha indicios tecnicos relevantes de onerosidade excessiva. Recomenda-se auditoria documental completa, notificacao administrativa e avaliacao de acao revisional, preservando a necessidade de prova concreta.";
        if (c.startsWith("Indicio tecnico moderado"))
            return "Ha indicios tecnicos moderados de descolamento da taxa contratual em relacao ao parametro de mercado. A conclusao juridica depende do contrato integral, CET, tarifas e justificativas de risco.";
        if (c.startsWith("Taxa acima"))
            return "A taxa contratual esta acima da referencia informada, mas isso nao permite conclusao isolada de abusividade. O relatorio indica pontos de investigacao e prova tecnica.";
        if (c.startsWith("Sem indicio"))
            return "Com os dados fornecidos, nao foi identificado indicio matematico de juros remuneratorios acima da referencia informada. A auditoria deve prosseguir sobre CET, tarifas e seguros.";
        return "Nao foi possivel concluir tecnicamente por insuficiencia de dados ou ausencia de referencia de mercado valida.";
    }

    private Double primeiroPositivo(Double a, Double b) {
        if (a != null && a > 0) return a;
        if (b != null && b > 0) return b;
        return a != null ? a : b;
    }
}
