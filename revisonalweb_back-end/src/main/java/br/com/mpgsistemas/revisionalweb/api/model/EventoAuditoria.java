package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "table_evento_auditoria")
public class EventoAuditoria {


    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_evento_auditoria;

    private String cpfUsuarioLogado;
    private String tipoEvento;
    private String payloadJson;
    private LocalDateTime criadoEm;

    // MULTIPLICIDADE: N Eventos pertencem a 1 Caso (Many-to-One)
    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private CasoRevisional caso;

}
