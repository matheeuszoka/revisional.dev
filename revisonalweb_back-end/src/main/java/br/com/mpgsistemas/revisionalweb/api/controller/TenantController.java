package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.UsuarioResponseDTO;
import br.com.mpgsistemas.revisionalweb.api.model.Tenant;
import br.com.mpgsistemas.revisionalweb.api.repository.TenantRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public TenantController(TenantRepository tenantRepository, UsuarioRepository usuarioRepository) {
        this.tenantRepository = tenantRepository;
        this.usuarioRepository = usuarioRepository;
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

    /** Ativa/desativa um escritório inteiro (bloqueia o acesso dos membros). */
    @PatchMapping("/{id}/ativo")
    public ResponseEntity<?> alterarAtivo(@PathVariable Long id, @RequestParam boolean ativo) {
        Tenant tenant = tenantRepository.findById(id).orElse(null);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        tenant.setAtivo(ativo);
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }
}
