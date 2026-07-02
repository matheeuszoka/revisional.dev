package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import br.com.mpgsistemas.revisionalweb.api.model.ParametrosSistema;
import br.com.mpgsistemas.revisionalweb.api.repository.ConfiguracaoSistemaRepository;
import br.com.mpgsistemas.revisionalweb.api.security.TenantContext;
import org.springframework.stereotype.Service;

/**
 * Fonte única dos parâmetros operacionais editáveis (OCR + IA). Uma linha POR
 * TENANT: cada escritório tem sua própria configuração (inclusive chave OpenRouter),
 * semeada dos defaults de ambiente no primeiro acesso. A chave da IA fica cifrada;
 * só este serviço a decifra para consumo interno (nunca trafega em claro pela API).
 */
@Service
public class ConfiguracaoService {

    private final ConfiguracaoSistemaRepository repository;
    private final ParametrosSistema defaults;
    private final CriptografiaService cripto;

    public ConfiguracaoService(ConfiguracaoSistemaRepository repository,
                               ParametrosSistema defaults,
                               CriptografiaService cripto) {
        this.repository = repository;
        this.defaults = defaults;
        this.cripto = cripto;
    }

    /** Carrega a configuração do tenant corrente; cria/semeia do ambiente se ainda vazia. */
    public ConfiguracaoSistema carregar() {
        Long tenantId = TenantContext.get();
        // Fora de requisição autenticada não há tenant — nada a carregar/semear.
        if (TenantContext.SEM_TENANT.equals(tenantId)) {
            throw new IllegalStateException("Configuração exige um tenant ativo na requisição.");
        }
        // Busca explícita por tenant (não depende do filtro automático: na sessão
        // root do SUPER_ADMIN o Hibernate não filtra, e o findAll veria todos).
        ConfiguracaoSistema c = repository.findByTenantId(tenantId)
                .orElseGet(ConfiguracaoSistema::new); // tenant_id preenchido pelo Hibernate no INSERT
        boolean alterou = semearSeVazio(c);
        if (alterou || c.getId() == null) c = repository.save(c);
        return c;
    }

    private boolean semearSeVazio(ConfiguracaoSistema c) {
        boolean mudou = false;
        if (c.getOcrIdioma() == null) { c.setOcrIdioma(defaults.getOcrIdioma()); mudou = true; }
        if (c.getOcrDpi() == null) { c.setOcrDpi(defaults.getOcrDpi()); mudou = true; }
        if (c.getOcrMaxPaginas() == null) { c.setOcrMaxPaginas(defaults.getOcrMaxPaginas()); mudou = true; }
        if (c.getOcrTessdataPath() == null) { c.setOcrTessdataPath(defaults.getOcrTessdataPath()); mudou = true; }
        if (c.getOpenRouterBaseUrl() == null) { c.setOpenRouterBaseUrl(defaults.getOpenRouterBaseUrl()); mudou = true; }
        if (c.getOpenRouterModel() == null) { c.setOpenRouterModel(defaults.getOpenRouterModel()); mudou = true; }
        if (c.getOpenRouterTimeoutSegundos() == null) { c.setOpenRouterTimeoutSegundos(defaults.getOpenRouterTimeoutSegundos()); mudou = true; }
        if (c.getOpenRouterMaxChars() == null) { c.setOpenRouterMaxChars(defaults.getOpenRouterMaxChars()); mudou = true; }
        if (c.getOpenRouterReferer() == null) { c.setOpenRouterReferer(defaults.getOpenRouterReferer()); mudou = true; }
        if (c.getOpenRouterTitulo() == null) { c.setOpenRouterTitulo(defaults.getOpenRouterTitulo()); mudou = true; }
        // Semeia a chave do ambiente (cifrada) só se ainda não houver nenhuma definida.
        if (c.getOpenRouterApiKeyEnc() == null && defaults.getOpenRouterApiKey() != null
                && !defaults.getOpenRouterApiKey().isBlank()) {
            c.setOpenRouterApiKeyEnc(cripto.cifrar(defaults.getOpenRouterApiKey()));
            mudou = true;
        }
        return mudou;
    }

    /** Chave da IA em claro (decifrada) para uso interno do ExtracaoIaService. */
    public String getOpenRouterApiKey() {
        return cripto.decifrar(carregar().getOpenRouterApiKeyEnc());
    }

    public boolean iaHabilitada() {
        String k = getOpenRouterApiKey();
        return k != null && !k.isBlank();
    }

    /**
     * Atualiza os campos editáveis. apiKeyClara: null/blank = mantém a atual;
     * limparApiKey = remove a chave. Demais nulos são ignorados (não sobrescrevem).
     */
    public ConfiguracaoSistema atualizar(ConfiguracaoSistema patch, String apiKeyClara, boolean limparApiKey) {
        ConfiguracaoSistema c = carregar();
        if (patch.getOcrIdioma() != null) c.setOcrIdioma(patch.getOcrIdioma());
        if (patch.getOcrDpi() != null) c.setOcrDpi(patch.getOcrDpi());
        if (patch.getOcrMaxPaginas() != null) c.setOcrMaxPaginas(patch.getOcrMaxPaginas());
        if (patch.getOcrTessdataPath() != null) c.setOcrTessdataPath(patch.getOcrTessdataPath());
        if (patch.getOpenRouterBaseUrl() != null) c.setOpenRouterBaseUrl(patch.getOpenRouterBaseUrl());
        if (patch.getOpenRouterModel() != null) c.setOpenRouterModel(patch.getOpenRouterModel());
        if (patch.getOpenRouterTimeoutSegundos() != null) c.setOpenRouterTimeoutSegundos(patch.getOpenRouterTimeoutSegundos());
        if (patch.getOpenRouterMaxChars() != null) c.setOpenRouterMaxChars(patch.getOpenRouterMaxChars());
        if (patch.getOpenRouterReferer() != null) c.setOpenRouterReferer(patch.getOpenRouterReferer());
        if (patch.getOpenRouterTitulo() != null) c.setOpenRouterTitulo(patch.getOpenRouterTitulo());

        if (limparApiKey) {
            c.setOpenRouterApiKeyEnc(null);
        } else if (apiKeyClara != null && !apiKeyClara.isBlank()) {
            c.setOpenRouterApiKeyEnc(cripto.cifrar(apiKeyClara.trim()));
        }
        return repository.save(c);
    }
}
