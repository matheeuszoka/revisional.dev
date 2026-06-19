package br.com.mpgsistemas.revisionalweb.api.model;

import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "table_caso_revisional")
public class CasoRevisional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_caso_revisional;

    private String titulo;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    // MULTIPLICIDADE: N Casos pertencem a 1 Usuário (Many-to-One)
    @ManyToOne
    @JoinColumn(name = "owner_cpf", nullable = false)
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


}

