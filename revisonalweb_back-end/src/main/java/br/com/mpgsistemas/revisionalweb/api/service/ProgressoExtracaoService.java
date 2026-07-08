package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.ProgressoExtracao;
import br.com.mpgsistemas.revisionalweb.api.security.TenantContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro em memória do progresso de extração (OCR + IA) por caso, para o
 * front acompanhar via polling durante o POST de upload (que é síncrono).
 *
 * Chave inclui o tenant: mesmo caso id em tenants distintos nunca colide e a
 * consulta só enxerga progresso do próprio escritório. Entradas antigas são
 * varridas a cada escrita (TTL) — o mapa não cresce indefinidamente.
 */
@Service
public class ProgressoExtracaoService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private record Entrada(ProgressoExtracao progresso, Instant em) {
    }

    private final Map<String, Entrada> progressos = new ConcurrentHashMap<>();

    public void publicar(Long casoId, String etapa, String mensagem, int percentual) {
        registrar(casoId, new ProgressoExtracao(etapa, mensagem, percentual, false, null));
    }

    public void concluir(Long casoId) {
        registrar(casoId, new ProgressoExtracao("CONCLUIDO", "Processamento concluído.", 100, true, null));
    }

    public void falhar(Long casoId, String erro) {
        registrar(casoId, new ProgressoExtracao("ERRO", "Falha no processamento.", null, true, erro));
    }

    public ProgressoExtracao consultar(Long casoId) {
        Entrada e = progressos.get(chave(casoId));
        return e != null ? e.progresso() : ProgressoExtracao.ocioso();
    }

    private void registrar(Long casoId, ProgressoExtracao progresso) {
        Instant agora = Instant.now();
        progressos.values().removeIf(e -> Duration.between(e.em(), agora).compareTo(TTL) > 0);
        progressos.put(chave(casoId), new Entrada(progresso, agora));
    }

    private static String chave(Long casoId) {
        return TenantContext.get() + ":" + casoId;
    }
}
