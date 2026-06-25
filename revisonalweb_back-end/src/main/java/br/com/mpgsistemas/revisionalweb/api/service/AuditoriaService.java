package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.config.MatrizNormativa;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditItem;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditPackage;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;
import br.com.mpgsistemas.revisionalweb.api.model.ParametrosSistema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Auditoria pericial determinística (port de audit.py). Constrói o {@link AuditPackage}
 * a partir de contrato + mercado + resultado: matriz de itens (DOC/CAD/FIN/MKT/CET/TAR/JUR/INC/EXT),
 * score 0-100, fingerprint do cálculo e hash do snapshot de entrada.
 *
 * NÃO conhece banco nem HTTP (regra de isolamento). Recebe DTOs, devolve DTO.
 * Sem números mágicos: limiares/pesos vêm de {@link ParametrosSistema}.
 */
@Service
public class AuditoriaService {

    private static final String STATUS_OK = "OK";
    private static final String STATUS_PENDENTE = "PENDENTE";
    private static final String STATUS_ATENCAO = "ATENÇÃO";

    private final ParametrosSistema params;
    private final MatrizNormativa normas;

    // ObjectMapper determinístico (chaves ordenadas) para fingerprint reproduzível.
    private final ObjectMapper fingerprintMapper = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    public AuditoriaService(ParametrosSistema params, MatrizNormativa normas) {
        this.params = params;
        this.normas = normas;
    }

