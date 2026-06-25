package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.AnaliseRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditPackage;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.DocumentoArquivo;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoExtracao;
import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;
import br.com.mpgsistemas.revisionalweb.api.model.EventoAuditoria;
import br.com.mpgsistemas.revisionalweb.api.model.UploadDocumento;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.repository.CasoRevisionalRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.EventoAuditoriaRepository;
import br.com.mpgsistemas.revisionalweb.api.repository.UploadDocumentoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CasoRevisionalService {

    private final CasoRevisionalRepository repository;
    private final CalculadoraFinanceiraService calculadora;
    private final AuditoriaService auditoria;
    private final RelatorioService relatorio;
    private final BcbService bcbService;
    private final ArmazenamentoService armazenamento;
    private final ExtractorService extractor;
    private final ExtracaoIaService extracaoIa;
    private final ParserRegexService parserRegex;
    private final UploadDocumentoRepository uploadRepository;
    private final EventoAuditoriaRepository eventoRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CasoRevisionalService(CasoRevisionalRepository repository,
                                 CalculadoraFinanceiraService calculadora,
                                 AuditoriaService auditoria,
                                 RelatorioService relatorio,
                                 BcbService bcbService,
                                 ArmazenamentoService armazenamento,
                                 ExtractorService extractor,
                                 ExtracaoIaService extracaoIa,
                                 ParserRegexService parserRegex,
                                 UploadDocumentoRepository uploadRepository,
                                 EventoAuditoriaRepository eventoRepository) {
        this.repository = repository;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
        this.relatorio = relatorio;
        this.bcbService = bcbService;
        this.armazenamento = armazenamento;
        this.extractor = extractor;
        this.extracaoIa = extracaoIa;
        this.parserRegex = parserRegex;
        this.uploadRepository = uploadRepository;
        this.eventoRepository = eventoRepository;
    }

    // Colunas reais ordenáveis (cliente/instituição vivem em JSONB, não dá pra ordenar via JPA).
    private static final Set<String> ORDENAVEIS = Set.of("titulo", "criadoEm", "atualizadoEm", "id");

    public Page<CasoRevisional> listar(Usuario auditor, int page, int size, String sort, String dir, String q) {
        String campo = ORDENAVEIS.contains(sort) ? sort : "atualizadoEm";
        Sort.Direction direcao = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(direcao, campo));

        if (q != null && !q.isBlank()) {
            return repository.findByAuditor_CpfAndTituloContainingIgnoreCase(auditor.getCpf(), q.trim(), pageable);
        }
        return repository.findByAuditor_Cpf(auditor.getCpf(), pageable);
    }

    public CasoRevisional buscar(Long id, Usuario auditor) {
        CasoRevisional caso = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Caso não encontrado."));
        // Escopo: só o dono acessa (ou admin)
        if (!pertenceAo(caso, auditor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado a este caso.");
        }
        return caso;
    }

    public CasoRevisional criar(CasoRequestDTO dto, Usuario auditor) {
        CasoRevisional caso = new CasoRevisional();
        caso.setTitulo(dto.titulo());
        caso.setContrato(dto.contrato());
        caso.setAuditor(auditor);
        return repository.save(caso);
    }

    public CasoRevisional atualizar(Long id, CasoRequestDTO dto, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        if (dto.titulo() != null) caso.setTitulo(dto.titulo());
        if (dto.contrato() != null) caso.setContrato(dto.contrato());
        return repository.save(caso);
    }

    /**
     * Executa o motor financeiro sobre o caso e persiste resultado + referencia
     * de mercado. Opcionalmente revisa contrato/mercado antes e busca a taxa BCB.
     */
    public CasoRevisional analisar(Long id, AnaliseRequestDTO dto, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);

        DadosContrato contrato = (dto != null && dto.contrato() != null) ? dto.contrato() : caso.getContrato();
        if (contrato == null) contrato = new DadosContrato();
        caso.setContrato(contrato);

        ReferenciaMercado mercado = (dto != null && dto.mercado() != null) ? dto.mercado() : caso.getMercado();
        if (mercado == null) mercado = new ReferenciaMercado();

        boolean usarBcb = dto != null && Boolean.TRUE.equals(dto.usarBcb());
        boolean semTaxaManual = mercado.getTaxaMensalPct() == null
                && mercado.getTaxaMensalInstituicaoPct() == null
                && mercado.getTaxaAnualPct() == null;
        if (usarBcb && semTaxaManual) {
            try {
                mercado = bcbService.consultarTaxaVeiculoPf();
            } catch (BcbService.BcbException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        ex.getMessage() + " Informe a taxa de referencia manualmente para concluir a analise.");
            }
        }
        caso.setMercado(mercado);

        ResultadoCalculo resultado = calculadora.analisar(contrato, mercado);
        caso.setResultado(resultado);
        CasoRevisional salvo = repository.save(caso);

        // Trilha de auditoria pericial (espelha app.py ANALYSIS_RUN): registra score,
        // classificação e fingerprint do cálculo. O AuditPackage em si é determinístico
        // e recalculado sob demanda em gerarAuditoria().
        AuditPackage audit = auditoria.build(contrato, resultado, mercado);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("score", audit.getScore());
        meta.put("classificacao", resultado.getClassificacaoRisco());
        meta.put("fingerprint", audit.getCalculationFingerprint());
        registrarEvento(salvo, auditor, "ANALYSIS_RUN", meta);

        return salvo;
    }

    /** Pacote de auditoria pericial do caso (score, itens, fingerprint, normas). Determinístico. */
    public AuditPackage gerarAuditoria(Long id, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        return auditoria.build(caso.getContrato(), caso.getResultado(), caso.getMercado());
    }

    /**
     * Gera um dos 4 laudos PDF (parecer/gerencial/notificacao/elementos) sobre o caso.
     * A auditoria é recalculada (determinística) e embutida no documento. Registra REPORT_GENERATE.
     */
    public byte[] gerarRelatorio(Long id, String tipo, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        AuditPackage audit = auditoria.build(caso.getContrato(), caso.getResultado(), caso.getMercado());
        byte[] pdf = relatorio.gerar(tipo, caso.getContrato(), caso.getMercado(), caso.getResultado(), audit);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("tipo", tipo);
        meta.put("score", audit.getScore());
        registrarEvento(caso, auditor, "REPORT_GENERATE", meta);
        return pdf;
    }

    /** Pacote ZIP do caso: os 4 laudos PDF + auditoria.json. Registra BUNDLE_GENERATE. */
    public byte[] gerarPacoteZip(Long id, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        AuditPackage audit = auditoria.build(caso.getContrato(), caso.getResultado(), caso.getMercado());
        byte[] zip = relatorio.gerarPacote(caso.getContrato(), caso.getMercado(), caso.getResultado(), audit);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("score", audit.getScore());
        registrarEvento(caso, auditor, "BUNDLE_GENERATE", meta);
        return zip;
    }

    /**
     * Anexa um documento ao caso: grava no MinIO, extrai o texto (PDFBox/OCR),
     * estrutura os campos via IA (OpenRouter) e funde no contrato preenchendo só
     * os campos vazios (preserva o que o operador já conferiu). O arquivo é sempre
     * salvo, mesmo que a extração falhe — registra-se o evento de auditoria.
     */
    public CasoRevisional processarUpload(Long id, MultipartFile file, boolean forcarOcr, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um documento para anexar.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", e);
        }

        String nomeOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento";
        String hashArquivo = sha256(bytes);
        String objectName = caso.getAuditor().getCpf() + "/" + id + "/" + UUID.randomUUID() + "_" + nomeOriginal;

        // 1) Armazena o documento (sempre).
        armazenamento.salvar(objectName, bytes, file.getContentType());

        UploadDocumento upload = new UploadDocumento();
        upload.setNomeOriginal(nomeOriginal);
        upload.setCaminhoArmazenado(objectName);
        upload.setHashSha256(hashArquivo);
        upload.setCaso(caso);
        uploadRepository.save(upload);

        DadosContrato contrato = caso.getContrato() != null ? caso.getContrato() : new DadosContrato();
        contrato.setContratoArquivo(objectName);
        contrato.setHashContrato(hashArquivo);

        // 2) Extrai texto + estrutura campos (best-effort).
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("arquivo", nomeOriginal);
        try {
            ResultadoExtracao extracao = extractor.extrair(bytes, nomeOriginal, forcarOcr);
            String texto = extracao.texto() != null ? extracao.texto() : "";
            contrato.setHashTextoExtraido(sha256(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            contrato.setAmostraTextoExtraido(texto.length() > 2500 ? texto.substring(0, 2500) : texto);
            contrato.getMetadadosExtracao().put("metodo", extracao.metodo());
            contrato.getMetadadosExtracao().put("paginas", String.valueOf(extracao.paginas()));
            contrato.getMetadadosExtracao().put("avisos", extracao.avisos());

            // IA primeiro (melhor qualidade); regex preenche o que faltar. Ambos só vazios.
            Map<String, CampoExtraido> camposIa = extracaoIa.extrairCampos(texto);
            List<String> aplicadosIa = MapeadorContrato.aplicar(contrato, camposIa, true);
            Map<String, CampoExtraido> camposRegex = parserRegex.extrair(texto);
            List<String> aplicadosRegex = MapeadorContrato.aplicar(contrato, camposRegex, true);

            contrato.getMetadadosExtracao().put("ia", extracaoIa.habilitado() ? "openrouter" : "desativada");
            contrato.getMetadadosExtracao().put("regex", "ativo");
            caso.setContrato(contrato);

            meta.put("metodo", extracao.metodo());
            meta.put("camposAplicadosIa", aplicadosIa);
            meta.put("camposAplicadosRegex", aplicadosRegex);
            meta.put("totalExtraidoIa", camposIa.size());
            meta.put("totalExtraidoRegex", camposRegex.size());
            registrarEvento(caso, auditor, "DOCUMENT_UPLOAD_EXTRACT", meta);
        } catch (ResponseStatusException e) {
            // Falha de extração não derruba o upload: arquivo já está salvo.
            caso.setContrato(contrato);
            meta.put("erro", e.getReason());
            registrarEvento(caso, auditor, "DOCUMENT_UPLOAD_ERROR", meta);
        }

        return repository.save(caso);
    }

    public List<UploadDocumento> listarDocumentos(Long id, Usuario auditor) {
        buscar(id, auditor); // valida escopo
        return uploadRepository.listarPorCaso(id);
    }

    /** Lê os bytes de um documento do caso (para visualização inline/download). */
    public DocumentoArquivo lerDocumento(Long id, Long docId, Usuario auditor) {
        buscar(id, auditor); // valida escopo
        UploadDocumento doc = uploadRepository.findById(docId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado."));
        if (doc.getCaso() == null || !id.equals(doc.getCaso().getId_caso_revisional())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não pertence a este caso.");
        }
        byte[] bytes = armazenamento.ler(doc.getCaminhoArmazenado());
        return new DocumentoArquivo(bytes, contentTypePorNome(doc.getNomeOriginal()), doc.getNomeOriginal());
    }

    private static String contentTypePorNome(String nome) {
        String n = nome == null ? "" : nome.toLowerCase();
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".tif") || n.endsWith(".tiff")) return "image/tiff";
        if (n.endsWith(".bmp")) return "image/bmp";
        if (n.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private void registrarEvento(CasoRevisional caso, Usuario auditor, String tipo, Map<String, Object> payload) {
        try {
            EventoAuditoria evento = new EventoAuditoria();
            evento.setCaso(caso);
            evento.setCpfUsuarioLogado(auditor.getCpf());
            evento.setTipoEvento(tipo);
            evento.setPayloadJson(objectMapper.writeValueAsString(payload));
            eventoRepository.save(evento);
        } catch (Exception ignored) {
            // Auditoria não pode quebrar o fluxo principal.
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void excluir(Long id, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        repository.delete(caso);
    }

    private boolean pertenceAo(CasoRevisional caso, Usuario auditor) {
        return caso.getAuditor() != null
                && caso.getAuditor().getCpf() != null
                && caso.getAuditor().getCpf().equals(auditor.getCpf());
    }
}
