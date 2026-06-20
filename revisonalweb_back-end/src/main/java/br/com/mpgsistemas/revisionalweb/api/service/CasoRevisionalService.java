package br.com.mpgsistemas.revisionalweb.api.service;

import br.com.mpgsistemas.revisionalweb.api.dto.CasoRequestDTO;
import br.com.mpgsistemas.revisionalweb.api.model.CasoRevisional;
import br.com.mpgsistemas.revisionalweb.api.model.Usuario;
import br.com.mpgsistemas.revisionalweb.api.repository.CasoRevisionalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CasoRevisionalService {

    private final CasoRevisionalRepository repository;

    public CasoRevisionalService(CasoRevisionalRepository repository) {
        this.repository = repository;
    }

    public List<CasoRevisional> listar(Usuario auditor) {
        return repository.findByAuditor_CpfOrderByAtualizadoEmDesc(auditor.getCpf());
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
