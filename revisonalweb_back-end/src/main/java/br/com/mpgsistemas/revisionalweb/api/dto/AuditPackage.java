package br.com.mpgsistemas.revisionalweb.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacote de auditoria pericial de um caso (port de audit.py AuditPackage).
 * Determinístico: recalculado a partir de contrato + mercado + resultado.
 * Não persiste por si — é a "fotografia" técnica que alimenta laudo/PDF.
 */
@Data
public class AuditPackage {

    private String auditId;
    private String createdAt;
    private String softwareVersion;
    private String documentHash = "";
    private String extractedTextHash = "";
    private int score;
    private List<AuditItem> items = new ArrayList<>();
    private List<FonteNormativa> sources = new ArrayList<>();
    private String calculationFingerprint = "";
    private String inputSnapshotHash = "";
    private List<String> warnings = new ArrayList<>();
}
