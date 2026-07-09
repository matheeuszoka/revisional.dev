package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "table_evento_auditoria")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_auditoria")
    private Long id_evento_auditoria;

    // MULTI-TENANCY: discriminator (preenchido/filtrado pelo Hibernate).
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(length = 11)
    private String cpfUsuarioLogado;

    @Column(nullable = false)
    private String tipoEvento;

    @Column(columnDefinition = "text")
    private String payloadJson;

    @Column(updatable = false)
    private LocalDateTime criadoEm;

    // MULTIPLICIDADE: N Eventos pertencem a 1 Caso (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CasoRevisional caso;

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }
}
