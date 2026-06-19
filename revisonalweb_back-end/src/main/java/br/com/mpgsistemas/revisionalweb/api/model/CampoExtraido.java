package br.com.mpgsistemas.revisionalweb.api.model;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "table_campo_extraido")
public class CampoExtraido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_c_extraido")
    private Long id_c_extraido;

    @Column(nullable = false, length = 1000)
    private String nome;

    @Column(nullable = false, length = 1000)
    private Object valor;

    @Column(nullable = false, length = 1000)
    private Double confianca = 0.0;

    @Column(nullable = false, length = 1000)
    private String evidencia = "";

    @Column(nullable = false, length = 1000)
    private String origem = "manual";

}
