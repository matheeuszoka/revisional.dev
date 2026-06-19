package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "table_usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id_usuario;

    @Column(unique = true, length = 11)
    private String cpf;

    @Column(length = 100, nullable = false)
    private String nomeCompleto;

    @Column(length = 100, nullable = false)
    private String oab;

    @Column(length = 100, nullable = false)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    // MULTIPLICIDADE: 1 Usuário tem N Casos (One-to-Many)
    // mappedBy = "auditor" indica que a classe CasoRevisional é a dona da chave estrangeira
    @OneToMany(mappedBy = "auditor", cascade = CascadeType.ALL)
    private List<CasoRevisional> casos;
}
