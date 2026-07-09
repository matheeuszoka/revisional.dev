package br.com.mpgsistemas.revisionalweb.api.dto;

/**
 * Item da matriz de auditoria (port de audit.py AuditItem).
 * status: "OK" | "PENDENTE" | "ATENÇÃO". severidade: "ok" | "info" | "medio" | "alto".
 */
public record AuditItem(
        String codigo,
        String item,
        String status,
        String detalhe,
        String severidade
) {
}
