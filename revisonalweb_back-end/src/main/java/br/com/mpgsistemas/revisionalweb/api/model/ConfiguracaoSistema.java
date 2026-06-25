package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Parâmetros operacionais editáveis pelo admin (OCR + IA). Linha única na tabela
 * (id=1). Segredos (chave da IA) são gravados CIFRADOS em open_router_api_key_enc.
 * Não inclui constantes periciais (tolerâncias/iterações) — essas ficam fixas em
 * ParametrosSistema para garantir determinismo do laudo.
 */
@Data
@Entity
@Table(name = "table_configuracao_sistema")
public class ConfiguracaoSistema {

    @Id
    @Column(name = "id_configuracao_sistema")
    private Long id;

    // --- OCR ---
    private String ocrIdioma;
    private Integer ocrDpi;
    private Integer ocrMaxPaginas;
    private String ocrTessdataPath;

    // --- OpenRouter (IA) ---
    private String openRouterBaseUrl;
    private String openRouterModel;
    @Column(columnDefinition = "text")
    private String openRouterApiKeyEnc; // sempre cifrado (enc:v1:...)
    private Integer openRouterTimeoutSegundos;
    private Integer openRouterMaxChars;
    private String openRouterReferer;
    private String openRouterTitulo;

    private LocalDateTime atualizadoEm;

    @PreUpdate
    @PrePersist
    void aoSalvar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}
