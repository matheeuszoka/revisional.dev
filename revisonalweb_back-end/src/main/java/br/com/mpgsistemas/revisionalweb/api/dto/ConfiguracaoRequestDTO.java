package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;

// Atualização dos parâmetros editáveis. Campos nulos não sobrescrevem.
// openRouterApiKey: em branco = mantém a chave atual; com valor = define nova (será cifrada).
// limparApiKey = true: remove a chave existente.
public record ConfiguracaoRequestDTO(
        String ocrIdioma,
        Integer ocrDpi,
        Integer ocrMaxPaginas,
        String ocrTessdataPath,
        String openRouterBaseUrl,
        String openRouterModel,
        Integer openRouterTimeoutSegundos,
        Integer openRouterMaxChars,
        String openRouterReferer,
        String openRouterTitulo,
        String openRouterApiKey,
        Boolean limparApiKey
) {
    // Monta um "patch" entidade com os campos não-sensíveis (a chave é tratada à parte).
    public ConfiguracaoSistema toPatch() {
        ConfiguracaoSistema c = new ConfiguracaoSistema();
        c.setOcrIdioma(ocrIdioma);
        c.setOcrDpi(ocrDpi);
        c.setOcrMaxPaginas(ocrMaxPaginas);
        c.setOcrTessdataPath(ocrTessdataPath);
        c.setOpenRouterBaseUrl(openRouterBaseUrl);
        c.setOpenRouterModel(openRouterModel);
        c.setOpenRouterTimeoutSegundos(openRouterTimeoutSegundos);
        c.setOpenRouterMaxChars(openRouterMaxChars);
        c.setOpenRouterReferer(openRouterReferer);
        c.setOpenRouterTitulo(openRouterTitulo);
        return c;
    }
}
