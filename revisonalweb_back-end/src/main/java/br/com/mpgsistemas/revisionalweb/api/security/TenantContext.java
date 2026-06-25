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

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CONTEXTO.set(tenantId);
    }

    public static Long get() {
        Long t = CONTEXTO.get();
        return t != null ? t : SEM_TENANT;
    }

    public static void clear() {
        CONTEXTO.remove();
    }
}
