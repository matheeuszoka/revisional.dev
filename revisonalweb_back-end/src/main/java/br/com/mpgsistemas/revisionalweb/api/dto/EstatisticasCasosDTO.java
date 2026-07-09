package br.com.mpgsistemas.revisionalweb.api.dto;

/** Números do dashboard: casos do auditor no tenant corrente. */
public record EstatisticasCasosDTO(long total, long laudoPronto, long emAnalise,
                                   long indicioForte, long indicioModerado) {
}
