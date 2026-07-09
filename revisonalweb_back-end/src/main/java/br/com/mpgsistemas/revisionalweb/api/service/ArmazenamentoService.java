package br.com.mpgsistemas.revisionalweb.api.service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Armazenamento de documentos no MinIO (bucket revisional-docs). Isola o SDK do
 * resto da aplicação: recebe bytes + chave do objeto, devolve a chave persistida.
 * Cria o bucket sob demanda no primeiro upload.
 */
@Service
public class ArmazenamentoService {

    private final MinioClient minio;
    private final String bucket;

    public ArmazenamentoService(MinioClient minio, @Value("${minio.bucket}") String bucket) {
        this.minio = minio;
        this.bucket = bucket;
    }

    /** Garante a existência do bucket configurado. */
    private void garantirBucket() {
        try {
            boolean existe = minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falha ao preparar o armazenamento de documentos.", e);
        }
    }

    /** Persiste os bytes sob a chave informada. Retorna a própria chave (objectName). */
    public String salvar(String objectName, byte[] conteudo, String contentType) {
        garantirBucket();
        try (InputStream in = new ByteArrayInputStream(conteudo)) {
            minio.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(in, conteudo.length, -1)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Falha ao gravar o documento no armazenamento.", e);
        }
    }

    /** Lê os bytes de um objeto previamente armazenado. */
    public byte[] ler(String objectName) {
        try (InputStream in = minio.getObject(GetObjectArgs.builder()
                .bucket(bucket).object(objectName).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Documento não encontrado no armazenamento.", e);
        }
    }
}
