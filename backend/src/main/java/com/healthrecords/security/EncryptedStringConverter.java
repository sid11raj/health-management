package com.healthrecords.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for sensitive free-text fields (diagnosis, treatment,
 * prescription instructions, TOTP secrets).
 *
 * Behaviour, deliberately documented in README/Security:
 * - When ENCRYPTION_KEY is configured, values are stored as AES-GCM ciphertext.
 * - When it is NOT configured, values pass through as plaintext so a dev
 *   database keeps working — but that is NOT secure storage, and the app
 *   logs a warning at startup (see EncryptionKeyConfig). Production must
 *   set ENCRYPTION_KEY before any sensitive data is written.
 * - On read, if decryption fails (e.g. a row written before encryption was
 *   enabled), the stored value is returned as-is instead of crashing the
 *   whole query. That migration behaviour is intentional, not silent
 *   corruption: such rows remain plaintext and must be re-saved after a
 *   key is configured. Search on encrypted fields is not possible — the
 *   repository searches only cover unencrypted fields (title, medication).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    /** Set by EncryptionKeyConfig at startup; tests may set it directly. */
    static volatile byte[] key;

    static void setKeyForTests(byte[] testKey) {
        key = testKey;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || key == null) {
            return attribute;
        }
        return AesGcmEncryption.encrypt(attribute, key);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || key == null) {
            return dbData;
        }
        try {
            return AesGcmEncryption.decrypt(dbData, key);
        } catch (Exception ignored) {
            // Pre-encryption plaintext row: return as-is (see class javadoc).
            return dbData;
        }
    }
}
