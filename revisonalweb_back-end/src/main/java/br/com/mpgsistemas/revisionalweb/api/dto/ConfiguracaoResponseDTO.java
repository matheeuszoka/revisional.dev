package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;

import java.time.LocalDateTime;

// Visão segura da configuração: NUNCA expõe a chave da IA em claro. Só informa se
// existe uma chave definida (openRouterApiKeyDefinida).
public record ConfiguracaoResponseDTO(
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
        boolean openRouterApiKeyDefinida,
        LocalDateTime atualizadoEm
) {
    public static ConfiguracaoResponseDTO from(ConfiguracaoSistema c) {
        boolean temChave = c.getOpenRouterApiKeyEnc() != null && !c.getOpenRouterApiKeyEnc().isBlank();
        return new ConfiguracaoResponseDTO(
                c.getOcrIdioma(), c.getOcrDpi(), c.getOcrMaxPaginas(), c.getOcrTessdataPath(),
                c.getOpenRouterBaseUrl(), c.getOpenRouterModel(), c.getOpenRouterTimeoutSegundos(),
                c.getOpenRouterMaxChars(), c.getOpenRouterReferer(), c.getOpenRouterTitulo(),
                temChave, c.getAtualizadoEm());
    }
}
