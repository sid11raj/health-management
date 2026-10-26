package com.healthrecords.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptionTest {

    private final byte[] key = new byte[32];

    AesGcmEncryptionTest() {
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i + 1);
        }
    }

    @AfterEach
    void clearConverterKey() {
        EncryptedStringConverter.setKeyForTests(null);
    }

    @Test
    void encryptDecryptRoundTrip() {
        String encrypted = AesGcmEncryption.encrypt("Diagnosis: hypertension", key);
        assertThat(encrypted).isNotEqualTo("Diagnosis: hypertension");
        assertThat(AesGcmEncryption.decrypt(encrypted, key)).isEqualTo("Diagnosis: hypertension");
    }

    @Test
    void samePlaintextEncryptsDifferentlyEachTime() {
        String first = AesGcmEncryption.encrypt("same", key);
        String second = AesGcmEncryption.encrypt("same", key);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void wrongKeyCannotDecrypt() {
        byte[] otherKey = new byte[32];
        for (int i = 0; i < otherKey.length; i++) {
            otherKey[i] = (byte) (100 + i);
        }
        String encrypted = AesGcmEncryption.encrypt("secret", key);
        assertThatThrownBy(() -> AesGcmEncryption.decrypt(encrypted, otherKey))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shortKeyIsRejected() {
        assertThatThrownBy(() -> AesGcmEncryption.decodeKey(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void converterEncryptsWhenKeySetAndPassesThroughWhenUnset() {
        EncryptedStringConverter converter = new EncryptedStringConverter();
        assertThat(converter.convertToDatabaseColumn("plain")).isEqualTo("plain");
        EncryptedStringConverter.setKeyForTests(key);
        String stored = converter.convertToDatabaseColumn("Treatment: rest");
        assertThat(stored).isNotEqualTo("Treatment: rest");
        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo("Treatment: rest");
        // A pre-encryption plaintext row survives a read after the key is set.
        assertThat(converter.convertToEntityAttribute("old plaintext row")).isEqualTo("old plaintext row");
    }
}
