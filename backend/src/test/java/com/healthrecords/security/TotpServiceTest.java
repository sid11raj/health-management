package com.healthrecords.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatedSecretIsBase32AndVerifiesAtFixedTime() {
        String secret = totpService.generateSecret();
        assertThat(secret).matches("[A-Z2-7]+");
        // A code generated for a fixed step verifies at that step.
        String code = totpService.generateCode(secret, 1_000_000L);
        assertThat(totpService.verifyAt(secret, code, 1_000_000L * 30L)).isTrue();
    }

    @Test
    void wrongCodeIsRejected() {
        String secret = totpService.generateSecret();
        String code = totpService.generateCode(secret, 1_000_000L);
        String wrong = code.equals("000000") ? "000001" : "000000";
        assertThat(totpService.verifyAt(secret, wrong, 1_000_000L * 30L)).isFalse();
    }

    @Test
    void base32RoundTrip() {
        byte[] original = "hello-totp-test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = TotpService.base32Encode(original);
        assertThat(TotpService.base32Decode(encoded)).isEqualTo(original);
    }

    @Test
    void otpauthUrlContainsSecretAndIssuer() {
        String url = totpService.otpauthUrl("Health Records", "a@example.com", "JBSWY3DPEHPK3PXP");
        assertThat(url).startsWith("otpauth://totp/");
        assertThat(url).contains("secret=JBSWY3DPEHPK3PXP");
    }
}
