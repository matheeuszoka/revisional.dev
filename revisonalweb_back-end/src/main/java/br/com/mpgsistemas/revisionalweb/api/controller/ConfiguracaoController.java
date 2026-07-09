package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.ConfiguracaoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ConfiguracaoResponseDTO;
import br.com.mpgsistemas.revisionalweb.api.service.ConfiguracaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Parâmetros operacionais (OCR + IA). Somente ADMIN. A chave da IA é gravada
 * cifrada e nunca retornada em claro (o GET só informa se há chave definida).
 */
@RestController
@RequestMapping("/api/configuracoes")
@PreAuthorize("hasRole('ADMIN')")
public class ConfiguracaoController {

    private final ConfiguracaoService service;

    public ConfiguracaoController(ConfiguracaoService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracaoResponseDTO obter() {
        return ConfiguracaoResponseDTO.from(service.carregar());
    }

    @PutMapping
    public ConfiguracaoResponseDTO atualizar(@RequestBody ConfiguracaoRequestDTO dto) {
        boolean limpar = Boolean.TRUE.equals(dto.limparApiKey());
        return ConfiguracaoResponseDTO.from(service.atualizar(dto.toPatch(), dto.openRouterApiKey(), limpar));
    }
}
