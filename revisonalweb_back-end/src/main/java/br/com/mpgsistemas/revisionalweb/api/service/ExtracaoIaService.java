package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estruturação dos campos do contrato a partir do texto extraído, via LLM no
 * OpenRouter (API compatível com OpenAI chat/completions). Parâmetros e chave vêm
 * de ConfiguracaoService (editáveis na tela de admin; chave guardada cifrada).
 *
 * Sem chave configurada, devolve mapa vazio e o fluxo segue só com OCR. Nunca
 * lança para o caller — falha de IA não pode quebrar o upload.
 */
@Service
public class ExtracaoIaService {

    private static final Logger log = LoggerFactory.getLogger(ExtracaoIaService.class);

    private static final String LISTA_CAMPOS = String.join(", ",
            "clienteNome", "clienteCpf", "instituicao", "instituicaoCnpj", "contratoNumero",
            "modalidade", "dataContrato", "veiculoDescricao", "valorVeiculo", "valorEntrada",
            "valorFinanciado", "valorLiquidoLiberado", "prazoMeses", "valorParcela",
            "taxaJurosMensalPct", "taxaJurosAnualPct", "cetMensalPct", "cetAnualPct",
            "iof", "tarifaCadastro", "tarifaAvaliacaoBem", "tarifaRegistroContrato",
            "gravame", "seguro", "outrosEncargos", "descricaoOutrosEncargos");

    private static final String SISTEMA = """
            Você é um perito em contratos bancários de financiamento. Extraia os campos
            do contrato a partir do texto fornecido (pode conter ruído de OCR).
            Responda APENAS um JSON válido, sem markdown, no formato:
            {"campos": {"<nome>": {"valor": "<texto>", "confianca": <0..1>, "evidencia": "<trecho>"}}}
            Regras:
            - Inclua somente os campos efetivamente encontrados. Não invente valores.
            - Valores monetários e percentuais: use ponto decimal e sem símbolos (ex.: 45000.00, 1.99).
            - Datas no formato dd/MM/yyyy. prazoMeses como inteiro.
            - confianca reflete a certeza da extração. evidencia é o trecho-base (máx. 200 chars).
            Campos possíveis: """ + LISTA_CAMPOS + ".";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfiguracaoService config;

    public ExtracaoIaService(ConfiguracaoService config) {
        this.config = config;
    }

    public boolean habilitado() {
        return config.iaHabilitada();
    }

    /**
     * Pede ao modelo a estruturação dos campos. Devolve mapa nome->CampoExtraido
     * (origem="ia"). Em qualquer falha, devolve mapa vazio (não interrompe o upload).
     */
    public Map<String, CampoExtraido> extrairCampos(String texto) {
        String apiKey = config.getOpenRouterApiKey();
        if (apiKey == null || apiKey.isBlank() || texto == null || texto.isBlank()) {
            return Map.of();
        }
        ConfiguracaoSistema cfg = config.carregar();
        int maxChars = cfg.getOpenRouterMaxChars() != null ? cfg.getOpenRouterMaxChars() : 14000;
        String conteudo = texto.length() > maxChars ? texto.substring(0, maxChars) : texto;
        try {
            Map<String, Object> body = Map.of(
                    "model", cfg.getOpenRouterModel(),
                    "temperature", 0,
                    "response_format", Map.of("type", "json_object"),
                    "messages", java.util.List.of(
                            Map.of("role", "system", "content", SISTEMA),
                            Map.of("role", "user", "content", "Texto do contrato:\n\n" + conteudo)));

            RestClient.RequestBodySpec req = restClient(cfg).post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON);
            if (naoVazio(cfg.getOpenRouterReferer())) req.header("HTTP-Referer", cfg.getOpenRouterReferer());
            if (naoVazio(cfg.getOpenRouterTitulo())) req.header("X-Title", cfg.getOpenRouterTitulo());

            String resposta = req.body(body).retrieve().body(String.class);
            return parsear(resposta);
        } catch (Exception e) {
            // LGPD: não logar conteúdo do contrato; só a causa técnica.
            log.warn("Extração por IA indisponível: {}", e.getMessage());
            return Map.of();
        }
    }

    private RestClient restClient(ConfiguracaoSistema cfg) {
        int timeout = cfg.getOpenRouterTimeoutSegundos() != null ? cfg.getOpenRouterTimeoutSegundos() : 45;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeout));
        factory.setReadTimeout(Duration.ofSeconds(timeout));
        return RestClient.builder().baseUrl(cfg.getOpenRouterBaseUrl()).requestFactory(factory).build();
    }

    private static boolean naoVazio(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Sanitiza a saída do modelo: remove cercas markdown (```json) e qualquer texto
     * antes/depois, mantendo só o objeto JSON (do primeiro '{' ao último '}').
     */
    static String limparJson(String texto) {
        if (texto == null) return "{}";
        String s = texto.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("(?s)^```[a-zA-Z]*\\s*", "").replaceFirst("(?s)\\s*```\\s*$", "").trim();
        }
        int ini = s.indexOf('{');
        int fim = s.lastIndexOf('}');
        if (ini >= 0 && fim > ini) {
            s = s.substring(ini, fim + 1);
        }
        return s.isBlank() ? "{}" : s;
    }

    private Map<String, CampoExtraido> parsear(String resposta) throws Exception {
        Map<String, CampoExtraido> out = new LinkedHashMap<>();
        if (resposta == null || resposta.isBlank()) return out;

        JsonNode root = mapper.readTree(resposta);
        JsonNode conteudo = root.path("choices").path(0).path("message").path("content");
        // O modelo às vezes embrulha em ```json ... ``` ou adiciona texto: limpa antes de parsear.
        JsonNode dados = conteudo.isMissingNode()
                ? root
                : mapper.readTree(limparJson(conteudo.asText()));

        JsonNode campos = dados.path("campos");
        if (!campos.isObject()) return out;

        campos.fields().forEachRemaining(entry -> {
            String nome = entry.getKey();
            JsonNode v = entry.getValue();
            String valor = v.path("valor").asText("").strip();
            if (valor.isEmpty()) return;
            CampoExtraido c = new CampoExtraido();
            c.setNome(nome);
            c.setValor(valor);
            c.setConfianca(v.path("confianca").asDouble(0.5));
            c.setEvidencia(v.path("evidencia").asText("").strip());
            c.setOrigem("ia");
            out.put(nome, c);
        });
        return out;
    }
}
