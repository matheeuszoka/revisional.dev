package br.com.mpgsistemas.revisionalweb.api.config;

import br.com.mpgsistemas.revisionalweb.api.security.TenantContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Informa ao Hibernate qual tenant está ativo na thread atual. Para entidades
 * anotadas com @TenantId (CasoRevisional, UploadDocumento, EventoAuditoria) o
 * Hibernate adiciona "tenant_id = ?" em todo SELECT/UPDATE/DELETE e grava o
 * tenant corrente nos INSERTs — sem precisar tocar nas queries/repositories.
 */
@Component
public class TenantIdentifierResolver
        implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return TenantContext.get();
    }

    // Sessão "root" (SUPER_ADMIN): o Hibernate NÃO aplica o filtro tenant_id nos
    // SELECTs — visão cross-tenant da plataforma. INSERTs continuam gravando o
    // tenant corrente (o próprio tenant do super-admin).
    @Override
    public boolean isRoot(Long tenantId) {
        return TenantContext.isSuperAdmin();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
