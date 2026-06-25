package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CasoRevisionalRepository extends JpaRepository<CasoRevisional, Long> {

    // Casos do auditor logado (FK owner_cpf -> usuario.cpf), paginados/ordenados pelo back.
    Page<CasoRevisional> findByAuditor_Cpf(String cpf, Pageable pageable);

    // Busca server-side por título (coluna real; cliente/instituição vivem em JSONB).
    Page<CasoRevisional> findByAuditor_CpfAndTituloContainingIgnoreCase(String cpf, String titulo, Pageable pageable);
}