    public AuditPackage build(DadosContrato contrato, ResultadoCalculo resultado, ReferenciaMercado mercado) {
        if (contrato == null) contrato = new DadosContrato();
        if (resultado == null) resultado = new ResultadoCalculo();
        if (mercado == null) mercado = new ReferenciaMercado();

        List<AuditItem> items = new ArrayList<>();

        add(items, "DOC-001", "Hash SHA-256 do contrato",
                temTexto(contrato.getHashContrato()),
                ou(contrato.getHashContrato(), "Documento ainda não anexado ou hash indisponível."),
                "ok", "medio");
        add(items, "DOC-002", "Hash do texto extraído/OCR",
                temTexto(contrato.getHashTextoExtraido()),
                ou(contrato.getHashTextoExtraido(), "Texto ainda não extraído."),
                "ok", "medio");
        add(items, "DOC-003", "Amostra de texto para conferência",
                temTexto(contrato.getAmostraTextoExtraido()),
                temTexto(contrato.getAmostraTextoExtraido())
                        ? amostra(contrato.getAmostraTextoExtraido()) : "Sem amostra textual.",
                "ok", "medio");

        String cpfLimpo = limparCpf(contrato.getClienteCpf());
        add(items, "CAD-001", "CPF do cliente validado",
                temTexto(cpfLimpo) ? validarCpf(cpfLimpo) : Boolean.FALSE,
                ou(contrato.getClienteCpf(), "CPF não informado."),
                "ok", "alto");
        add(items, "CAD-002", "Identificação da instituição",
                temTexto(contrato.getInstituicao()),
                ou(contrato.getInstituicao(), "Instituição não identificada."),
                "ok", "medio");
        add(items, "CAD-003", "Número de contrato/CCB",
                temTexto(contrato.getContratoNumero()),
                ou(contrato.getContratoNumero(), "Contrato/CCB não identificado."),
                "ok", "medio");

        Double valorPrincipal = primeiroPositivo(contrato.getValorFinanciado(), contrato.getValorLiquidoLiberado());
        add(items, "FIN-001", "Valor financiado/líquido",
                valorPrincipal != null && valorPrincipal > 0,
                valorPrincipal != null ? String.valueOf(valorPrincipal) : "Não informado",
                "ok", "alto");
        add(items, "FIN-002", "Prazo e parcela",
                contrato.getPrazoMeses() != null && contrato.getValorParcela() != null,
                "Prazo: " + txt(contrato.getPrazoMeses()) + "; Parcela: " + txt(contrato.getValorParcela()),
                "ok", "alto");
        add(items, "FIN-003", "Taxa contratual ou taxa apurada",
                resultado.getTaxaMensalContratoApuradaPct() != null,
                "Taxa mensal apurada: " + txt(resultado.getTaxaMensalContratoApuradaPct()),
                "ok", "alto");
        add(items, "MKT-001", "Taxa de referência registrada",
                resultado.getTaxaMensalMercadoPct() != null,
                "Fonte: " + txt(mercado.getFonte()) + "; Data: " + txt(mercado.getDataReferencia())
                        + "; Taxa mês: " + txt(resultado.getTaxaMensalMercadoPct()),
                "ok", "alto");
        add(items, "CET-001", "CET anual informado no contrato",
                contrato.getCetAnualPct() != null,
                "CET anual contratual: " + txt(contrato.getCetAnualPct()),
                "ok", "alto");
        add(items, "CET-002", "CET estimado pelo fluxo",
                resultado.getCetAnualApuradoPct() != null,
                "CET anual estimado: " + txt(resultado.getCetAnualApuradoPct()),
                "ok", "medio");

        Double[] tarifas = {contrato.getIof(), contrato.getTarifaCadastro(), contrato.getTarifaAvaliacaoBem(),
                contrato.getTarifaRegistroContrato(), contrato.getGravame(), contrato.getSeguro(),
                contrato.getOutrosEncargos()};
        int tarifasDetectadas = 0;
        for (Double t : tarifas) {
            if (t != null && t != 0.0) tarifasDetectadas++;
        }
        add(items, "TAR-001", "Encargos/tarifas destacados",
                tarifasDetectadas > 0,
                "Quantidade com valor: " + tarifasDetectadas
                        + ". Conferir autorização, contratação e prestação do serviço.",
                "info", "medio");

        Double spread = resultado.getSpreadPercentualSobreMercado();
        if (spread != null) {
            add(items, "JUR-001", "Spread sobre referência",
                    spread <= params.getAuditSpreadMaxPctSobreMercado(),
                    "Spread percentual sobre referência: " + fmt2(spread) + "%",
                    "info", "alto");
        } else {
            add(items, "JUR-001", "Spread sobre referência", null,
                    "Spread não calculado por ausência de taxa contratual ou referência.",
                    "ok", "alto");
        }

        List<String> inconsistencias = resultado.getInconsistencias() != null
                ? resultado.getInconsistencias() : List.of();
        if (!inconsistencias.isEmpty()) {
            int limite = Math.min(inconsistencias.size(), params.getAuditMaxInconsistencias());
            for (int i = 0; i < limite; i++) {
                items.add(new AuditItem(String.format("INC-%03d", i + 1), "Inconsistência técnica",
                        STATUS_ATENCAO, inconsistencias.get(i), "alto"));
            }
        } else {
            items.add(new AuditItem("INC-000", "Inconsistências técnicas", STATUS_OK,
                    "Nenhuma inconsistência crítica registrada pelo motor de cálculo.", "ok"));
        }

        List<Double> confiancas = new ArrayList<>();
        if (contrato.getCamposExtraidos() != null) {
            for (CampoExtraido c : contrato.getCamposExtraidos().values()) {
                if (c != null && "documento".equals(c.getOrigem())) {
                    confiancas.add(c.getConfianca() != null ? c.getConfianca() : 0.0);
                }
            }
        }
        double mediaConf = confiancas.isEmpty() ? 0.0
                : confiancas.stream().mapToDouble(Double::doubleValue).sum() / confiancas.size();
        add(items, "EXT-001", "Confiabilidade média da extração",
                mediaConf >= params.getAuditConfiancaMinimaExtracao(),
                "Campos extraídos: " + confiancas.size() + "; confiança média: " + fmt2(mediaConf)
                        + ". Operador deve conferir campos críticos.",
                "info", "medio");

        long falhas = items.stream().filter(i -> STATUS_PENDENTE.equals(i.status())).count();
        long avisos = items.stream().filter(i -> STATUS_ATENCAO.equals(i.status())).count();
        int score = (int) Math.max(0, Math.min(100,
                100 - falhas * params.getAuditPenalidadeFalha() - avisos * params.getAuditPenalidadeAviso()));

        AuditPackage pkg = new AuditPackage();
        pkg.setAuditId(UUID.randomUUID().toString());
        pkg.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        pkg.setSoftwareVersion(params.getSoftwareVersao());
        pkg.setDocumentHash(ou(contrato.getHashContrato(), ""));
        pkg.setExtractedTextHash(ou(contrato.getHashTextoExtraido(), ""));
        pkg.setScore(score);
        pkg.setItems(items);
        pkg.setSources(normas.fontes());
        pkg.setCalculationFingerprint(fingerprint(contrato, mercado, resultado));
        pkg.setInputSnapshotHash(inputSnapshotHash(contrato, mercado));
        pkg.setWarnings(new ArrayList<>(inconsistencias));
        return pkg;
    }

