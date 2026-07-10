package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.model.CampoExtraido;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extração de campos por expressões regulares — fallback determinístico que roda
 * SEM chave de IA. Portado de revisional_web/extractor.py (parse_contract_text).
 *
 * Devolve nome(camelCase do DadosContrato) -> CampoExtraido (origem="regex"). O
 * valor é o texto cru casado; a conversão numérica fica no MapeadorContrato.
 */
@Service
public class ParserRegexService {

    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;

    private static final String NUM_BR = "[0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{2})|[0-9]+(?:,[0-9]{2})|[0-9]+(?:\\.[0-9]{2})";
    // OCR de contrato escaneado costuma perder a pontuação ("R$ 90.000,00" -> "r$9000000"):
    // dígitos corridos após R$ são tratados como centavos implícitos (normalizarCentavos).
    private static final String NUM_SEM_PONTUACAO = "[0-9]{3,10}";
    // Separador tolerante entre rótulo percentual e o número ("CET Wam.:263%" tem ".:").
    private static final String SEP_PCT = "[\\s.:\\-=]{0,4}";

    private static final Map<String, List<String>> CAMPOS_MONETARIOS = new LinkedHashMap<>();

    // Percentuais das tabelas de CDC (juros F.4, CET H). OCR degrada "%" para W/H e cola
    // as células: "mensal: % a.m.: 1,48%" -> "mensal:%a.m.:1,48%" / "CET % a.m.: 2,63%" ->
    // "CET Wam.:263%". Padrões toleram [%whº] no lugar do % e espaços opcionais.
    // Juros exige a palavra mensal/anual/juros no contexto para não roubar o valor do CET.
    // "a.m."/"a.a." toleram vírgula no lugar do ponto ("CET a,m:") e % lido como W/H.
    private static final String AM = "a\\s*[.,]?\\s*m\\b";
    private static final String AA = "a\\s*[.,]?\\s*a\\b";
    private static final List<String[]> CAMPOS_PERCENTUAIS = List.of(
            // CET: rótulo H do CDC "CET - CUSTO EFETIVO TOTAL DA OPERAÇÃO ... CET % a.m.: 2,63% CET % a.a.: 37,13%"
            new String[]{"cetMensalPct", "cet\\s*[%whº]{0,2}\\s*" + AM, "cet\\s*(?:mensal|ao\\s*mes)",
                    "custo\\s*efetivo\\s*total(?:\\s*da\\s*opera[cç][aã]o)?[^0-9%]{0,40}?(?:mensal|" + AM + ")"},
            new String[]{"cetAnualPct", "cet\\s*[%whº]{0,2}\\s*" + AA, "cet\\s*(?:anual|ao\\s*ano)",
                    "custo\\s*efetivo\\s*total(?:\\s*da\\s*opera[cç][aã]o)?[^0-9%]{0,40}?(?:anual|" + AA + ")"},
            // Juros remuneratórios: linha F.4 "Taxa de juros remuneratórios diária, mensal e anual:
            // diária: % a.d.: 0,05% mensal: % a.m.: 1,48% anual: % a.a.: 19,32%" (não há campo p/ diária;
            // o âmbito mensal/anual exige a palavra no contexto p/ não roubar o valor do CET).
            new String[]{"taxaJurosMensalPct", "taxa\\s*(?:de\\s*)?juros(?:\\s*remunerat[oó]ri[oa]s)?[^0-9%]{0,60}?mensal",
                    "mensal\\s*:?\\s*[%whº]?\\s*" + AM, "juros\\s*(?:" + AM + "|ao\\s*mes|mensal)"},
            new String[]{"taxaJurosAnualPct", "taxa\\s*(?:de\\s*)?juros(?:\\s*remunerat[oó]ri[oa]s)?[^0-9%]{0,80}?anual",
                    "anual\\s*:?\\s*[%whº]?\\s*" + AA, "juros\\s*(?:" + AA + "|ao\\s*ano|anual)"}
    );

