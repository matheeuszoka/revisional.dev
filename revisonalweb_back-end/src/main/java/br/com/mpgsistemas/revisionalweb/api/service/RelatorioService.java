package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.config.MatrizNormativa;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditItem;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditPackage;
import br.com.mpgsistemas.revisionalweb.api.dto.FonteNormativa;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;
import br.com.mpgsistemas.revisionalweb.api.util.FormatoBr;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Gera os 4 laudos PDF (port de reports.py via OpenPDF): parecer técnico-jurídico,
 * gerencial executivo, notificação extrajudicial e elementos para inicial revisional.
 * Service isolado: recebe DTOs + AuditPackage, devolve bytes. Não conhece banco nem HTTP.
 */
@Service
public class RelatorioService {

    private static final float CM = 28.35f;
    private static final Color HEADER_BG = new Color(0xE8, 0xEE, 0xF7);
    private static final Color GRID = new Color(0xB8, 0xC2, 0xCC);
    private static final Color ZEBRA = new Color(0xF8, 0xFA, 0xFC);

    private static final String[] TIPOS_PACOTE = {"parecer", "gerencial", "notificacao", "elementos"};

    private final MatrizNormativa normas;
    private final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public RelatorioService(MatrizNormativa normas) {
        this.normas = normas;
    }

