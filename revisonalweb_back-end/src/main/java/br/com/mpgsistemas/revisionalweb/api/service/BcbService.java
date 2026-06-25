package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.model.ParametrosSistema;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Integracao com a serie SGS 25471 do Banco Central (taxa media de juros -
 * aquisicao de veiculos PF). A API devolve pares data/valor; valor e % ao mes.
 *
 * Portado de revisional_web/bcb.py. Service isolado: nao conhece banco nem HTTP
 * do cliente; so consulta o BCB e devolve um DTO ReferenciaMercado.
 */
@Service
public class BcbService {

    private static final String SGS_25471_URL =
            "https://api.bcb.gov.br/dados/serie/bcdata.sgs.25471/dados/ultimos/12?formato=json";
    private static final String SGS_25471_DATASET =
            "https://dadosabertos.bcb.gov.br/dataset/25471-taxa-media-mensal-de-juros-das-operacoes-de-credito-com-recursos-livres---pessoas-fisicas---a";
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ParametrosSistema params;
    private final CalculadoraFinanceiraService calculadora;
    private final RestClient restClient;

    public BcbService(ParametrosSistema params, CalculadoraFinanceiraService calculadora) {
        this.params = params;
        this.calculadora = calculadora;
        Duration timeout = Duration.ofSeconds(params.getBcbTimeoutSegundos());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) timeout.toMillis());
        factory.setReadTimeout((int) timeout.toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public static class BcbException extends RuntimeException {
        public BcbException(String msg, Throwable cause) {
            super(msg, cause);
        }
        public BcbException(String msg) {
            super(msg);
        }
    }

    /** Consulta a ultima taxa mensal disponivel da serie SGS 25471. */
    @SuppressWarnings("unchecked")
    public ReferenciaMercado consultarTaxaVeiculoPf() {
        List<Map<String, String>> payload;
        try {
            payload = restClient.get()
                    .uri(SGS_25471_URL)
                    .retrieve()
                    .body(List.class);
        } catch (Exception exc) {
            throw new BcbException("Falha ao consultar BCB SGS 25471: " + exc.getMessage(), exc);
        }

        if (payload == null || payload.isEmpty()) {
            throw new BcbException("BCB SGS 25471 retornou payload vazio ou inesperado.");
        }

        LocalDate maisRecente = null;
        String dataRaw = null;
        Double mensalPct = null;
        for (Map<String, String> linha : payload) {
            try {
                Double valor = parseDecimalBr(linha.get("valor"));
                String dRaw = linha.get("data");
                if (valor == null || dRaw == null) continue;
                LocalDate dt = LocalDate.parse(dRaw, BR_DATE);
                if (maisRecente == null || dt.isAfter(maisRecente)) {
                    maisRecente = dt;
                    dataRaw = dRaw;
                    mensalPct = valor;
                }
            } catch (Exception ignored) {
                // linha malformada: ignora e segue
            }
        }
        if (mensalPct == null) {
            throw new BcbException("Nao foi possivel interpretar dados da serie SGS 25471.");
        }

        double anualPct = calculadora.mensalParaAnual(mensalPct / 100.0) * 100;
        ReferenciaMercado m = new ReferenciaMercado();
        m.setFonte("BCB SGS 25471 - taxa media mensal aquisicao de veiculos PF");
        m.setCodigoModalidade(params.getBcbCodigoModalidade());
        m.setDataReferencia(dataRaw);
        m.setTaxaMensalPct(mensalPct);
        m.setTaxaAnualPct(anualPct);
        m.setObservacao("Taxa media ponderada pelo valor das concessoes; parametro tecnico, nao teto legal automatico. Fonte: " + SGS_25471_DATASET);
        return m;
    }

    /** Parse de decimal no formato BR ("12,34" -> 12.34) ou US ("12.34"). */
    private Double parseDecimalBr(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = raw.trim().replace(".", "").replace(",", ".");
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
