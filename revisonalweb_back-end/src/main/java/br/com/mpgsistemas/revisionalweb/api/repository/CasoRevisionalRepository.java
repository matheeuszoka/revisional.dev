package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CasoRevisionalRepository extends JpaRepository<CasoRevisional, Long> {

    // Casos do auditor logado (FK owner_cpf -> usuario.cpf)
    List<CasoRevisional> findByAuditor_CpfOrderByAtualizadoEmDesc(String cpf);
}
