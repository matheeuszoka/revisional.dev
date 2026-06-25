package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tenant = escritório/empresa cliente do SaaS. Unidade de isolamento multi-tenant.
 * Vários {@link Usuario} (advogados/auditores) pertencem a um mesmo Tenant e
 * compartilham os {@link CasoRevisional}. O isolamento dos dados de negócio é
 * feito por discriminator (coluna tenant_id) via Hibernate @TenantId.
 */
@Entity
@Data
@Table(name = "table_tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tenant")
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 14, unique = true)
    private String cnpj;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;
}
