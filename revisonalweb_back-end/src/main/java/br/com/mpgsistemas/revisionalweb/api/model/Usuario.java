package br.com.mpgsistemas.revisionalweb.api.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Data
@Table(name = "table_usuario")
public class Usuario implements UserDetails {

    // Campo em Java DEVE se chamar "id": o Spring Data (4.x) deriva queries
    // exists/count selecionando a propriedade "id" — nome fora do padrão
    // (id_usuario) quebra existsByCpf/Email/Oab com PropertyReferenceException.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    // --- Identificadores de login (qualquer um: email, cpf ou oab) ---
    @Column(unique = true, length = 11)
    private String cpf;

    @Column(unique = true, length = 150)
    private String email;

    @Column(length = 100, nullable = false, unique = true)
    private String oab;

    @Column(length = 100, nullable = false)
    private String nomeCompleto;

    @Column(length = 100, nullable = false)
    private String senha;

    @Column(nullable = false)
    private boolean ativo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "usuario_role")
    private UsuarioRole usuarioRole;

    // MULTI-TENANCY: N Usuários pertencem a 1 Tenant (escritório). NÃO usa @TenantId
    // de propósito: o login é cross-tenant (acha o usuário por cpf/email/oab global),
    // e só então o tenant é resolvido a partir deste vínculo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // --- Segurança e auditoria (padrão sigapsi) ---
    @CreationTimestamp
    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_ultimo_login")
    private LocalDateTime dataUltimoLogin;

    @Column(name = "tentativas_falhas")
    private Integer tentativasFalhasLogin = 0;

    @Column(name = "conta_bloqueada_ate")
    private LocalDateTime contaBloqueadaAte;

    @Column(name = "forcar_troca_senha")
    private Boolean forcarTrocaSenha = false;

    // Sessão única: só vale o último token emitido
    @Column(name = "token_ativo", columnDefinition = "TEXT")
    private String tokenAtivo;

    @Column(name = "ultimo_ip", length = 45)
    private String ultimoIp;

    // MULTIPLICIDADE: 1 Usuário tem N Casos (One-to-Many)
    @OneToMany(mappedBy = "auditor", cascade = CascadeType.ALL)
    private List<CasoRevisional> casos;

    /** Id do tenant (escritório) do usuário, ou null se ainda não vinculado. */
    public Long getTenantId() {
        return tenant != null ? tenant.getId() : null;
    }

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.usuarioRole == UsuarioRole.ROLE_SUPER_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_AUDITOR"));
        } else if (this.usuarioRole == UsuarioRole.ROLE_ADMIN) {
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_AUDITOR"));
        } else if (this.usuarioRole == UsuarioRole.ROLE_VISUALIZADOR) {
            return List.of(new SimpleGrantedAuthority("ROLE_VISUALIZADOR"));
        } else {
            return List.of(new SimpleGrantedAuthority("ROLE_AUDITOR"));
        }
    }

    @Override
    public String getPassword() {
        return senha;
    }

    // Identificador canônico do principal (cpf). O login aceita email/cpf/oab via repository.
    @Override
    public String getUsername() {
        return cpf;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Não bloqueada se "contaBloqueadaAte" for nula ou já no passado
        if (contaBloqueadaAte == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(contaBloqueadaAte);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
