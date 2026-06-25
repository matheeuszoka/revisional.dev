package br.com.mpgsistemas.revisionalweb.api.model;

import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "table_caso_revisional")
public class CasoRevisional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caso_revisional")
    private Long id_caso_revisional;

    private String titulo;

    // MULTI-TENANCY: discriminator. Hibernate preenche no insert e filtra todo
    // SELECT/UPDATE/DELETE por este valor automaticamente. Não setar na mão.
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(updatable = false)
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // MULTIPLICIDADE: N Casos pertencem a 1 Usuário (Many-to-One)
    // FK aponta para Usuario.cpf (chave de negócio única), não para o id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_cpf", referencedColumnName = "cpf", nullable = false)
    private Usuario auditor;

    // MULTIPLICIDADE: 1 Caso tem N Documentos anexados (One-to-Many)
    @OneToMany(mappedBy = "caso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadDocumento> documentos;

    // MULTIPLICIDADE: 1 Caso tem N Eventos de log/auditoria (One-to-Many)
    @OneToMany(mappedBy = "caso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventoAuditoria> eventos;

    // --- COMPOSIÇÕES JSONB (PostgreSQL/MySQL moderno) ---
    // Em vez de criar dezenas de tabelas para os cálculos, salvamos o objeto inteiro como JSON!

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private DadosContrato contrato;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ReferenciaMercado mercado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ResultadoCalculo resultado;

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}

