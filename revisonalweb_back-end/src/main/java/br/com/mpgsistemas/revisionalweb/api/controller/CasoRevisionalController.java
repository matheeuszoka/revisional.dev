package br.com.mpgsistemas.revisionalweb.api.controller;

import br.com.mpgsistemas.revisionalweb.api.dto.AnaliseRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditPackage;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoDetalheDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoResumoDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.PaginaResposta;
import br.com.mpgsistemas.revisionalweb.api.dto.EstatisticasCasosDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ProgressoExtracao;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.UploadDocumentoDTO;
import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.security.TenantContext;
import br.com.mpgsistemas.revisionalweb.api.service.CasoRevisionalService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/casos")
public class CasoRevisionalController {

    private final CasoRevisionalService service;

    public CasoRevisionalController(CasoRevisionalService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResposta<CasoResumoDTO> listar(
            @AuthenticationPrincipal Usuario auditor,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "atualizadoEm") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(required = false) String q,
            // Filtro de status: null=todos, true=laudo pronto, false=em análise
            @RequestParam(required = false) Boolean comResultado) {
        // Rótulo de escritório na visão cross-tenant (mapa vazio p/ papéis comuns).
        var nomesEscritorios = service.nomesEscritorios(auditor);
        return PaginaResposta.de(service.listar(auditor, page, size, sort, dir, q, comResultado),
                c -> CasoResumoDTO.from(c, nomesEscritorios.get(c.getTenantId())));
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

    @PostMapping("/{id}/analisar")
    public CasoDetalheDTO analisar(@PathVariable Long id,
                                   @RequestBody(required = false) AnaliseRequestDTO dto,
                                   @AuthenticationPrincipal Usuario auditor) {
        return CasoDetalheDTO.from(service.analisar(id, dto, auditor));
    }

    /**
     * Anexa um documento (PDF/imagem/TXT) ao caso, extrai texto (OCR) e estrutura
     * os campos via IA + regex. Nada é aplicado automaticamente: os candidatos ficam
     * em candidatosExtracao e o operador confirma via POST /{id}/extracao/aplicar.
     */
    @PostMapping(value = "/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CasoDetalheDTO> upload(@PathVariable Long id,
                                                 @RequestParam("documento") MultipartFile documento,
                                                 @RequestParam(value = "forcarOcr", defaultValue = "false") boolean forcarOcr,
                                                 @AuthenticationPrincipal Usuario auditor) {
        // Fase síncrona (rápida): arquivo salvo. Extração pesada segue em background;
        // o front acompanha por GET /{id}/upload/progresso e recarrega ao concluir.
        CasoRevisionalService.DocumentoAnexado anexo = service.anexarDocumento(id, documento, auditor);
        service.extrairCamposAsync(id, anexo.bytes(), anexo.nomeOriginal(), forcarOcr, auditor, TenantContext.get());
        return ResponseEntity.accepted().body(CasoDetalheDTO.from(anexo.caso()));
    }

    /**
     * Aplica os candidatos de extração escolhidos pelo operador na conferência
     * (checkbox por campo no front). Corpo: [{"campo": "clienteNome", "origem": "ia"}, ...].
     */
    @PostMapping("/{id}/extracao/aplicar")
    public CasoDetalheDTO aplicarExtracao(@PathVariable Long id,
                                          @RequestBody List<CasoRevisionalService.EscolhaExtracao> escolhas,
                                          @AuthenticationPrincipal Usuario auditor) {
        return CasoDetalheDTO.from(service.aplicarExtracao(id, escolhas, auditor));
    }

    /**
     * Progresso do upload/extração em andamento (etapa, mensagem, percentual).
     * O front faz polling aqui enquanto o POST /upload não retorna.
     */
    @GetMapping("/{id}/upload/progresso")
    public ProgressoExtracao progressoUpload(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        return service.progressoUpload(id, auditor);
    }

    /** Pacote de auditoria pericial do caso (score, matriz de itens, fingerprint, normas). */
    @GetMapping("/{id}/auditoria")
    public AuditPackage auditoria(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        return service.gerarAuditoria(id, auditor);
    }

    /** Download standalone do auditoria.json (mesmo conteúdo embutido no ZIP). */
    @GetMapping("/{id}/auditoria/download")
    public ResponseEntity<byte[]> auditoriaDownload(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        byte[] json = service.gerarAuditoriaJson(id, auditor);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header("Content-Disposition", "attachment; filename=\"auditoria_caso_" + id + ".json\"")
                .body(json);
    }

    /** Taxa média BCB (SGS 25471) para preencher a referência de mercado no front. */
    @GetMapping("/referencia-bcb")
    public ReferenciaMercado referenciaBcb() {
        return service.consultarReferenciaBcb();
    }

    /** Números do dashboard: casos do auditor autenticado no tenant corrente. */
    @GetMapping("/estatisticas")
    public EstatisticasCasosDTO estatisticas(@AuthenticationPrincipal Usuario auditor) {
        return service.estatisticas(auditor);
    }

    /**
     * Gera e devolve um dos 4 laudos PDF do caso (parecer/gerencial/notificacao/elementos),
     * inline para visualização via Blob + iframe no front.
     */
    @GetMapping("/{id}/relatorio/{tipo}")
    public ResponseEntity<byte[]> relatorio(@PathVariable Long id, @PathVariable String tipo,
                                            @AuthenticationPrincipal Usuario auditor) {
        byte[] pdf = service.gerarRelatorio(id, tipo, auditor);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "inline; filename=\"" + tipo + "_caso_" + id + ".pdf\"")
                .body(pdf);
    }

    /** Pacote ZIP do caso: os 4 laudos PDF + auditoria.json, para instruir processo. */
    @GetMapping("/{id}/pacote")
    public ResponseEntity<byte[]> pacote(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        byte[] zip = service.gerarPacoteZip(id, auditor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header("Content-Disposition", "attachment; filename=\"pacote_caso_" + id + ".zip\"")
                .body(zip);
    }

    @GetMapping("/{id}/documentos")
    public List<UploadDocumentoDTO> documentos(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        return service.listarDocumentos(id, auditor).stream().map(UploadDocumentoDTO::from).toList();
    }

    /** Conteúdo do documento para visualização inline (PDF/imagem) no front. */
    @GetMapping("/{id}/documentos/{docId}/arquivo")
    public ResponseEntity<byte[]> arquivoDocumento(@PathVariable Long id, @PathVariable Long docId,
                                                   @AuthenticationPrincipal Usuario auditor) {
        var doc = service.lerDocumento(id, docId, auditor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header("Content-Disposition", "inline; filename=\"" + doc.nomeOriginal() + "\"")
                .body(doc.conteudo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal Usuario auditor) {
        service.excluir(id, auditor);
        return ResponseEntity.noContent().build();
    }
}
