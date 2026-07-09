package br.com.mpgsistemas.revisionalweb.api.dto;

// Resultado bruto da extração textual de um documento (antes de estruturar campos).
// metodo: pdfbox_text | ocr_pdf_tesseract | ocr_image_tesseract | txt_utf8.
public record ResultadoExtracao(
        String texto,
        String metodo,
        int paginas,
        String avisos
) {
}