    static {
        // Rótulos SEM acento e minúsculos: labelRegex() gera regex tolerante a OCR
        // (acentos opcionais, \s* entre palavras casa texto colado "Valordoveiculoavista").
        CAMPOS_MONETARIOS.put("valorVeiculo", List.of("valor do veiculo a vista", "valor do bem a vista", "valor a vista", "valor do veiculo", "preco do veiculo", "valor do bem"));
        CAMPOS_MONETARIOS.put("valorEntrada", List.of("valor da entrada", "valor de entrada", "pagamento inicial", "entrada", "sinal"));
        CAMPOS_MONETARIOS.put("valorFinanciado", List.of("valor total financiado", "valor financiado", "total financiado", "valor do financiamento", "credito concedido"));
        CAMPOS_MONETARIOS.put("valorLiquidoLiberado", List.of("valor liquido liberado", "valor liberado", "credito liquido"));
        CAMPOS_MONETARIOS.put("valorParcela", List.of("valor de cada parcela mensal", "valor da parcela", "prestacao", "parcela mensal", "valor de cada parcela"));
        CAMPOS_MONETARIOS.put("iof", List.of("total de impostos a serem financiados", "iof", "imposto sobre operacoes financeiras"));
        CAMPOS_MONETARIOS.put("tarifaCadastro", List.of("tarifa de cadastro", "tarifa cadastro", "cadastro"));
        CAMPOS_MONETARIOS.put("tarifaAvaliacaoBem", List.of("tarifa de avaliacao de bem", "avaliacao do bem", "tarifa de avaliacao"));
        CAMPOS_MONETARIOS.put("tarifaRegistroContrato", List.of("registro de contrato", "registro contrato", "tarifa de registro", "despesa de registro"));
        CAMPOS_MONETARIOS.put("gravame", List.of("registro de gravame", "gravame", "registro contrato-orgao de transito", "orgao de transito"));
        CAMPOS_MONETARIOS.put("seguro", List.of("seguro prestamista", "seguro protecao financeira", "seguro"));
        CAMPOS_MONETARIOS.put("outrosEncargos", List.of("outros encargos", "outras despesas", "servicos de terceiros"));
    }

