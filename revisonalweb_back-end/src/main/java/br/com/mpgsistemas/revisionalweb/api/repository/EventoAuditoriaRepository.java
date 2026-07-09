package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
}
