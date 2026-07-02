package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.AuthenticationDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.LoginResponseDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.RegisterDTO;
import br.com.mpgsistemas.revisionalweb.api.model.Tenant;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.model.UsuarioRole;
import br.com.mpgsistemas.revisionalweb.api.repository.TenantRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.UsuarioRepository;
import br.com.mpgsistemas.revisionalweb.api.security.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationDTO data,
                                   @RequestParam(defaultValue = "false") boolean force,
                                   HttpServletRequest request) {

        Usuario usuarioDB = repository.findByLoginComTenant(data.login());

        // Conta bloqueada por tentativas: nem tenta autenticar
        if (usuarioDB != null && !usuarioDB.isAccountNonLocked()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Conta temporariamente bloqueada. Tente novamente mais tarde.");
        }

        // Escritório desativado pela plataforma: bloqueia todos os membros
        if (usuarioDB != null && usuarioDB.getTenant() != null && !usuarioDB.getTenant().isAtivo()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Escritório desativado. Contate o suporte da plataforma.");
        }

        // Sessão já ativa noutro dispositivo (a menos que force=true)
        if (usuarioDB != null && usuarioDB.getTokenAtivo() != null && !force) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Sessão já ativa noutro dispositivo.");
        }

        try {
            var usernamePassword = new UsernamePasswordAuthenticationToken(data.login(), data.senha());
            var auth = this.authenticationManager.authenticate(usernamePassword);

            Usuario usuarioLogado = (Usuario) auth.getPrincipal();

            // Sucesso: zera tentativas e registra login
            usuarioLogado.setTentativasFalhasLogin(0);
            usuarioLogado.setContaBloqueadaAte(null);
            usuarioLogado.setDataUltimoLogin(LocalDateTime.now());
            usuarioLogado.setUltimoIp(resolveIp(request));

            var token = tokenService.generateToken(usuarioLogado);
            usuarioLogado.setTokenAtivo(token); // sobrepõe sessão anterior
            repository.save(usuarioLogado);

            return ResponseEntity.ok(new LoginResponseDTO(token,
                    Boolean.TRUE.equals(usuarioLogado.getForcarTrocaSenha())));

        } catch (AuthenticationException e) {
            // Falha: incrementa tentativas e bloqueia após 5 erros
            if (usuarioDB != null) {
                int tentativas = usuarioDB.getTentativasFalhasLogin() == null ? 0 : usuarioDB.getTentativasFalhasLogin();
                usuarioDB.setTentativasFalhasLogin(tentativas + 1);
                if (usuarioDB.getTentativasFalhasLogin() >= 5) {
                    usuarioDB.setContaBloqueadaAte(LocalDateTime.now().plusMinutes(30));
                }
                repository.save(usuarioDB);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais inválidas.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO data) {
        if (data.cpf() != null && repository.existsByCpf(data.cpf())) {
            return ResponseEntity.badRequest().body("CPF já cadastrado.");
        }
        if (data.email() != null && repository.existsByEmail(data.email())) {
            return ResponseEntity.badRequest().body("E-mail já cadastrado.");
        }
        if (data.oab() != null && repository.existsByOab(data.oab())) {
            return ResponseEntity.badRequest().body("OAB já cadastrada.");
        }
        if (data.nomeEscritorio() == null || data.nomeEscritorio().isBlank()) {
            return ResponseEntity.badRequest().body("Informe o escritório/empresa.");
        }

        // Multi-tenancy: register público SÓ cria escritório novo (o 1º usuário vira
        // ADMIN dele). Anexar-se a escritório existente é vedado — membros extras
        // entram por convite do admin do tenant (POST /api/usuarios).
        String nomeEscritorio = data.nomeEscritorio().trim();
        if (tenantRepository.findByNomeIgnoreCase(nomeEscritorio) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Escritório já cadastrado. Peça um convite ao administrador do escritório.");
        }
        String cnpj = data.cnpjEscritorio() != null ? data.cnpjEscritorio().replaceAll("\\D", "") : "";
        if (!cnpj.isBlank() && tenantRepository.existsByCnpj(cnpj)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("CNPJ já cadastrado. Peça um convite ao administrador do escritório.");
        }

        Tenant tenant = new Tenant();
        tenant.setNome(nomeEscritorio);
        if (!cnpj.isBlank()) {
            tenant.setCnpj(cnpj);
        }
        tenant.setAtivo(true);
        tenant = tenantRepository.save(tenant);

        Usuario novo = new Usuario();
        novo.setNomeCompleto(data.nomeCompleto());
        novo.setEmail(data.email());
        novo.setCpf(data.cpf());
        novo.setOab(data.oab());
        novo.setSenha(passwordEncoder.encode(data.senha()));
        novo.setUsuarioRole(UsuarioRole.ROLE_ADMIN); // dono do escritório
        novo.setTenant(tenant);
        novo.setAtivo(true);

        repository.save(novo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            usuario.setTokenAtivo(null);
            repository.save(usuario);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok("Desconexão realizada com sucesso.");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Nenhum usuário autenticado.");
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> response = new HashMap<>();
        if (authentication != null && authentication.getPrincipal() instanceof Usuario usuario) {
            response.put("nomeCompleto", usuario.getNomeCompleto());
            response.put("role", usuario.getUsuarioRole());
            response.put("id_usuario", usuario.getId());
        }
        return ResponseEntity.ok(response);
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) return request.getRemoteAddr();
        return ip.split(",")[0].trim();
    }
}
