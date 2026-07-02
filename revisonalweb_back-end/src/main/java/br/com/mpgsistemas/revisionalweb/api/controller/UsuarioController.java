package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.AlterarSenhaDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ConviteUsuarioDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.UsuarioResponseDTO;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;
import br.com.mpgsistemas.revisionalweb.api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestão de membros do escritório (tenant). O tenant NUNCA vem do payload:
 * é sempre o do admin autenticado (JWT) — fecha o furo do register público
 * anexar usuários a escritórios alheios pelo nome.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lista os membros do escritório do admin autenticado. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UsuarioResponseDTO> listar(@AuthenticationPrincipal Usuario admin) {
        return repository.findByTenant_IdOrderByNomeCompletoAsc(admin.getTenantId())
                .stream().map(UsuarioResponseDTO::from).toList();
    }

    /**
     * Convida (cria) um membro no escritório do admin, com senha temporária:
     * o convidado é obrigado a trocá-la no primeiro login (forcar_troca_senha).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> convidar(@RequestBody ConviteUsuarioDTO data,
                                      @AuthenticationPrincipal Usuario admin) {
        if (data.nomeCompleto() == null || data.nomeCompleto().isBlank()
                || data.oab() == null || data.oab().isBlank()
                || data.senhaTemporaria() == null || data.senhaTemporaria().isBlank()) {
            return ResponseEntity.badRequest().body("Informe nome, OAB e senha temporária.");
        }
        if (data.cpf() != null && !data.cpf().isBlank() && repository.existsByCpf(data.cpf())) {
            return ResponseEntity.badRequest().body("CPF já cadastrado.");
        }
        if (data.email() != null && !data.email().isBlank() && repository.existsByEmail(data.email())) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        }
        if (repository.existsByOab(data.oab())) {
            return ResponseEntity.badRequest().body("OAB já cadastrada.");
        }
        // Papel do convidado: nunca SUPER_ADMIN (papel exclusivo da plataforma)
        UsuarioRole role = data.role() != null ? data.role() : UsuarioRole.ROLE_AUDITOR;
        if (role == UsuarioRole.ROLE_SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Papel não permitido.");
        }

        Usuario novo = new Usuario();
        novo.setNomeCompleto(data.nomeCompleto().trim());
        novo.setEmail(data.email());
        novo.setCpf(data.cpf());
        novo.setOab(data.oab().trim());
        novo.setSenha(passwordEncoder.encode(data.senhaTemporaria()));
        novo.setUsuarioRole(role);
        novo.setTenant(admin.getTenant());
        novo.setAtivo(true);
        novo.setForcarTrocaSenha(true);

        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponseDTO.from(repository.save(novo)));
    }

    /** Ativa/desativa um membro do próprio escritório. */
    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> alterarAtivo(@PathVariable Long id,
                                          @RequestParam boolean ativo,
                                          @AuthenticationPrincipal Usuario admin) {
        Usuario alvo = repository.findById(id).orElse(null);
        // Isolamento: admin só gerencia membros do próprio tenant
        if (alvo == null || !admin.getTenantId().equals(alvo.getTenantId())) {
            return ResponseEntity.notFound().build();
        }
        if (alvo.getId_usuario().equals(admin.getId_usuario())) {
            return ResponseEntity.badRequest().body("Não é possível desativar a própria conta.");
        }
        if (alvo.getUsuarioRole() == UsuarioRole.ROLE_SUPER_ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Usuário da plataforma não pode ser alterado.");
        }
        alvo.setAtivo(ativo);
        if (!ativo) {
            alvo.setTokenAtivo(null); // derruba a sessão do desativado
        }
        return ResponseEntity.ok(UsuarioResponseDTO.from(repository.save(alvo)));
    }

    /** Troca a própria senha (qualquer papel, inclusive VISUALIZADOR). */
    @PutMapping("/senha")
    public ResponseEntity<?> alterarSenha(@RequestBody AlterarSenhaDTO data,
                                          @AuthenticationPrincipal Usuario usuario) {
        if (data.novaSenha() == null || data.novaSenha().length() < 6) {
            return ResponseEntity.badRequest().body("A nova senha deve ter ao menos 6 caracteres.");
        }
        if (data.senhaAtual() == null || !passwordEncoder.matches(data.senhaAtual(), usuario.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Senha atual incorreta.");
        }
        usuario.setSenha(passwordEncoder.encode(data.novaSenha()));
        usuario.setForcarTrocaSenha(false);
        repository.save(usuario);
        return ResponseEntity.ok("Senha alterada com sucesso.");
    }
}
