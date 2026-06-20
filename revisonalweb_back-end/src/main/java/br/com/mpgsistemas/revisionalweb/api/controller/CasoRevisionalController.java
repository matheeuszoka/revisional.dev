package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.CasoDetalheDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoResumoDTO;
import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.service.CasoRevisionalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/casos")
public class CasoRevisionalController {

    private final CasoRevisionalService service;

    public CasoRevisionalController(CasoRevisionalService service) {
        this.service = service;
    }

    @GetMapping
    public List<CasoResumoDTO> listar(@AuthenticationPrincipal Usuario auditor) {
        return service.listar(auditor).stream().map(CasoResumoDTO::from).toList();
    }

    @GetMapping("/{id}")
    public CasoDetalheDTO buscar(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        return CasoDetalheDTO.from(service.buscar(id, auditor));
    }

    @PostMapping
    public ResponseEntity<CasoResumoDTO> criar(@RequestBody CasoRequestDTO dto,
                                               @AuthenticationPrincipal Usuario auditor) {
        CasoRevisional caso = service.criar(dto, auditor);
        return ResponseEntity.ok(CasoResumoDTO.from(caso));
    }

    @PutMapping("/{id}")
    public CasoResumoDTO atualizar(@PathVariable Long id, @RequestBody CasoRequestDTO dto,
                                   @AuthenticationPrincipal Usuario auditor) {
        return CasoResumoDTO.from(service.atualizar(id, dto, auditor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        service.excluir(id, auditor);
        return ResponseEntity.noContent().build();
    }
}
