package br.com.mpgsistemas.revisionalweb.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografia simétrica de segredos persistidos (ex.: chave da API de IA).
 * AES-256/GCM com IV aleatório por valor. A chave-mestra vem do ambiente
 * (config.encryption.secret) e NUNCA é persistida no banco — sem ela, o texto
 * cifrado é inútil. Formato armazenado: "enc:v1:" + base64(iv | ciphertext+tag).
 */
@Service
public class CriptografiaService {

    private static final String PREFIXO = "enc:v1:";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec chave;
    private final SecureRandom random = new SecureRandom();

    public CriptografiaService(@Value("${config.encryption.secret}") String segredo) {
        try {
            // Deriva 32 bytes determinísticos do segredo do ambiente.
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(segredo.getBytes(StandardCharsets.UTF_8));
            this.chave = new SecretKeySpec(hash, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar a criptografia.", e);
        }
    }

    public boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }

    /** Cifra um texto claro. Retorna null/vazio inalterado. */
    public String cifrar(String claro) {
        if (claro == null || claro.isBlank()) return claro;
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(claro.getBytes(StandardCharsets.UTF_8));
            byte[] juntos = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, juntos, 0, iv.length);
            System.arraycopy(ct, 0, juntos, iv.length, ct.length);
            return PREFIXO + Base64.getEncoder().encodeToString(juntos);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar segredo.", e);
        }
    }

    /** Decifra um texto cifrado por este serviço. Retorna null/vazio inalterado. */
    public String decifrar(String cifrado) {
        if (cifrado == null || cifrado.isBlank()) return cifrado;
        if (!estaCifrado(cifrado)) return cifrado; // compat: valor em claro legado
        try {
            byte[] juntos = Base64.getDecoder().decode(cifrado.substring(PREFIXO.length()));
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(juntos, 0, iv, 0, IV_BYTES);
            byte[] ct = new byte[juntos.length - IV_BYTES];
            System.arraycopy(juntos, IV_BYTES, ct, 0, ct.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMACAO);
            cipher.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao decifrar segredo (chave-mestra mudou?).", e);
        }
    }
}
