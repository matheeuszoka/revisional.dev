package br.com.mpgsistemas.revisionalweb.api.repository;

import br.com.mpgsistemas.revisionalweb.api.model.UploadDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadDocumentoRepository extends JpaRepository<UploadDocumento, Long> {

    // id do caso é 'id_caso_revisional'; o '_' quebra o parser de path, então usamos JPQL.
    @Query("select u from UploadDocumento u where u.caso.id_caso_revisional = :caseId order by u.criadoEm desc")
    List<UploadDocumento> listarPorCaso(@Param("caseId") Long caseId);
}
