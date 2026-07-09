package br.com.mpgsistemas.revisionalweb.api.dto;

// Conteúdo bruto de um documento para download/visualização inline.
public record DocumentoArquivo(byte[] conteudo, String contentType, String nomeOriginal) {
}
