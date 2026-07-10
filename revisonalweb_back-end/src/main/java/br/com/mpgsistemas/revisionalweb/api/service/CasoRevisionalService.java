package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.AnaliseRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.AuditPackage;
import br.com.mpgsistemas.revisionalweb.api.dto.CasoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ReferenciaMercado;
import br.com.mpgsistemas.revisionalweb.api.dto.DocumentoArquivo;
import br.com.mpgsistemas.revisionalweb.api.dto.EstatisticasCasosDTO;
import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoCalculo;
import br.com.mpgsistemas.revisionalweb.api.dto.ProgressoExtracao;
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
import br.com.mpgsistemas.revisionalweb.api.security.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ProgressoExtracaoService progressoExtracao;
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
                                 EventoAuditoriaRepository eventoRepository,
                                 ProgressoExtracaoService progressoExtracao) {
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
        this.progressoExtracao = progressoExtracao;
    }

    // Colunas reais ordenáveis, mapeadas p/ o nome físico (a listagem usa query
    // nativa; cliente/instituição vivem em JSONB e não são ordenáveis).
    private static final Map<String, String> ORDENAVEIS = Map.of(
            "titulo", "titulo",
            "criadoEm", "criado_em",
            "atualizadoEm", "atualizado_em",
            "id", "id_caso_revisional");

    public Page<CasoRevisional> listar(Usuario auditor, int page, int size, String sort, String dir,
                                       String q, Boolean comResultado) {
        String coluna = ORDENAVEIS.getOrDefault(sort, "atualizado_em");
        Sort.Direction direcao = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(direcao, coluna));

        String termo = (q == null || q.isBlank()) ? null : q.trim();
        return repository.buscarPagina(TenantContext.get(), auditor.getCpf(), termo, comResultado, pageable);
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
        if (dto.contrato() != null) caso.setContrato(preservarExtracao(caso.getContrato(), dto.contrato()));
        return repository.save(caso);
    }

    /**
     * O form do front não envia as composições de extração nem os hashes do documento —
     * substituir o JSONB inteiro apagaria camposExtraidos, candidatosExtracao e a trilha
     * do arquivo a cada save. Preserva o que o novo contrato veio sem.
     */
    private static DadosContrato preservarExtracao(DadosContrato atual, DadosContrato novo) {
        if (atual == null || novo == null) return novo;
        if (novo.getCamposExtraidos() == null || novo.getCamposExtraidos().isEmpty())
            novo.setCamposExtraidos(atual.getCamposExtraidos());
        if (novo.getCandidatosExtracao() == null || novo.getCandidatosExtracao().isEmpty())
            novo.setCandidatosExtracao(atual.getCandidatosExtracao());
        if (novo.getMetadadosExtracao() == null || novo.getMetadadosExtracao().isEmpty())
            novo.setMetadadosExtracao(atual.getMetadadosExtracao());
        if (isBlank(novo.getContratoArquivo())) novo.setContratoArquivo(atual.getContratoArquivo());
        if (isBlank(novo.getHashContrato())) novo.setHashContrato(atual.getHashContrato());
        if (isBlank(novo.getHashTextoExtraido())) novo.setHashTextoExtraido(atual.getHashTextoExtraido());
        if (isBlank(novo.getAmostraTextoExtraido())) novo.setAmostraTextoExtraido(atual.getAmostraTextoExtraido());
        return novo;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Executa o motor financeiro sobre o caso e persiste resultado + referencia
     * de mercado. Opcionalmente revisa contrato/mercado antes e busca a taxa BCB.
     */
    public CasoRevisional analisar(Long id, AnaliseRequestDTO dto, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);

        DadosContrato contrato = (dto != null && dto.contrato() != null)
                ? preservarExtracao(caso.getContrato(), dto.contrato())
                : caso.getContrato();
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
                progressoExtracao.publicar(id, "BCB", "Consultando taxa média de mercado no Banco Central…", 25);
                mercado = bcbService.consultarTaxaVeiculoPf();
            } catch (BcbService.BcbException ex) {
                progressoExtracao.falhar(id, "Falha na consulta ao Banco Central.");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        ex.getMessage() + " Informe a taxa de referencia manualmente para concluir a analise.");
            }
        }
        caso.setMercado(mercado);

        progressoExtracao.publicar(id, "CALCULO", "Executando motor de cálculo (PRICE, CET, spread)…", 55);
        ResultadoCalculo resultado = calculadora.analisar(contrato, mercado);
        caso.setResultado(resultado);
        progressoExtracao.publicar(id, "AUDITORIA", "Gerando trilha de auditoria pericial…", 85);
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

        progressoExtracao.concluir(id);
        return salvo;
    }

    /** Pacote de auditoria pericial do caso (score, itens, fingerprint, normas). Determinístico. */
    public AuditPackage gerarAuditoria(Long id, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        return auditoria.build(caso.getContrato(), caso.getResultado(), caso.getMercado());
    }

    /** auditoria.json standalone (mesmo conteúdo do ZIP). Registra AUDIT_JSON_DOWNLOAD. */
    public byte[] gerarAuditoriaJson(Long id, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        AuditPackage audit = auditoria.build(caso.getContrato(), caso.getResultado(), caso.getMercado());
        byte[] json = relatorio.auditoriaJson(audit);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("score", audit.getScore());
        meta.put("fingerprint", audit.getCalculationFingerprint());
        registrarEvento(caso, auditor, "AUDIT_JSON_DOWNLOAD", meta);
        return json;
    }

    /** Números do dashboard (casos do auditor no tenant corrente). */
    public EstatisticasCasosDTO estatisticas(Usuario auditor) {
        CasoRevisionalRepository.EstatisticasCasos e =
                repository.estatisticas(TenantContext.get(), auditor.getCpf());
        return new EstatisticasCasosDTO(e.getTotal(), e.getLaudoPronto(),
                e.getTotal() - e.getLaudoPronto(), e.getIndicioForte(), e.getIndicioModerado());
    }

    /** Consulta avulsa da taxa BCB (preenche a referência de mercado no front). */
    public ReferenciaMercado consultarReferenciaBcb() {
        try {
            return bcbService.consultarTaxaVeiculoPf();
        } catch (BcbService.BcbException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
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
    /** Resultado da fase síncrona do upload (arquivo já persistido no MinIO). */
    public record DocumentoAnexado(CasoRevisional caso, byte[] bytes, String nomeOriginal) {
    }

    /**
     * Fase 1 (síncrona): valida, armazena o arquivo no MinIO e registra o
     * UploadDocumento. Rápida — a extração pesada roda depois em background.
     */
    public DocumentoAnexado anexarDocumento(Long id, MultipartFile file, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um documento para anexar.");
        }

        progressoExtracao.publicar(id, "ENVIO", "Documento recebido, preparando processamento…", 10);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            progressoExtracao.falhar(id, "Não foi possível ler o arquivo enviado.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", e);
        }

        String nomeOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento";
        String hashArquivo = sha256(bytes);
        String objectName = caso.getAuditor().getCpf() + "/" + id + "/" + UUID.randomUUID() + "_" + nomeOriginal;

        progressoExtracao.publicar(id, "ARMAZENANDO", "Armazenando o documento original…", 20);
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
        caso.setContrato(contrato);

        return new DocumentoAnexado(repository.save(caso), bytes, nomeOriginal);
    }

    /**
     * Fase 2 (assíncrona): extração de texto (PDF/OCR) + IA + regex, fundindo só
     * campos vazios. Roda fora da thread do Tomcat — o front acompanha por polling
     * do progresso e recarrega o caso quando terminado.
     *
     * TenantContext é ThreadLocal: precisa ser setado aqui (thread do executor)
     * para o filtro @TenantId do Hibernate e a chave do progresso funcionarem.
     */
    @Async("extracaoExecutor")
    public void extrairCamposAsync(Long id, byte[] bytes, String nomeOriginal, boolean forcarOcr,
                                   Usuario auditor, Long tenantId) {
        TenantContext.set(tenantId);
        try {
            CasoRevisional caso = repository.findById(id).orElse(null);
            if (caso == null) {
                progressoExtracao.falhar(id, "Caso não encontrado.");
                return;
            }
            DadosContrato contrato = caso.getContrato() != null ? caso.getContrato() : new DadosContrato();

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("arquivo", nomeOriginal);
            try {
                progressoExtracao.publicar(id, "EXTRAINDO", "Extraindo texto do documento…", 35);
                // OCR reporta página a página; faixa 35–60% do progresso total.
                ResultadoExtracao extracao = extractor.extrair(bytes, nomeOriginal, forcarOcr,
                        (atual, total) -> progressoExtracao.publicar(id, "OCR",
                                "OCR na página " + atual + " de " + total + "…",
                                35 + (int) Math.round(atual * 25.0 / Math.max(total, 1))));
                String texto = extracao.texto() != null ? extracao.texto() : "";
                contrato.setHashTextoExtraido(sha256(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                contrato.setAmostraTextoExtraido(texto.length() > 2500 ? texto.substring(0, 2500) : texto);
                contrato.getMetadadosExtracao().put("metodo", extracao.metodo());
                contrato.getMetadadosExtracao().put("paginas", String.valueOf(extracao.paginas()));
                contrato.getMetadadosExtracao().put("avisos", extracao.avisos());

                // IA e regex extraem em paralelo conceitual; nada é aplicado ao contrato aqui.
                // Os candidatos das duas origens ficam em candidatosExtracao e o operador
                // escolhe no front o que entra no formulário (POST /extracao/aplicar).
                // Texto integral vai à IA em janelas (contrato real excede 1 chamada); faixa 60–85%.
                progressoExtracao.publicar(id, "IA", "IA analisando e mapeando os campos do contrato…", 62);
                Map<String, CampoExtraido> camposIa = extracaoIa.extrairCampos(texto,
                        (atual, total) -> progressoExtracao.publicar(id, "IA",
                                total > 1
                                        ? "IA analisando parte " + atual + " de " + total + " do contrato…"
                                        : "IA analisando e mapeando os campos do contrato…",
                                62 + (int) Math.round(atual * 23.0 / Math.max(total, 1))));
                progressoExtracao.publicar(id, "REGEX", "Extraindo campos com o parser estrutural…", 85);
                Map<String, CampoExtraido> camposRegex = parserRegex.extrair(texto);

                // Upload em lote acumula candidatos (última extração de cada origem vence);
                // valores idênticos entre origens colapsam no de maior confiança.
                contrato.setCandidatosExtracao(
                        montarCandidatos(contrato.getCandidatosExtracao(), camposIa, camposRegex));

                contrato.getMetadadosExtracao().put("ia", extracaoIa.habilitado() ? "openrouter" : "desativada");
                contrato.getMetadadosExtracao().put("regex", "ativo");
                caso.setContrato(contrato);

                meta.put("metodo", extracao.metodo());
                meta.put("totalExtraidoIa", camposIa.size());
                meta.put("totalExtraidoRegex", camposRegex.size());
                registrarEvento(caso, auditor, "DOCUMENT_UPLOAD_EXTRACT", meta);
            } catch (ResponseStatusException e) {
                // Falha de extração não derruba o upload: arquivo já está salvo.
                caso.setContrato(contrato);
                meta.put("erro", e.getReason());
                registrarEvento(caso, auditor, "DOCUMENT_UPLOAD_ERROR", meta);
            }

            progressoExtracao.publicar(id, "SALVANDO", "Consolidando dados extraídos…", 95);
            repository.save(caso);
            progressoExtracao.concluir(id);
        } catch (Throwable e) {
            // Throwable, não Exception: o Tesseract/JNA lança java.lang.Error — se escapar,
            // o progresso nunca recebe falhar() e o front fica pendurado no polling.
            // LGPD: sem dados do contrato na mensagem de erro.
            progressoExtracao.falhar(id, "Falha inesperada no processamento do documento.");
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Junta os candidatos de IA e regex por campo sobre os já existentes (upload em
     * lote acumula). No máximo um candidato por origem em cada campo — a extração mais
     * recente da origem substitui a anterior. Quando IA e regex concordam no valor,
     * colapsa no de maior confiança; quando divergem, ambos ficam e o operador decide.
     */
    static Map<String, List<CampoExtraido>> montarCandidatos(Map<String, List<CampoExtraido>> base,
                                                             Map<String, CampoExtraido> ia,
                                                             Map<String, CampoExtraido> regex) {
        Map<String, Map<String, CampoExtraido>> porOrigem = new LinkedHashMap<>();
        if (base != null) {
            base.forEach((nome, lista) -> lista.forEach(c -> porOrigem
                    .computeIfAbsent(nome, k -> new LinkedHashMap<>())
                    .put(c.getOrigem() != null ? c.getOrigem() : "ocr", c)));
        }
        ia.forEach((nome, c) -> porOrigem.computeIfAbsent(nome, k -> new LinkedHashMap<>()).put("ia", c));
        regex.forEach((nome, c) -> porOrigem.computeIfAbsent(nome, k -> new LinkedHashMap<>()).put("regex", c));

        Map<String, List<CampoExtraido>> out = new LinkedHashMap<>();
        porOrigem.forEach((nome, origens) -> {
            List<CampoExtraido> lista = new java.util.ArrayList<>(origens.values());
            if (lista.size() == 2 && lista.get(0).getValor() != null
                    && lista.get(0).getValor().equalsIgnoreCase(lista.get(1).getValor())) {
                double c0 = lista.get(0).getConfianca() != null ? lista.get(0).getConfianca() : 0.0;
                double c1 = lista.get(1).getConfianca() != null ? lista.get(1).getConfianca() : 0.0;
                lista = new java.util.ArrayList<>(List.of(c1 > c0 ? lista.get(1) : lista.get(0)));
            }
            out.put(nome, lista);
        });
        return out;
    }

    /** Escolha do operador na conferência: qual candidato (campo+origem) entra no formulário. */
    public record EscolhaExtracao(String campo, String origem) {
    }

    /**
     * Aplica os candidatos escolhidos pelo operador sobre o contrato (sobrescreve o
     * valor atual do campo — a escolha é explícita). Registra EXTRACTION_APPLY com a
     * lista aplicada; os candidatos permanecem salvos para reconferência posterior.
     */
    public CasoRevisional aplicarExtracao(Long id, List<EscolhaExtracao> escolhas, Usuario auditor) {
        CasoRevisional caso = buscar(id, auditor);
        DadosContrato contrato = caso.getContrato() != null ? caso.getContrato() : new DadosContrato();
        Map<String, List<CampoExtraido>> candidatos = contrato.getCandidatosExtracao();
        if (candidatos == null || candidatos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há extração pendente de conferência neste caso. Anexe um documento primeiro.");
        }

        Map<String, CampoExtraido> escolhidos = new LinkedHashMap<>();
        for (EscolhaExtracao e : escolhas != null ? escolhas : List.<EscolhaExtracao>of()) {
            List<CampoExtraido> lista = candidatos.get(e.campo());
            if (lista == null) continue;
            lista.stream()
                    .filter(c -> c.getOrigem() != null && c.getOrigem().equalsIgnoreCase(e.origem()))
                    .findFirst()
                    .ifPresent(c -> escolhidos.put(e.campo(), c));
        }
        List<String> aplicados = MapeadorContrato.aplicar(contrato, escolhidos, false);
        caso.setContrato(contrato);
        CasoRevisional salvo = repository.save(caso);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("camposAplicados", aplicados);
        meta.put("totalCandidatos", candidatos.size());
        registrarEvento(salvo, auditor, "EXTRACTION_APPLY", meta);
        return salvo;
    }

    /** Progresso corrente do upload/extração do caso (consultado por polling pelo front). */
    public ProgressoExtracao progressoUpload(Long id, Usuario auditor) {
        buscar(id, auditor); // valida escopo (tenant + papel)
        return progressoExtracao.consultar(id);
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
