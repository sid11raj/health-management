package com.healthrecords.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM field encryption (pure JDK, no extra dependency).
 * Format: Base64( 12-byte random IV || ciphertext with 128-bit GCM tag ).
 * The key must be 32 bytes (AES-256), supplied as Base64 in ENCRYPTION_KEY.
 * A fresh IV per value means the same plaintext encrypts differently each time.
 */
public final class AesGcmEncryption {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private AesGcmEncryption() {
    }

    public static byte[] decodeKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException("ENCRYPTION_KEY is not set. It must be Base64, decoding to 32 bytes.");
        }
        byte[] key = Base64.getDecoder().decode(base64Key.trim());
        if (key.length != 32) {
            throw new IllegalStateException("ENCRYPTION_KEY must decode to exactly 32 bytes (AES-256).");
        }
        return key;
    }

    public static String encrypt(String plaintext, byte[] key) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Field encryption failed", ex);
        }
    }

    public static String decrypt(String encoded, byte[] key) {
        if (encoded == null) {
            return null;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length <= IV_BYTES) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[bytes.length - IV_BYTES];
            System.arraycopy(bytes, 0, iv, 0, IV_BYTES);
            System.arraycopy(bytes, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Field decryption failed", ex);
        }
    }
}
