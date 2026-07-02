package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Login flexível: aceita email, cpf ou oab no mesmo campo
    Usuario findByEmailOrCpfOrOab(String email, String cpf, String oab);

    Usuario findByCpf(String cpf);

    // Usado no SecurityFilter (fora do open-in-view): tenant já carregado para
    // checar tenant.ativo sem LazyInitializationException.
    @Query("select u from Usuario u join fetch u.tenant where u.cpf = :cpf")
    Usuario findByCpfComTenant(String cpf);

    // Membros do escritório (Usuario não tem @TenantId: filtro explícito por FK)
    List<Usuario> findByTenant_IdOrderByNomeCompletoAsc(Long tenantId);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByOab(String oab);
}