    // ----------------------------------------------------------------
    // Fingerprint determinístico do cálculo (espelha build_calculation_fingerprint)
    // ----------------------------------------------------------------
    private String fingerprint(DadosContrato contrato, ReferenciaMercado mercado, ResultadoCalculo resultado) {
        Map<String, Object> resultCore = new LinkedHashMap<>();
        resultCore.put("contrato_taxa_mensal_pct_apurada", resultado.getTaxaMensalContratoApuradaPct());
        resultCore.put("taxa_mercado_mensal_pct", resultado.getTaxaMensalMercadoPct());
        resultCore.put("parcela_recalculada_mercado", resultado.getParcelaRecalculadaMercado());
        resultCore.put("diferenca_total_nominal", resultado.getDiferencaTotalNominal());
        resultCore.put("classificacao_risco", resultado.getClassificacaoRisco());
        resultCore.put("memoria_calculo", resultado.getMemoriaCalculo());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contract", contrato);
        payload.put("market", mercado);
        payload.put("result_core", resultCore);
        return sha256(serializar(payload));
    }

    private String inputSnapshotHash(DadosContrato contrato, ReferenciaMercado mercado) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contract", contrato);
        payload.put("market", mercado);
        return sha256(serializar(payload));
    }

    private String serializar(Object o) {
        try {
            return fingerprintMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------
    private void add(List<AuditItem> items, String codigo, String item, Boolean ok,
                     String detalhe, String sevOk, String sevFail) {
        String status;
        String severidade;
        if (Boolean.TRUE.equals(ok)) {
            status = STATUS_OK;
            severidade = sevOk;
        } else if (Boolean.FALSE.equals(ok)) {
            status = STATUS_PENDENTE;
            severidade = sevFail;
        } else {
            status = STATUS_ATENCAO;
            severidade = "medio";
        }
        items.add(new AuditItem(codigo, item, status, detalhe, severidade));
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String ou(String valor, String alternativa) {
        return temTexto(valor) ? valor : alternativa;
    }

    private static String amostra(String s) {
        return (s.length() > 180 ? s.substring(0, 180) : s) + "...";
    }

    private static String txt(Object o) {
        return o == null ? "null" : String.valueOf(o);
    }

    private static String fmt2(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static Double primeiroPositivo(Double... valores) {
        for (Double v : valores) {
            if (v != null && v != 0.0) return v;
        }
        return null;
    }

    private static String limparCpf(String cpf) {
        return cpf == null ? "" : cpf.replaceAll("\\D", "");
    }

    /** Validação de CPF (dígitos verificadores). Espelha validate_cpf do protótipo. */
    private static boolean validarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return false;
        if (cpf.chars().distinct().count() == 1) return false; // todos iguais
        try {
            for (int j = 9; j <= 10; j++) {
                int soma = 0;
                for (int i = 0; i < j; i++) {
                    soma += (cpf.charAt(i) - '0') * ((j + 1) - i);
                }
                int dv = (soma * 10) % 11;
                if (dv == 10) dv = 0;
                if (dv != (cpf.charAt(j) - '0')) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256(String texto) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
