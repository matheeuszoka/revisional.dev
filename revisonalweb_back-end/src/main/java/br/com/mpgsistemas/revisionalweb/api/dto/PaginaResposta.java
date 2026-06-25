package br.com.mpgsistemas.revisionalweb.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação estável para o front (evita serializar PageImpl direto,
 * cuja forma JSON é instável entre versões do Spring Data).
 */
public record PaginaResposta<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PaginaResposta<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaResposta<>(
                page.map(mapper).getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
