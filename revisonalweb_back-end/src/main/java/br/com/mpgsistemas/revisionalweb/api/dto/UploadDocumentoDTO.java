package br.com.mpgsistemas.revisionalweb.api.dto;

import br.com.mpgsistemas.revisionalweb.api.model.UploadDocumento;

import java.time.LocalDateTime;

// Resumo de um documento anexado (sem expor a entidade/relacionamento do caso).
public record UploadDocumentoDTO(
        Long id,
        String nomeOriginal,
        String hashSha256,
        LocalDateTime criadoEm
) {
    public static UploadDocumentoDTO from(UploadDocumento u) {
        return new UploadDocumentoDTO(u.getId(), u.getNomeOriginal(), u.getHashSha256(), u.getCriadoEm());
    }
}
