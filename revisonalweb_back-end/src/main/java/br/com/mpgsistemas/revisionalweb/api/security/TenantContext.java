package br.com.mpgsistemas.revisionalweb.api.security;

/**
 * Guarda o tenant (escritório) do usuário autenticado durante a requisição.
 * Setado no {@link SecurityFilter} a partir do JWT e limpo no fim do request.
 * O {@link br.com.mpgsistemas.revisionalweb.api.config.TenantIdentifierResolver}
 * lê daqui para o Hibernate filtrar/gravar tenant_id automaticamente.
 */
public final class TenantContext {

    /** Sentinela "sem tenant": usado fora de requisição (boot/seeder). Não casa com dado real. */
    public static final Long SEM_TENANT = 0L;

    private static final ThreadLocal<Long> CONTEXTO = new ThreadLocal<>();

    // SUPER_ADMIN (plataforma): mantém o próprio tenant p/ INSERTs, mas o
    // TenantIdentifierResolver marca a sessão como "root" e o Hibernate deixa
    // de filtrar SELECT/UPDATE/DELETE — enxerga todos os tenants.
    private static final ThreadLocal<Boolean> SUPER_ADMIN = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CONTEXTO.set(tenantId);
    }

    public static Long get() {
        Long t = CONTEXTO.get();
        return t != null ? t : SEM_TENANT;
    }

    public static void setSuperAdmin(boolean superAdmin) {
        SUPER_ADMIN.set(superAdmin);
    }

    public static boolean isSuperAdmin() {
        return Boolean.TRUE.equals(SUPER_ADMIN.get());
    }

    public static void clear() {
        CONTEXTO.remove();
        SUPER_ADMIN.remove();
    }
}