    /**
     * Converte rótulo (sem acento, minúsculo) em regex tolerante a OCR: espaço vira
     * \s* (OCR cola palavras: "Valordaentrada"), vogais/c casam com e sem acento.
     */
    static String labelRegex(String label) {
        StringBuilder sb = new StringBuilder();
        for (char ch : label.toCharArray()) {
            switch (ch) {
                case ' ' -> sb.append("\\s*");
                case 'a' -> sb.append("[aàáâã]");
                case 'e' -> sb.append("[eéê]");
                case 'i' -> sb.append("[ií]");
                case 'o' -> sb.append("[oóôõ]");
                case 'u' -> sb.append("[uú]");
                case 'c' -> sb.append("[cç]");
                case '-' -> sb.append("[\\-\\s]?");
                default -> sb.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        return sb.toString();
    }

    /** "9000000" (R$ sem pontuação no OCR) -> "90000,00": últimos 2 dígitos são centavos. */
    static String normalizarCentavos(String digitos) {
        if (digitos.length() <= 2) return digitos;
        return digitos.substring(0, digitos.length() - 2) + "," + digitos.substring(digitos.length() - 2);
    }

    /**
     * Percentual sem separador decimal vindo do OCR ("263" de "2,63%", "1932" de "19,32%"):
     * 3-4 dígitos viram vírgula antes dos 2 últimos. 1-2 dígitos ficam como estão.
     */
    static String normalizarPct(String bruto) {
        if (bruto.contains(",") || bruto.contains(".")) return bruto;
        if (bruto.length() >= 3 && bruto.length() <= 4) return normalizarCentavos(bruto);
        return bruto;
    }

    public Map<String, CampoExtraido> extrair(String texto) {
        Map<String, CampoExtraido> out = new LinkedHashMap<>();
        if (texto == null || texto.isBlank()) return out;
        String raw = texto;
        String norm = texto.replaceAll("\\s+", " ").strip();

        // CPF / CNPJ
        achar(norm, "\\b(\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2})\\b", m ->
                add(out, "clienteCpf", m.group(1), 0.92, m.group(0)));
        achar(norm, "\\b(\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2})\\b", m ->
                add(out, "instituicaoCnpj", m.group(1), 0.75, m.group(0)));

        // Nome do cliente. Rótulo do quadro-resumo CDC ("Nome/Razão Social do Cliente:")
        // primeiro — é o mais confiável; genéricos depois.
        for (String pat : List.of(
                "nome\\s*/?\\s*raz[aã]o\\s*social(?:\\s*do\\s*cliente)?\\s*[:\\-]?\\s*([\\p{Lu}][\\p{L}\\s]{5,90})",
                "(?:cliente|financiado|devedor|emitente|comprador|mutuario|mutuário)\\s*[:\\-]?\\s*([\\p{Lu}][\\p{L}\\s]{5,90})",
                "nome\\s*[:\\-]?\\s*([\\p{Lu}][\\p{L}\\s]{5,90})")) {
            Matcher m = Pattern.compile(pat, FLAGS).matcher(raw);
            if (m.find()) {
                String nome = m.group(1).replaceAll("\\s+", " ").strip();
                nome = nome.split("(?i)\\s+(CPF|RG|Endere|Contrato|CNPJ|Data)\\b")[0].strip();
                if (nome.length() >= 5 && !nome.matches("(?i).*(valor|parcela|taxa|contrato).*")) {
                    add(out, "clienteNome", nome, 0.72, m.group(0));
                    break;
                }
            }
        }

        // Instituição financeira (financeiras de montadora/varejo não começam com "BANCO",
        // ex. "AYMORÉ CRÉDITO, FINANCIAMENTO E INVESTIMENTO S.A.")
        for (String pat : List.of(
                "\\b([\\p{Lu}][\\p{Lu}\\s]{1,60}CR[EÉ]DITO,?\\s+FINANCIAMENTO\\s+E\\s+INVESTIMENTO\\s+S\\.?A\\.?)",
                "\\b(BANCO\\s+[^\\n,;]{3,90})",
                "\\b(COOPERATIVA\\s+[^\\n,;]{3,90})",
                "(?:instituicao financeira|instituição financeira|credor|cedente)\\s*[:\\-]?\\s*([^\\n,;]{3,90})")) {
            Matcher m = Pattern.compile(pat, FLAGS).matcher(raw);
            if (m.find()) {
                String banco = m.group(1).replaceAll("\\s+", " ").strip();
                banco = banco.split("(?i)\\s+(CNPJ|Contrato|Agencia|Agência|Data)\\b")[0].strip();
                add(out, "instituicao", banco, 0.72, m.group(0));
                break;
            }
        }

        // Número do contrato ("OPERAÇÃO Nº 104925128/00695937871" no CDC Santander)
        achar(norm, "(?:contrato|cedula|c[eé]dula|ccb|proposta|opera[cç][aã]o)\\s*(?:n[oº°\\.]?|numero|número)?\\s*[:\\-]?\\s*([A-Z0-9][A-Z0-9\\.\\-/]{4,40})",
                m -> add(out, "contratoNumero", m.group(1), 0.80, m.group(0)));

        // Data do contrato. "Data:" seca (rodapé de assinatura) antes do fallback: a primeira
        // data solta do doc costuma ser o 1º vencimento, não a contratação.
        Matcher dt = Pattern.compile("(?:data\\s*(?:do\\s*)?(?:contrato|emissao|emissão|contratacao|contratação)|contratado\\s+em)\\s*[:\\-]?\\s*(\\d{2}/\\d{2}/\\d{4})", FLAGS).matcher(raw);
        if (dt.find()) {
            add(out, "dataContrato", dt.group(1), 0.75, dt.group(0));
        } else {
            Matcher dt1 = Pattern.compile("\\bdata\\s*:\\s*(\\d{2}/\\d{2}/\\d{4})", FLAGS).matcher(raw);
            if (dt1.find()) {
                add(out, "dataContrato", dt1.group(1), 0.70, dt1.group(0));
            } else {
                Matcher dt2 = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4})\\b").matcher(raw);
                if (dt2.find()) add(out, "dataContrato", dt2.group(1), 0.60, dt2.group(0));
            }
        }

        // Descrição do veículo. Campos "Marca:"/"Modelo:" separados (layout tabular) primeiro:
        // composição limpa sem arrastar rótulos vizinhos.
        Matcher mm = Pattern.compile("marca\\s*:\\s*([^\\n:]{2,40}?)\\s+modelo\\s*:\\s*([^\\n]{2,80})", FLAGS).matcher(norm);
        if (mm.find()) {
            String desc = (mm.group(1).strip() + " " + mm.group(2).strip()).replaceAll("\\s+", " ");
            desc = desc.split("(?i)\\s+(ano/?modelo|placa|renavam|chassi|cor|combust[ií]vel|valor)\\b")[0].strip();
            add(out, "veiculoDescricao", desc, 0.80, mm.group(0));
        }
        for (String pat : out.containsKey("veiculoDescricao") ? List.<String>of() : List.of(
                "(?:ve[ií]culo|bem)\\s*[:=]\\s*([^\\n\\r]{4,140})",
                "(?:descri[cç][aã]o\\s+do\\s+(?:ve[ií]culo|bem))\\s*[:=]\\s*([^\\n\\r]{4,140})")) {
            Matcher m = Pattern.compile(pat, FLAGS).matcher(raw);
            if (m.find()) {
                String desc = m.group(1).replaceAll("\\s+", " ").strip();
                desc = desc.split("(?i)\\s+(placa|renavam|chassi|valor|ano)\\b")[0].strip();
                add(out, "veiculoDescricao", desc, 0.72, m.group(0));
                break;
            }
        }

        // Valores monetários por rótulo (labelRegex: tolera OCR colado/sem acento). Três tentativas:
        // 1) estrita: valor BR logo após o rótulo;
        // 2) tolerante: layouts tabulares (CDC Santander etc.) intercalam parênteses de fórmula
        //    "(E.1 + E.4)" e checkboxes "Isenta: sim não Financiada: sim não" entre rótulo e valor —
        //    pula parênteses e até 60 chars sem dígito, mas exige "R$" para ancorar no valor certo;
        // 3) sem pontuação: OCR perde separadores ("r$9000000") — dígitos corridos após R$
        //    viram centavos implícitos, com confiança menor (operador confere no diálogo).
        CAMPOS_MONETARIOS.forEach((campo, labels) -> {
            for (String label : labels) {
                String lr = labelRegex(label);
                String estrito = "(" + lr + ")\\s*[:\\-=]?\\s*(?:R\\$\\s*)?(" + NUM_BR + ")";
                Matcher m = Pattern.compile(estrito, FLAGS).matcher(raw);
                if (m.find()) {
                    add(out, campo, m.group(2), 0.86, m.group(0));
                    return;
                }
                String tolerante = "(" + lr + ")(?:\\s*\\([^)]{0,80}\\))*[^0-9]{0,60}?R\\$\\s*(" + NUM_BR + ")";
                m = Pattern.compile(tolerante, FLAGS).matcher(raw);
                if (m.find()) {
                    add(out, campo, m.group(2), 0.80, m.group(0));
                    return;
                }
                String semPontuacao = "(" + lr + ")(?:\\s*\\([^)]{0,80}\\))*[^0-9]{0,60}?R\\$\\s*(" + NUM_SEM_PONTUACAO + ")\\b";
                m = Pattern.compile(semPontuacao, FLAGS).matcher(raw);
                if (m.find()) {
                    add(out, campo, normalizarCentavos(m.group(2)), 0.62, m.group(0));
                    return;
                }
            }
        });

        // Prazo (meses). \s* entre palavras: OCR cola "NúmerodeParcelasmensais:36".
        for (String pat : List.of(
                "(?:prazo|(?:quantidade|numero|n[uú]mero)\\s*de\\s*parcelas(?:\\s*mensais)?)\\s*[:\\-]?\\s*(\\d{1,3})\\s*(?:meses|parcelas)?",
                "(\\d{1,3})\\s*(?:parcelas\\s*mensais|presta[cç][oõ]es\\s*mensais)")) {
            Matcher m = Pattern.compile(pat, FLAGS).matcher(raw);
            if (m.find()) {
                int n = Integer.parseInt(m.group(1));
                if (n >= 1 && n <= 240) {
                    add(out, "prazoMeses", String.valueOf(n), 0.88, m.group(0));
                    break;
                }
            }
        }

        // Taxas / CET por rótulo. Número aceita forma sem vírgula ("263%" = 2,63% no OCR);
        // normalizarPct decide. Separador tolera ".:" que o OCR insere ("CET Wam.:263%").
        for (String[] spec : CAMPOS_PERCENTUAIS) {
            String campo = spec[0];
            for (int i = 1; i < spec.length; i++) {
                String pat = "(" + spec[i] + ")" + SEP_PCT + "([0-9]{1,3}(?:[.,][0-9]{1,4})?|[0-9]{3,4})\\s*%?";
                Matcher m = Pattern.compile(pat, FLAGS).matcher(raw);
                if (m.find()) {
                    String bruto = m.group(2);
                    String valor = normalizarPct(bruto);
                    add(out, campo, valor, valor.equals(bruto) ? 0.84 : 0.62, m.group(0));
                    break;
                }
            }
        }

        return out;
    }

    private interface OnMatch {
        void apply(Matcher m);
    }

    private void achar(String texto, String regex, OnMatch fn) {
        Matcher m = Pattern.compile(regex, FLAGS).matcher(texto);
        if (m.find()) fn.apply(m);
    }

    private void add(Map<String, CampoExtraido> out, String nome, String valor, double conf, String evidencia) {
        if (valor == null || valor.isBlank()) return;
        CampoExtraido c = new CampoExtraido();
        c.setNome(nome);
        c.setValor(valor.strip());
        c.setConfianca(conf);
        c.setEvidencia(evidencia != null && evidencia.length() > 200 ? evidencia.substring(0, 200) : evidencia);
        c.setOrigem("regex");
        out.put(nome, c);
    }
}
