package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "table_upload_documento")
public class UploadDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_upload_documento")
    private Long id;

    @Column(nullable = false)
    private String nomeOriginal;

    // Caminho/objeto no MinIO (bucket revisional-docs)
    @Column(nullable = false)
    private String caminhoArmazenado;

    @Column(length = 64)
    private String hashSha256;

    @Column(updatable = false)
    private LocalDateTime criadoEm;

    // MULTIPLICIDADE: N Uploads pertencem a 1 Caso (Many-to-One)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private CasoRevisional caso;

    @PrePersist
    void aoCriar() {
        this.criadoEm = LocalDateTime.now();
    }
}
