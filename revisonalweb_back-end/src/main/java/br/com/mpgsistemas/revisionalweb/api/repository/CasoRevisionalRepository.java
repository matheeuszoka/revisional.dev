package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CasoRevisionalRepository extends JpaRepository<CasoRevisional, Long> {

    // Busca ampliada: título (coluna) + clienteNome/instituicao (JSONB ->>) e filtro
    // opcional de status (comResultado: null=todos, true=laudo pronto, false=em análise).
    // Query NATIVA porque JPQL não navega em JSONB. ⚠️ Nativa NÃO passa pelo filtro
    // automático do @TenantId — o tenant_id vai explícito na cláusula.
    // Escopo por papel (resolvido no Service): tenantId null = todos os escritórios
    // (SUPER_ADMIN); cpf null = todos os auditores do escritório (ADMIN).
    String FILTRO_CASOS = """
            from table_caso_revisional c
            where (cast(:tenantId as bigint) is null or c.tenant_id = :tenantId)
              and (cast(:cpf as text) is null or c.owner_cpf = :cpf)
              and (cast(:q as text) is null
                   or c.titulo ilike '%' || cast(:q as text) || '%'
                   or (c.contrato ->> 'clienteNome') ilike '%' || cast(:q as text) || '%'
                   or (c.contrato ->> 'instituicao') ilike '%' || cast(:q as text) || '%')
              and (cast(:comResultado as boolean) is null
                   or (c.resultado is not null) = cast(:comResultado as boolean))
            """;

    @Query(value = "select c.* " + FILTRO_CASOS, countQuery = "select count(*) " + FILTRO_CASOS, nativeQuery = true)
    Page<CasoRevisional> buscarPagina(Long tenantId, String cpf, String q, Boolean comResultado, Pageable pageable);

    /** Projeção dos números do dashboard (aliases citados preservam camelCase). */
    interface EstatisticasCasos {
        long getTotal();
        long getLaudoPronto();
        long getIndicioForte();
        long getIndicioModerado();
    }

    // Números do dashboard. Nativa (lê classificação dentro do JSONB resultado) —
    // tenant/owner explícitos, mesmo padrão do FILTRO_CASOS acima.
    @Query(value = """
            select count(*)             as "total",
                   count(c.resultado)   as "laudoPronto",
                   count(*) filter (where c.resultado ->> 'classificacaoRisco' ilike 'Indicio tecnico forte%')    as "indicioForte",
                   count(*) filter (where c.resultado ->> 'classificacaoRisco' ilike 'Indicio tecnico moderado%') as "indicioModerado"
            from table_caso_revisional c
            where (cast(:tenantId as bigint) is null or c.tenant_id = :tenantId)
              and (cast(:cpf as text) is null or c.owner_cpf = :cpf)
            """, nativeQuery = true)
    EstatisticasCasos estatisticas(Long tenantId, String cpf);
}
