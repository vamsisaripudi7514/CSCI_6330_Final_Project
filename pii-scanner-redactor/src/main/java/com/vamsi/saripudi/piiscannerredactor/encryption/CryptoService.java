package com.vamsi.saripudi.piiscannerredactor.encryption;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Getter
public class CryptoService {
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_BYTES = 12;

    private final SecretKey key;
    private final SecureRandom rng = new SecureRandom();

    public CryptoService(byte[] rawKey) {
        if (rawKey == null || (rawKey.length != 16 && rawKey.length != 24 && rawKey.length != 32)) {
            throw new IllegalArgumentException("AES key must be 16/24/32 bytes");
        }
        this.key = new SecretKeySpec(rawKey, "AES");
    }

    /** Encrypts plaintext and returns base64url(iv||ciphertext). */
    public String encrypt(String plaintext, String aad) {
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            rng.nextBytes(iv);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
//            if (aad != null && !aad.isEmpty()) {
//                c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
//            }
            byte[] ct = c.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] packed = ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(packed);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    /** Decrypts base64url(iv||ciphertext). */
    public String decrypt(String token, String aad) {
        try {
            byte[] packed = Base64.getUrlDecoder().decode(token);
            if (packed.length < GCM_IV_BYTES + 1) throw new IllegalArgumentException("Bad token");

            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ct = new byte[packed.length - GCM_IV_BYTES];
            System.arraycopy(packed, 0, iv, 0, GCM_IV_BYTES);
            System.arraycopy(packed, GCM_IV_BYTES, ct, 0, ct.length);

            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
//            if (aad != null && !aad.isEmpty()) {
//                c.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
//            }
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }
}
