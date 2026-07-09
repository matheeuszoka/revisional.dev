package br.com.mpgsistemas.revisionalweb.api.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatação pt-BR para laudos/PDF (port de utils.py: money_br, pct_br, format_cpf, int_br, now_br).
 */
public final class FormatoBr {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private FormatoBr() {
    }

    public static String moeda(Double valor) {
        if (valor == null) return "Não informado";
        String s = String.format(Locale.US, "R$ %,.2f", valor); // 1,234.56
        return s.replace(",", "X").replace(".", ",").replace("X", ".");
    }

    public static String pct(Double valor) {
        if (valor == null) return "Não informado";
        return String.format(Locale.US, "%.2f", valor).replace(".", ",") + "%";
    }

    public static String inteiro(Integer valor) {
        return valor == null ? "Não informado" : String.valueOf(valor);
    }

    public static String cpf(String cpf) {
        String d = cpf == null ? "" : cpf.replaceAll("\\D", "");
        if (d.length() != 11) return cpf == null ? "" : cpf;
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static String agora() {
        return LocalDateTime.now().format(DATA_HORA);
    }
}
