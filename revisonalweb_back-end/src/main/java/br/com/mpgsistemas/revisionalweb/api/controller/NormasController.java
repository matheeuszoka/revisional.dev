package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.config.MatrizNormativa;
import br.com.mpgsistemas.revisionalweb.api.dto.FonteNormativa;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Matriz normativa (leis/súmulas/resoluções). Port de norms.py — também consumida
 * pela auditoria e pelos laudos. Estática: independe de tenant.
 */
@RestController
@RequestMapping("/api/normas")
public class NormasController {

    private final MatrizNormativa normas;

    public NormasController(MatrizNormativa normas) {
        this.normas = normas;
    }

    @GetMapping
    public List<FonteNormativa> listar() {
        return normas.fontes();
    }
}
