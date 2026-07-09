package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.EditarEscritorioDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.NovoEscritorioDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.UsuarioResponseDTO;
import br.com.mpgsistemas.revisionalweb.api.model.Tenant;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;
import br.com.mpgsistemas.revisionalweb.api.repository.TenantRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administração da plataforma (MPG): visão de todos os escritórios/tenants.
 * Exclusivo de SUPER_ADMIN. Tenant e Usuario não têm @TenantId, então as
 * consultas aqui já são naturalmente cross-tenant.
 */
@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class TenantController {

    private final TenantRepository tenantRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantController(TenantRepository tenantRepository, UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Tenant> listar() {
        return tenantRepository.findAll(Sort.by("nome"));
    }

    @GetMapping("/{id}/usuarios")
    public ResponseEntity<?> usuariosDoTenant(@PathVariable Long id) {
        if (!tenantRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(usuarioRepository.findByTenant_IdOrderByNomeCompletoAsc(id)
                .stream().map(UsuarioResponseDTO::from).toList());
    }

    /**
     * Cria um escritório (tenant) + admin inicial do cliente com senha temporária
     * (troca obrigatória no 1º login, mesmo fluxo do convite de membros).
     * Substitui o workaround de usar o register público em nome do cliente.
     */
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody NovoEscritorioDTO data) {
        if (data.nome() == null || data.nome().isBlank()) {
            return ResponseEntity.badRequest().body("Informe o nome do escritório.");
        }
        if (data.adminNome() == null || data.adminNome().isBlank()
                || data.adminOab() == null || data.adminOab().isBlank()
                || data.senhaTemporaria() == null || data.senhaTemporaria().isBlank()) {
            return ResponseEntity.badRequest().body("Informe nome, OAB e senha temporária do administrador.");
        }
        String nome = data.nome().trim();
        if (tenantRepository.findByNomeIgnoreCase(nome) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Escritório já cadastrado.");
        }
        String cnpj = normalizarCnpj(data.cnpj());
        if (!cnpj.isBlank() && tenantRepository.existsByCnpj(cnpj)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("CNPJ já cadastrado.");
        }
        if (data.adminCpf() != null && !data.adminCpf().isBlank() && usuarioRepository.existsByCpf(data.adminCpf())) {
            return ResponseEntity.badRequest().body("CPF do administrador já cadastrado.");
        }
        if (data.adminEmail() != null && !data.adminEmail().isBlank() && usuarioRepository.existsByEmail(data.adminEmail())) {
            return ResponseEntity.badRequest().body("E-mail do administrador já cadastrado.");
        }
        if (usuarioRepository.existsByOab(data.adminOab())) {
            return ResponseEntity.badRequest().body("OAB do administrador já cadastrada.");
        }

        Tenant tenant = new Tenant();
        tenant.setNome(nome);
        if (!cnpj.isBlank()) {
            tenant.setCnpj(cnpj);
        }
        tenant.setAtivo(true);
        tenant = tenantRepository.save(tenant);

        Usuario admin = new Usuario();
        admin.setNomeCompleto(data.adminNome().trim());
        admin.setEmail(data.adminEmail());
        admin.setCpf(data.adminCpf());
        admin.setOab(data.adminOab().trim());
        admin.setSenha(passwordEncoder.encode(data.senhaTemporaria()));
        admin.setUsuarioRole(UsuarioRole.ROLE_ADMIN);
        admin.setTenant(tenant);
        admin.setAtivo(true);
        admin.setForcarTrocaSenha(true);
        usuarioRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
    }

    /** Altera nome/CNPJ de um escritório existente. */
    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody EditarEscritorioDTO data) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        if (data.nome() == null || data.nome().isBlank()) {
            return ResponseEntity.badRequest().body("Informe o nome do escritório.");
        }
        String nome = data.nome().trim();
        Tenant mesmoNome = tenantRepository.findByNomeIgnoreCase(nome);
        if (mesmoNome != null && !mesmoNome.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe escritório com este nome.");
        }
        String cnpj = normalizarCnpj(data.cnpj());
        if (!cnpj.isBlank() && !cnpj.equals(tenant.getCnpj()) && tenantRepository.existsByCnpj(cnpj)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("CNPJ já cadastrado em outro escritório.");
        }
        tenant.setNome(nome);
        tenant.setCnpj(cnpj.isBlank() ? null : cnpj);
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    /**
     * Normaliza CNPJ para armazenamento: mantém [0-9A-Z] (suporta o padrão
     * numérico clássico e o novo alfanumérico da Receita Federal).
     */
    private static String normalizarCnpj(String v) {
        return v == null ? "" : v.toUpperCase().replaceAll("[^0-9A-Z]", "");
    }

    /** Ativa/desativa um escritório inteiro (bloqueia o acesso dos membros). */
    @PatchMapping("/{id}/ativo")
    public ResponseEntity<?> alterarAtivo(@PathVariable Long id,
                                          @RequestParam boolean ativo,
                                          @AuthenticationPrincipal Usuario superAdmin) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        // Trava de segurança: desativar o próprio escritório trancaria a plataforma
        if (!ativo && id.equals(superAdmin.getTenantId())) {
            return ResponseEntity.badRequest().body("Não é possível desativar o escritório da plataforma.");
        }
        tenant.setAtivo(ativo);
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }
}
