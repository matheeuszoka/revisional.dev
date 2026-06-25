package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.ResultadoExtracao;
import br.com.mpgsistemas.revisionalweb.api.model.ConfiguracaoSistema;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Extração textual de documentos. PDF com texto nativo via PDFBox; PDF escaneado
 * e imagens via OCR Tesseract (Tess4J). Portado de revisional_web/extractor.py.
 *
 * Parâmetros de OCR vêm de ConfiguracaoService (editáveis na tela de admin). OCR é
 * best-effort: sem Tesseract disponível, cai para o texto nativo.
 */
@Service
public class ExtractorService {

    private static final Set<String> IMAGENS = Set.of("jpg", "jpeg", "png", "tif", "tiff", "bmp");
    private static final int LIMIAR_TEXTO_OCR = 200;

    private final ConfiguracaoService config;

    public ExtractorService(ConfiguracaoService config) {
        this.config = config;
    }

    public ResultadoExtracao extrair(byte[] bytes, String nomeArquivo, boolean forcarOcr) {
        ConfiguracaoSistema cfg = config.carregar();
        String ext = extensao(nomeArquivo);
        if ("txt".equals(ext)) {
            return new ResultadoExtracao(new String(bytes, StandardCharsets.UTF_8), "txt_utf8", 1, "");
        }
        if ("pdf".equals(ext)) {
            return extrairPdf(bytes, forcarOcr, cfg);
        }
        if (IMAGENS.contains(ext)) {
            return new ResultadoExtracao(ocrImagem(bytes, cfg), "ocr_image_tesseract", 1, "");
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato não suportado. Use PDF, JPG, JPEG, PNG, TIF, BMP ou TXT.");
    }

    private ResultadoExtracao extrairPdf(byte[] bytes, boolean forcarOcr, ConfiguracaoSistema cfg) {
        StringBuilder avisos = new StringBuilder();
        String texto = "";
        int paginas = 0;
        int maxPaginas = cfg.getOcrMaxPaginas() != null ? cfg.getOcrMaxPaginas() : 25;

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            paginas = doc.getNumberOfPages();
            if (!forcarOcr) {
                try {
                    PDFTextStripper stripper = new PDFTextStripper();
                    stripper.setEndPage(Math.min(paginas, maxPaginas));
                    texto = stripper.getText(doc).strip();
                } catch (Exception e) {
                    avisos.append("Falha extração textual: ").append(e.getMessage()).append(". ");
                }
            }

            if (forcarOcr || normalizar(texto).length() < LIMIAR_TEXTO_OCR) {
                try {
                    String ocr = ocrPdf(doc, cfg);
                    if (normalizar(ocr).length() > normalizar(texto).length()) {
                        return new ResultadoExtracao(ocr, "ocr_pdf_tesseract", paginas, avisos.toString());
                    }
                    avisos.append("OCR não superou a extração textual. ");
                } catch (Exception e) {
                    avisos.append(texto.isBlank()
                            ? "OCR indisponível: " + e.getMessage() + ". "
                            : "OCR ignorado: " + e.getMessage() + ". ");
                }
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Não foi possível ler o PDF enviado.", e);
        }
        return new ResultadoExtracao(texto, "pdfbox_text", paginas, avisos.toString());
    }

    private String ocrPdf(PDDocument doc, ConfiguracaoSistema cfg) throws Exception {
        Tesseract t = novoTesseract(cfg);
        PDFRenderer renderer = new PDFRenderer(doc);
        int dpi = cfg.getOcrDpi() != null ? cfg.getOcrDpi() : 300;
        int total = Math.min(doc.getNumberOfPages(), cfg.getOcrMaxPaginas() != null ? cfg.getOcrMaxPaginas() : 25);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < total; i++) {
            BufferedImage img = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
            sb.append(t.doOCR(img)).append("\n");
        }
        return sb.toString();
    }

    private String ocrImagem(byte[] bytes, ConfiguracaoSistema cfg) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (img == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Imagem inválida ou corrompida.");
            }
            return novoTesseract(cfg).doOCR(img);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Falha no OCR da imagem: " + e.getMessage(), e);
        }
    }

    private Tesseract novoTesseract(ConfiguracaoSistema cfg) {
        Tesseract t = new Tesseract();
        if (cfg.getOcrTessdataPath() != null && !cfg.getOcrTessdataPath().isBlank()) {
            t.setDatapath(cfg.getOcrTessdataPath());
        }
        t.setLanguage(cfg.getOcrIdioma() != null ? cfg.getOcrIdioma() : "por");
        return t;
    }

    private static String extensao(String nome) {
        if (nome == null) return "";
        int i = nome.lastIndexOf('.');
        return i >= 0 ? nome.substring(i + 1).toLowerCase() : "";
    }

    /** Colapsa espaços em branco para medir conteúdo real (heurística texto vs. escaneado). */
    static String normalizar(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").strip();
    }
}
