package br.com.mpgsistemas.revisionalweb.api.config;

import br.com.mpgsistemas.revisionalweb.api.model.Tenant;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;
import br.com.mpgsistemas.revisionalweb.api.repository.TenantRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.tenant:MPG Sistemas}")
    private String adminTenantNome;

    // Credenciais do admin inicial (sobrescreva via .env em produção)
    @Value("${app.admin.cpf:00000000000}")
    private String adminCpf;

    @Value("${app.admin.email:suporte@mpgsistemas.com.br}")
    private String adminEmail;

    @Value("${app.admin.oab:ADMIN-0001}")
    private String adminOab;

    @Value("${app.admin.senha:@revisional2026*}")
    private String adminSenha;

    @Override
    public void run(String... args) {
        if (usuarioRepository.findByCpf(adminCpf) == null) {
            // Multi-tenancy: garante o tenant inicial (escritório padrão da plataforma).
            Tenant tenant = tenantRepository.findByNomeIgnoreCase(adminTenantNome);
            if (tenant == null) {
                tenant = new Tenant();
                tenant.setNome(adminTenantNome);
                tenant.setAtivo(true);
                tenant = tenantRepository.save(tenant);
            }

            Usuario admin = new Usuario();
            admin.setNomeCompleto("Suporte MPG");
            admin.setCpf(adminCpf);
            admin.setEmail(adminEmail);
            admin.setOab(adminOab);
            admin.setSenha(passwordEncoder.encode(adminSenha));
            admin.setUsuarioRole(UsuarioRole.ROLE_ADMIN);
            admin.setTenant(tenant);
            admin.setAtivo(true);

            usuarioRepository.save(admin);
            System.out.println("Usuário admin inicial criado (ROLE_ADMIN). Altere a senha padrão.");
        }
    }
}
