package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

/**
 * Parâmetros operacionais editáveis pelo admin (OCR + IA). Uma linha POR TENANT
 * (@TenantId): cada escritório tem sua própria chave OpenRouter — isola custo e
 * billing de IA. Segredos são gravados CIFRADOS em open_router_api_key_enc.
 * Não inclui constantes periciais (tolerâncias/iterações) — essas ficam fixas em
 * ParametrosSistema para garantir determinismo do laudo.
 */
@Data
@Entity
@Table(name = "table_configuracao_sistema")
public class ConfiguracaoSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracao_sistema")
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

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