    /**
     * Pacote ZIP do caso (port de generate_bundle): os 4 laudos PDF + auditoria.json.
     * Documento único para instruir processo administrativo/judicial.
     */
    public byte[] gerarPacote(DadosContrato contrato, ReferenciaMercado mercado,
                              ResultadoCalculo resultado, AuditPackage audit) {
        String auditId = (audit != null && audit.getAuditId() != null) ? audit.getAuditId() : "sem_auditoria";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (String tipo : TIPOS_PACOTE) {
                zip.putNextEntry(new ZipEntry(tipo + "_" + auditId + ".pdf"));
                zip.write(gerar(tipo, contrato, mercado, resultado, audit));
                zip.closeEntry();
            }
            zip.putNextEntry(new ZipEntry("auditoria_" + auditId + ".json"));
            zip.write(jsonMapper.writeValueAsBytes(audit));
            zip.closeEntry();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gerar o pacote ZIP.", e);
        }
        return baos.toByteArray();
    }

    public byte[] gerar(String tipo, DadosContrato contrato, ReferenciaMercado mercado,
                        ResultadoCalculo resultado, AuditPackage audit) {
        if (contrato == null) contrato = new DadosContrato();
        if (mercado == null) mercado = new ReferenciaMercado();
        if (resultado == null) resultado = new ResultadoCalculo();

        String t = tipo == null ? "" : tipo.toLowerCase();
        String titulo = switch (t) {
            case "parecer" -> "PARECER TÉCNICO-JURÍDICO REVISIONAL - FINANCIAMENTO/EMPRÉSTIMO BANCÁRIO";
            case "gerencial" -> "RELATÓRIO GERENCIAL EXECUTIVO - AUDITORIA DE CONTRATO BANCÁRIO";
            case "notificacao" -> "NOTIFICAÇÃO EXTRAJUDICIAL - REVISÃO ADMINISTRATIVA DE CONTRATO BANCÁRIO";
            case "elementos" -> "ELEMENTOS TÉCNICOS PARA PETIÇÃO INICIAL REVISIONAL";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de relatório inválido.");
        };

        Document doc = new Document(PageSize.A4, 1.8f * CM, 1.8f * CM, 1.8f * CM, 1.7f * CM);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(doc, baos);
            doc.open();

            if (!"gerencial".equals(t)) {
                capa(doc, titulo, contrato, audit);
            } else {
                doc.add(titulo(titulo));
            }

            switch (t) {
                case "parecer" -> addParecer(doc, contrato, mercado, resultado, audit);
                case "gerencial" -> addGerencial(doc, contrato, resultado, audit);
                case "notificacao" -> addNotificacao(doc, mercado, resultado);
                case "elementos" -> addElementos(doc, contrato, resultado, audit);
                default -> { /* já validado */ }
            }
            doc.close();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao gerar o relatório PDF.", e);
        }
        return baos.toByteArray();
    }

    // ---------------------------------------------------------------- seções
    private void capa(Document doc, String titulo, DadosContrato c, AuditPackage audit) throws Exception {
        doc.add(titulo(titulo));
        doc.add(corpo("Cliente: " + ou(c.getClienteNome(), "Não informado")));
        doc.add(corpo("CPF: " + ouCpf(c.getClienteCpf())));
        doc.add(corpo("Instituição financeira/cooperativa: " + ou(c.getInstituicao(), "Não informada")));
        doc.add(corpo("Contrato/CCB: " + ou(c.getContratoNumero(), "Não informado")));
        doc.add(corpo("Auditor: " + ou(c.getAuditorNome(), "Não informado")
                + " - CPF " + ouCpf(c.getAuditorCpf()) + " - OAB " + ou(c.getOab(), "Não informada")));
        doc.add(corpo("Data de emissão: " + FormatoBr.agora() + ". ID de auditoria: "
                + (audit != null ? ou(audit.getAuditId(), "não gerado") : "não gerado")
                + ". Score de auditoria: " + (audit != null ? audit.getScore() : 0) + "/100."));
        doc.add(corpo("Documento técnico auxiliar para atuação administrativa e judicial. A conclusão deve ser "
                + "confirmada pelo advogado responsável mediante conferência do contrato integral e demais provas."));
        doc.newPage();
    }

    private void addParecer(Document doc, DadosContrato c, ReferenciaMercado m,
                            ResultadoCalculo r, AuditPackage audit) throws Exception {
        doc.add(heading("1. Metodologia de análise"));
        doc.add(corpo("O sistema realizou extração textual/OCR do documento anexado, estruturou os campos essenciais, "
                + "permitiu validação manual pelo operador, apurou a taxa efetiva pela fórmula PRICE quando necessário, "
                + "confrontou a operação com referência de mercado informada/BCB e gerou trilha de auditoria com hashes, "
                + "fingerprint de cálculo e matriz de inconsistências."));
        doc.add(heading("2. Dados estruturados do contrato"));
        doc.add(tabelaContrato(c));
        doc.add(heading("3. Cálculos financeiros e diagnóstico"));
        doc.add(tabelaCalculo(r, m));
        doc.add(corpo("Conclusão técnica: " + ou(r.getConclusaoTecnica(), "")));
        doc.add(heading("4. Inconsistências, premissas e recomendações"));
        lista(doc, "Premissas", r.getPremissas());
        lista(doc, "Inconsistências", ouLista(r.getInconsistencias(),
                "Nenhuma inconsistência crítica registrada pelo motor de cálculo."));
        lista(doc, "Recomendações", r.getRecomendacoes());
        doc.add(heading("5. Fundamentos normativos e jurisprudenciais utilizados"));
        doc.add(tabelaNormas());
        doc.add(corpo("Nota técnica: a taxa média BCB não é teto legal automático. O relatório aponta indícios e "
                + "elementos probatórios, cabendo ao advogado responsável definir tese, pedidos e estratégia processual "
                + "conforme o caso concreto."));
        doc.add(heading("6. Trilha de auditoria"));
        blocoAuditoria(doc, audit);
    }

    private void addGerencial(Document doc, DadosContrato c, ResultadoCalculo r, AuditPackage audit) throws Exception {
        PdfPTable t = tabela(new float[]{5.4f, 10.6f});
        cabecalho(t, "Indicador", "Valor");
        linha(t, "Cliente", ou(c.getClienteNome(), "Não informado"));
        linha(t, "Instituição", ou(c.getInstituicao(), "Não informada"));
        linha(t, "Contrato", ou(c.getContratoNumero(), "Não informado"));
        linha(t, "Score de auditoria", (audit != null ? audit.getScore() : 0) + "/100");
        linha(t, "Classificação", ou(r.getClassificacaoRisco(), "Não avaliado"));
        linha(t, "Taxa contratual mensal", FormatoBr.pct(r.getTaxaMensalContratoApuradaPct()));
        linha(t, "Taxa referência mensal", FormatoBr.pct(r.getTaxaMensalMercadoPct()));
        linha(t, "Diferença mensal estimada", FormatoBr.moeda(r.getDiferencaMensal()));
        linha(t, "Diferença total nominal", FormatoBr.moeda(r.getDiferencaTotalNominal()));
        linha(t, "Conclusão", ou(r.getConclusaoTecnica(), ""));
        doc.add(t);

        doc.add(heading("Pontos prioritários"));
        for (String inc : ouLista(r.getInconsistencias(),
                "Conferir contrato integral, CET, tarifas e seguros antes de eventual ajuizamento.")) {
            doc.add(bullet(inc));
        }
        doc.add(heading("Recomendações executivas"));
        for (String rec : ouLista(r.getRecomendacoes(),
                "Validar manualmente os dados extraídos e confrontar a documentação integral antes de qualquer providência.")) {
            doc.add(bullet(rec));
        }
        doc.add(heading("Trilha técnica resumida"));
        doc.add(small("Hash do documento: " + auditCampo(audit, "doc") + ". Hash do texto extraído: "
                + auditCampo(audit, "texto") + ". Fingerprint de cálculo: " + auditCampo(audit, "fp") + "."));
        PdfPTable ta = tabela(new float[]{2.4f, 2.6f, 11f});
        cabecalho(ta, "Código", "Status", "Detalhe");
        if (audit != null) {
            for (AuditItem it : audit.getItems()) {
                linha(ta, it.codigo(), it.status(), it.detalhe());
            }
        }
        doc.add(ta);
    }

    private void addNotificacao(Document doc, ReferenciaMercado m, ResultadoCalculo r) throws Exception {
        doc.add(corpo("À instituição financeira/cooperativa responsável pelo contrato acima identificado."));
        doc.add(corpo("Na qualidade de representante do consumidor/contratante, apresenta-se notificação para revisão "
                + "administrativa do contrato, com solicitação de documentos e esclarecimentos sobre taxa remuneratória, "
                + "CET, tarifas, seguros, IOF, registro/gravame, despesas de terceiros e memória de cálculo da evolução do saldo."));
        doc.add(heading("Resumo técnico da divergência"));
        doc.add(tabelaCalculo(r, m));
        doc.add(heading("Requerimentos administrativos"));
        String[] reqs = {
                "entrega do instrumento contratual completo, CCB, quadro-resumo, demonstrativo do CET e memória de cálculo;",
                "comprovação da autorização/solicitação e efetiva prestação dos serviços que originaram tarifas, seguros, avaliação de bem, registro, gravame e despesas de terceiros;",
                "esclarecimento da taxa mensal e anual efetiva aplicada e metodologia de capitalização/amortização;",
                "recálculo administrativo das parcelas e saldo, caso confirmada inconsistência, ausência de pactuação clara ou cobrança incompatível;",
                "suspensão de cobranças controvertidas e apresentação de proposta conciliatória, sem prejuízo de providências judiciais cabíveis.",
        };
        for (String req : reqs) doc.add(bullet(req));
        doc.add(corpo("A ausência de resposta adequada ou a manutenção de cobrança incompatível com a prova documental "
                + "poderá subsidiar ação revisional, pedido de exibição de documentos, tutela de urgência e demais medidas "
                + "processuais pertinentes."));
    }

    private void addElementos(Document doc, DadosContrato c, ResultadoCalculo r, AuditPackage audit) throws Exception {
        Double valor = c.getValorFinanciado() != null ? c.getValorFinanciado() : c.getValorLiquidoLiberado();
        doc.add(heading("1. Síntese fática sugerida"));
        doc.add(corpo("O consumidor firmou contrato bancário identificado como " + ou(c.getContratoNumero(), "não informado")
                + " junto a " + ou(c.getInstituicao(), "instituição não informada") + ", com valor financiado de "
                + FormatoBr.moeda(valor) + ", prazo de " + FormatoBr.inteiro(c.getPrazoMeses()) + " meses e parcela de "
                + FormatoBr.moeda(c.getValorParcela()) + "."));
        doc.add(heading("2. Pontos de prova"));
        String[] provas = {
                "contrato integral/CCB, quadro-resumo, demonstrativo do CET e memória de cálculo;",
                "boletos, comprovantes de pagamento e histórico de evolução do saldo;",
                "consulta BCB de taxa média da modalidade e, quando aplicável, taxa por instituição;",
                "prova de autorização de seguros, tarifas e serviços de terceiros;",
                "auditoria técnica com hash documental e fingerprint dos cálculos.",
        };
        for (String p : provas) doc.add(bullet(p));
        doc.add(heading("3. Pedidos a avaliar pelo advogado"));
        String[] pedidos = {
                "exibição de documentos e memória de cálculo;",
                "revisão de juros remuneratórios, caso demonstrada abusividade concreta ou ausência de pactuação clara;",
                "recálculo das parcelas/saldo e autorização para pagamento/deposito do valor incontroverso;",
                "declaração de nulidade/repetição de encargos indevidos, conforme prova;",
                "tutela de urgência contra negativação, busca e apreensão ou atos constritivos, conforme o risco do caso.",
        };
        for (String p : pedidos) doc.add(bullet(p));
        doc.add(heading("4. Matriz de inconsistências"));
        lista(doc, "Inconsistências", ouLista(r.getInconsistencias(),
                "Nenhuma inconsistência crítica registrada pelo motor de cálculo."));
        doc.add(heading("5. Auditoria e cadeia de custódia documental"));
        blocoAuditoria(doc, audit);
    }

    // ---------------------------------------------------------------- tabelas
    private PdfPTable tabelaContrato(DadosContrato c) {
        PdfPTable t = tabela(new float[]{5.2f, 10.8f});
        cabecalho(t, "Campo", "Informação");
        linha(t, "Cliente", ou(c.getClienteNome(), "Não informado"));
        linha(t, "CPF", ouCpf(c.getClienteCpf()));
        linha(t, "Instituição", ou(c.getInstituicao(), "Não informada"));
        linha(t, "CNPJ instituição", ou(c.getInstituicaoCnpj(), "Não informado"));
        linha(t, "Contrato/CCB", ou(c.getContratoNumero(), "Não informado"));
        linha(t, "Data do contrato", ou(c.getDataContrato(), "Não informada"));
        linha(t, "Modalidade", ou(c.getModalidade(), "Não informada"));
        linha(t, "Veículo/Bem", ou(c.getVeiculoDescricao(), "Não informado"));
        linha(t, "Valor do veículo", FormatoBr.moeda(c.getValorVeiculo()));
        linha(t, "Entrada", FormatoBr.moeda(c.getValorEntrada()));
        linha(t, "Valor financiado", FormatoBr.moeda(c.getValorFinanciado()));
        linha(t, "Valor líquido liberado", FormatoBr.moeda(c.getValorLiquidoLiberado()));
        linha(t, "Prazo", FormatoBr.inteiro(c.getPrazoMeses()) + (c.getPrazoMeses() != null ? " meses" : ""));
        linha(t, "Parcela", FormatoBr.moeda(c.getValorParcela()));
        linha(t, "Taxa mensal contratada", FormatoBr.pct(c.getTaxaJurosMensalPct()));
        linha(t, "Taxa anual contratada", FormatoBr.pct(c.getTaxaJurosAnualPct()));
        linha(t, "CET mensal", FormatoBr.pct(c.getCetMensalPct()));
        linha(t, "CET anual", FormatoBr.pct(c.getCetAnualPct()));
        linha(t, "IOF", FormatoBr.moeda(c.getIof()));
        linha(t, "Tarifa cadastro", FormatoBr.moeda(c.getTarifaCadastro()));
        linha(t, "Avaliação do bem", FormatoBr.moeda(c.getTarifaAvaliacaoBem()));
        linha(t, "Registro contrato", FormatoBr.moeda(c.getTarifaRegistroContrato()));
        linha(t, "Gravame", FormatoBr.moeda(c.getGravame()));
        linha(t, "Seguro", FormatoBr.moeda(c.getSeguro()));
        linha(t, "Outros encargos", FormatoBr.moeda(c.getOutrosEncargos()));
        return t;
    }

    private PdfPTable tabelaCalculo(ResultadoCalculo r, ReferenciaMercado m) {
        PdfPTable t = tabela(new float[]{6.8f, 9.2f});
        cabecalho(t, "Métrica", "Resultado");
        linha(t, "Fonte da taxa de referência", ou(m.getFonte(), "Não informada"));
        linha(t, "Data de referência", ou(m.getDataReferencia(), "Não informada"));
        linha(t, "Taxa contratual mensal apurada", FormatoBr.pct(r.getTaxaMensalContratoApuradaPct()));
        linha(t, "Taxa contratual anual efetiva", FormatoBr.pct(r.getTaxaAnualContratoApuradaPct()));
        linha(t, "Taxa referência mensal", FormatoBr.pct(r.getTaxaMensalMercadoPct()));
        linha(t, "Taxa referência anual", FormatoBr.pct(r.getTaxaAnualMercadoPct()));
        linha(t, "Diferença em pontos percentuais ao mês", FormatoBr.pct(r.getSpreadPontosPercentuaisMes()));
        linha(t, "Percentual acima da referência", FormatoBr.pct(r.getSpreadPercentualSobreMercado()));
        linha(t, "Parcela contratual", FormatoBr.moeda(r.getParcelaContratual()));
        linha(t, "Parcela recalculada pela referência", FormatoBr.moeda(r.getParcelaRecalculadaMercado()));
        linha(t, "Diferença mensal estimada", FormatoBr.moeda(r.getDiferencaMensal()));
        linha(t, "Diferença total nominal estimada", FormatoBr.moeda(r.getDiferencaTotalNominal()));
        linha(t, "Total contratado", FormatoBr.moeda(r.getTotalContratado()));
        linha(t, "Total recalculado", FormatoBr.moeda(r.getTotalRecalculado()));
        linha(t, "CET anual estimado", FormatoBr.pct(r.getCetAnualApuradoPct()));
        linha(t, "Classificação técnica", ou(r.getClassificacaoRisco(), "Não avaliado"));
        return t;
    }

    private PdfPTable tabelaNormas() {
        PdfPTable t = tabela(new float[]{4.2f, 4.6f, 7.2f});
        cabecalho(t, "Tema", "Fonte", "Uso técnico");
        for (FonteNormativa f : normas.fontes()) {
            linha(t, f.tema(), f.fonte(), f.usoNoSistema());
        }
        return t;
    }

    private void blocoAuditoria(Document doc, AuditPackage audit) throws Exception {
        if (audit == null) {
            doc.add(small("Auditoria não gerada."));
            return;
        }
        doc.add(small("ID: " + ou(audit.getAuditId(), "não gerado") + ". Criado em: "
                + ou(audit.getCreatedAt(), "não informado") + ". Versão: " + ou(audit.getSoftwareVersion(), "não informada")
                + ". Hash do documento: " + auditCampo(audit, "doc") + ". Hash do texto: " + auditCampo(audit, "texto")
                + ". Fingerprint: " + auditCampo(audit, "fp") + "."));
        PdfPTable t = tabela(new float[]{2f, 4f, 2.2f, 7.8f});
        cabecalho(t, "Código", "Item", "Status", "Detalhe");
        for (AuditItem it : audit.getItems()) {
            linha(t, it.codigo(), it.item(), it.status(), it.detalhe());
        }
        doc.add(t);
    }

    // ---------------------------------------------------------------- primitivas
    private Paragraph titulo(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES_BOLD, 11));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(14);
        return p;
    }

    private Paragraph heading(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES_BOLD, 11));
        p.setSpacingBefore(10);
        p.setSpacingAfter(6);
        return p;
    }

    private Paragraph corpo(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES_ROMAN, 11));
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setFirstLineIndent(5 * CM);
        p.setSpacingAfter(6);
        p.setLeading(16.5f);
        return p;
    }

    private Paragraph small(String texto) {
        Paragraph p = new Paragraph(texto, FontFactory.getFont(FontFactory.TIMES_ROMAN, 8.5f));
        p.setSpacingAfter(4);
        return p;
    }

    private Paragraph bullet(String texto) {
        Paragraph p = new Paragraph("-  " + texto, FontFactory.getFont(FontFactory.TIMES_ROMAN, 10));
        p.setIndentationLeft(0.5f * CM);
        p.setSpacingAfter(3);
        return p;
    }

    private void lista(Document doc, String titulo, List<String> itens) throws Exception {
        doc.add(heading(titulo));
        if (itens != null) {
            for (String it : itens) doc.add(bullet(it));
        }
    }

    private PdfPTable tabela(float[] larguras) {
        PdfPTable t = new PdfPTable(larguras.length);
        t.setWidthPercentage(100);
        try {
            t.setWidths(larguras);
        } catch (Exception ignored) {
            /* larguras inválidas: usa distribuição automática */
        }
        t.setSpacingBefore(4);
        t.setSpacingAfter(6);
        return t;
    }

    private void cabecalho(PdfPTable t, String... textos) {
        Font f = FontFactory.getFont(FontFactory.TIMES_BOLD, 7.4f);
        for (String texto : textos) {
            PdfPCell c = new PdfPCell(new Phrase(texto, f));
            c.setBackgroundColor(HEADER_BG);
            c.setBorderColor(GRID);
            c.setPadding(4);
            t.addCell(c);
        }
    }

    private void linha(PdfPTable t, String... textos) {
        Font f = FontFactory.getFont(FontFactory.TIMES_ROMAN, 7.2f);
        boolean zebra = (t.getRows().size() % 2) == 0;
        for (String texto : textos) {
            PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, f));
            c.setBorderColor(GRID);
            c.setPadding(4);
            if (zebra) c.setBackgroundColor(ZEBRA);
            t.addCell(c);
        }
    }

    // ---------------------------------------------------------------- helpers
    private static String ou(String valor, String alternativa) {
        return (valor != null && !valor.isBlank()) ? valor : alternativa;
    }

    private static String ouCpf(String cpf) {
        String f = FormatoBr.cpf(cpf);
        return (f == null || f.isBlank()) ? "Não informado" : f;
    }

    private static List<String> ouLista(List<String> itens, String padrao) {
        return (itens != null && !itens.isEmpty()) ? itens : List.of(padrao);
    }

    private static String auditCampo(AuditPackage audit, String qual) {
        if (audit == null) return "não disponível";
        String v = switch (qual) {
            case "doc" -> audit.getDocumentHash();
            case "texto" -> audit.getExtractedTextHash();
            default -> audit.getCalculationFingerprint();
        };
        return (v == null || v.isBlank()) ? ("fp".equals(qual) ? "não gerado" : "não disponível") : v;
    }
}
