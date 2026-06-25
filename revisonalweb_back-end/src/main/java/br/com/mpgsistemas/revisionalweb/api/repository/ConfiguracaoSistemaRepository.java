package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, Long> {
}
