package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

public class UploadDocumento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeOriginal;
    private String caminhoArmazenado;
    private String hashSha256;
    private LocalDateTime criadoEm;

    // MULTIPLICIDADE: N Uploads pertencem a 1 Caso (Many-to-One)
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CasoRevisional caso;

}
