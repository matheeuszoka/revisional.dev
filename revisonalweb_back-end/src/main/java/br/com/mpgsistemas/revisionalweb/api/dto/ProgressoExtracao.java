package br.com.mpgsistemas.revisionalweb.api.dto;

/**
 * Estado corrente do processamento de upload (OCR + IA) de um caso, consultado
 * por polling pelo front (GET /api/casos/{id}/upload/progresso).
 *
 * @param etapa      código da fase (ENVIO, ARMAZENANDO, EXTRAINDO, OCR, IA, REGEX, SALVANDO, CONCLUIDO, ERRO)
 * @param mensagem   texto amigável exibido no painel do front
 * @param percentual 0..100 (estimado por fase; OCR avança por página)
 * @param terminado  true quando o processamento acabou (sucesso ou erro)
 * @param erro       mensagem de erro quando etapa = ERRO (null caso contrário)
 */
public record ProgressoExtracao(String etapa, String mensagem, Integer percentual, boolean terminado, String erro) {

    public static ProgressoExtracao ocioso() {
        return new ProgressoExtracao("OCIOSO", "Nenhum processamento em andamento.", null, true, null);
    }
}
