package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {

    // Busca explícita pelo tenant: funciona também na sessão root do SUPER_ADMIN
    // (em que o Hibernate não aplica o filtro automático de tenant).
    Optional<ConfiguracaoSistema> findByTenantId(Long tenantId);
}
