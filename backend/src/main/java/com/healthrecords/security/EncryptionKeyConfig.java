package com.healthrecords.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Reads the encryption key once at startup and hands it to the JPA converter. */
@Configuration
public class EncryptionKeyConfig {

    private static final Logger log = LoggerFactory.getLogger(EncryptionKeyConfig.class);

    @Value("${app.encryption.key:}")
    private String base64Key;

    @PostConstruct
    void init() {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("ENCRYPTION_KEY is not set: sensitive fields (diagnosis, treatment, "
                    + "prescription instructions, TOTP secrets) will be stored as PLAINTEXT. "
                    + "Set ENCRYPTION_KEY (Base64, 32 bytes) before storing real patient data.");
            EncryptedStringConverter.key = null;
            return;
        }
        EncryptedStringConverter.key = AesGcmEncryption.decodeKey(base64Key);
        log.info("Field-level encryption is enabled (AES-GCM, key from environment).");
    }
}
