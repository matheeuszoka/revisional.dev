package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import br.com.mpgsistemas.revisionalweb.api.model.ParametrosSistema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private final ParametrosSistema params;

    public ExtracaoIaService(ConfiguracaoService config, ParametrosSistema params) {
        this.config = config;
        this.params = params;
    }

    public boolean habilitado() {
        return config.iaHabilitada();
    }

    /** Notificado a cada janela de texto enviada à IA (janela atual, total). */
    @FunctionalInterface
    public interface ProgressoJanela {
        void janela(int atual, int total);
    }

    private static final ProgressoJanela SEM_PROGRESSO = (atual, total) -> {
    };

    public Map<String, CampoExtraido> extrairCampos(String texto) {
        return extrairCampos(texto, SEM_PROGRESSO);
    }

    /**
     * Pede ao modelo a estruturação dos campos. Contratos reais excedem o limite de
     * caracteres por chamada: o texto é fatiado em janelas sobrepostas e cada uma vai
     * ao modelo; campos repetidos entre janelas ficam com a maior confiança. Devolve
     * mapa nome->CampoExtraido (origem="ia"). Falha de uma janela não derruba as
     * demais; em falha total, devolve mapa vazio (não interrompe o upload).
     */
    public Map<String, CampoExtraido> extrairCampos(String texto, ProgressoJanela progresso) {
        String apiKey = config.getOpenRouterApiKey();
        if (apiKey == null || apiKey.isBlank() || texto == null || texto.isBlank()) {
            return Map.of();
        }
        ConfiguracaoSistema cfg = config.carregar();
        int maxChars = cfg.getOpenRouterMaxChars() != null ? cfg.getOpenRouterMaxChars() : 14000;
        List<String> janelas = fatiar(texto, maxChars,
                params.getOpenRouterSobreposicaoChars(), params.getOpenRouterMaxChunks());

        Map<String, CampoExtraido> out = new LinkedHashMap<>();
        for (int i = 0; i < janelas.size(); i++) {
            progresso.janela(i + 1, janelas.size());
            Map<String, CampoExtraido> parcial = chamarModelo(cfg, apiKey, janelas.get(i));
            parcial.forEach((nome, campo) -> out.merge(nome, campo, ExtracaoIaService::maiorConfianca));
        }
        return out;
    }

    /** Entre duas extrações do mesmo campo (janelas distintas), fica a de maior confiança. */
    private static CampoExtraido maiorConfianca(CampoExtraido atual, CampoExtraido novo) {
        double cAtual = atual.getConfianca() != null ? atual.getConfianca() : 0.0;
        double cNovo = novo.getConfianca() != null ? novo.getConfianca() : 0.0;
        return cNovo > cAtual ? novo : atual;
    }

    /**
     * Fatia o texto em janelas de até {@code tamanho} chars com {@code sobreposicao}
     * de contexto entre janelas consecutivas (evita cortar o quadro-resumo ao meio).
     * Limitado a {@code maxJanelas} para conter custo/tempo por documento.
     */
    static List<String> fatiar(String texto, int tamanho, int sobreposicao, int maxJanelas) {
        List<String> janelas = new ArrayList<>();
        if (texto.length() <= tamanho) {
            janelas.add(texto);
            return janelas;
        }
        int passo = Math.max(1, tamanho - Math.max(0, sobreposicao));
        for (int ini = 0; ini < texto.length() && janelas.size() < Math.max(1, maxJanelas); ini += passo) {
            int fim = Math.min(texto.length(), ini + tamanho);
            janelas.add(texto.substring(ini, fim));
            if (fim >= texto.length()) break;
        }
        return janelas;
    }

    /** Uma chamada ao modelo sobre um trecho do contrato. Falha vira mapa vazio. */
    private Map<String, CampoExtraido> chamarModelo(ConfiguracaoSistema cfg, String apiKey, String conteudo) {
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
