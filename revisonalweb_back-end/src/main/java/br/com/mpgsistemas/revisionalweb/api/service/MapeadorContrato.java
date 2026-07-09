package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import br.com.mpgsistemas.revisionalweb.api.model.DadosContrato;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aplica os campos extraídos (IA/OCR) sobre o DadosContrato. Estratégia padrão
 * "preencher só vazios": respeita o que o operador já conferiu/preencheu. Faz a
 * conversão texto -> tipo (BR/US) e registra cada campo em camposExtraidos.
 */
final class MapeadorContrato {

    private MapeadorContrato() {}

    private static final Set<String> CAMPOS_DOUBLE = Set.of(
            "valorVeiculo", "valorEntrada", "valorFinanciado", "valorLiquidoLiberado",
            "valorParcela", "taxaJurosMensalPct", "taxaJurosAnualPct", "cetMensalPct",
            "cetAnualPct", "iof", "tarifaCadastro", "tarifaAvaliacaoBem",
            "tarifaRegistroContrato", "gravame", "seguro", "outrosEncargos");

    /** Aplica os campos. Retorna os nomes efetivamente preenchidos. */
    static List<String> aplicar(DadosContrato alvo, Map<String, CampoExtraido> campos, boolean soVazios) {
        List<String> aplicados = new ArrayList<>();
        if (alvo == null || campos == null) return aplicados;

        campos.forEach((nome, campo) -> {
            String valor = campo.getValor();
            if (valor == null || valor.isBlank()) return;
            boolean preencheu = setValor(alvo, nome, valor, soVazios);
            if (preencheu) {
                alvo.getCamposExtraidos().put(nome, campo);
                aplicados.add(nome);
            }
        });
        return aplicados;
    }

    private static boolean setValor(DadosContrato c, String nome, String valor, boolean soVazios) {
        if ("prazoMeses".equals(nome)) {
            if (soVazios && c.getPrazoMeses() != null) return false;
            Integer n = parseInt(valor);
            if (n == null) return false;
            c.setPrazoMeses(n);
            return true;
        }
        if (CAMPOS_DOUBLE.contains(nome)) {
            if (soVazios && getDouble(c, nome) != null) return false;
            Double d = parseNum(valor);
            if (d == null) return false;
            setDouble(c, nome, d);
            return true;
        }
        // Demais campos são String.
        if (soVazios && !isBlank(getString(c, nome))) return false;
        return setString(c, nome, valor);
    }

    // --- conversões numéricas (aceita "1.234,56" BR e "1234.56" US) ---

    static Double parseNum(String raw) {
        if (raw == null) return null;
        String s = raw.replaceAll("[^0-9,.-]", "").trim();
        if (s.isEmpty()) return null;
        boolean temVirgula = s.contains(",");
        boolean temPonto = s.contains(".");
        if (temVirgula && temPonto) {
            s = s.replace(".", "").replace(",", "."); // ponto = milhar, vírgula = decimal
        } else if (temVirgula) {
            s = s.replace(",", ".");
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static Integer parseInt(String raw) {
        Double d = parseNum(raw);
        return d == null ? null : (int) Math.round(d);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // --- acessores explícitos (sem reflexão; null-safe) ---

    private static Double getDouble(DadosContrato c, String n) {
        return switch (n) {
            case "valorVeiculo" -> c.getValorVeiculo();
            case "valorEntrada" -> c.getValorEntrada();
            case "valorFinanciado" -> c.getValorFinanciado();
            case "valorLiquidoLiberado" -> c.getValorLiquidoLiberado();
            case "valorParcela" -> c.getValorParcela();
            case "taxaJurosMensalPct" -> c.getTaxaJurosMensalPct();
            case "taxaJurosAnualPct" -> c.getTaxaJurosAnualPct();
            case "cetMensalPct" -> c.getCetMensalPct();
            case "cetAnualPct" -> c.getCetAnualPct();
            case "iof" -> c.getIof();
            case "tarifaCadastro" -> c.getTarifaCadastro();
            case "tarifaAvaliacaoBem" -> c.getTarifaAvaliacaoBem();
            case "tarifaRegistroContrato" -> c.getTarifaRegistroContrato();
            case "gravame" -> c.getGravame();
            case "seguro" -> c.getSeguro();
            case "outrosEncargos" -> c.getOutrosEncargos();
            default -> null;
        };
    }

    private static void setDouble(DadosContrato c, String n, Double v) {
        switch (n) {
            case "valorVeiculo" -> c.setValorVeiculo(v);
            case "valorEntrada" -> c.setValorEntrada(v);
            case "valorFinanciado" -> c.setValorFinanciado(v);
            case "valorLiquidoLiberado" -> c.setValorLiquidoLiberado(v);
            case "valorParcela" -> c.setValorParcela(v);
            case "taxaJurosMensalPct" -> c.setTaxaJurosMensalPct(v);
            case "taxaJurosAnualPct" -> c.setTaxaJurosAnualPct(v);
            case "cetMensalPct" -> c.setCetMensalPct(v);
            case "cetAnualPct" -> c.setCetAnualPct(v);
            case "iof" -> c.setIof(v);
            case "tarifaCadastro" -> c.setTarifaCadastro(v);
            case "tarifaAvaliacaoBem" -> c.setTarifaAvaliacaoBem(v);
            case "tarifaRegistroContrato" -> c.setTarifaRegistroContrato(v);
            case "gravame" -> c.setGravame(v);
            case "seguro" -> c.setSeguro(v);
            case "outrosEncargos" -> c.setOutrosEncargos(v);
            default -> { /* ignora desconhecido */ }
        }
    }

    private static String getString(DadosContrato c, String n) {
        return switch (n) {
            case "clienteNome" -> c.getClienteNome();
            case "clienteCpf" -> c.getClienteCpf();
            case "instituicao" -> c.getInstituicao();
            case "instituicaoCnpj" -> c.getInstituicaoCnpj();
            case "contratoNumero" -> c.getContratoNumero();
            case "modalidade" -> c.getModalidade();
            case "dataContrato" -> c.getDataContrato();
            case "veiculoDescricao" -> c.getVeiculoDescricao();
            case "descricaoOutrosEncargos" -> c.getDescricaoOutrosEncargos();
            default -> null;
        };
    }

    /** Retorna true se o campo é conhecido e foi setado. */
    private static boolean setString(DadosContrato c, String n, String v) {
        switch (n) {
            case "clienteNome" -> c.setClienteNome(v);
            case "clienteCpf" -> c.setClienteCpf(v);
            case "instituicao" -> c.setInstituicao(v);
            case "instituicaoCnpj" -> c.setInstituicaoCnpj(v);
            case "contratoNumero" -> c.setContratoNumero(v);
            case "modalidade" -> c.setModalidade(v);
            case "dataContrato" -> c.setDataContrato(v);
            case "veiculoDescricao" -> c.setVeiculoDescricao(v);
            case "descricaoOutrosEncargos" -> c.setDescricaoOutrosEncargos(v);
            default -> { return false; }
        }
        return true;
    }
}
