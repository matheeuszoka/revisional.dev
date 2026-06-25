package br.com.mpgsistemas.revisionalweb.api.dto;

/**
 * Fonte normativa/oficial usada na auditoria e nos laudos (port de norms.py).
 * Imutável: matriz estática de leis/súmulas/resoluções.
 */
public record FonteNormativa(
        String tema,
        String fonte,
        String url,
        String usoNoSistema
) {
}
