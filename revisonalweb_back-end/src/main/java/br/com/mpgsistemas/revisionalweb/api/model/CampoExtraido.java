package br.com.mpgsistemas.revisionalweb.api.model;

import lombok.Data;

// Value object nested em DadosContrato (JSONB). Um campo extraido via OCR com confianca/evidencia.
@Data
public class CampoExtraido {

    private String nome;

    // Valor cru extraido via OCR (sempre texto; conversao numerica e feita no Service)
    private String valor;

    // Null safety: wrapper Double (campo pode vir vazio do OCR)
    private Double confianca = 0.0;

    private String evidencia = "";

    private String origem = "manual";
}
