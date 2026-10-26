package com.healthrecords.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * TOTP (RFC 6238, SHA-1, 30-second steps, 6 digits) implemented with pure
 * JDK crypto — no extra dependency, and no hardcoded or demo code.
 * Secrets are Base32 (RFC 4648, no padding) so they can be pasted into any
 * standard authenticator app via the otpauth:// URL returned at setup.
 * Verification accepts the current step plus one step either side, to allow
 * for clock skew, but nothing wider.
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final long TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;

    public String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String otpauthUrl(String issuer, String accountName, String secret) {
        return "otpauth://totp/" + urlEncode(issuer) + ":" + urlEncode(accountName)
                + "?secret=" + secret + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    public boolean verify(String secret, String code) {
        return verifyAt(secret, code, Instant.now().getEpochSecond());
    }

    /** Package-visible for tests: verify against a fixed epoch second. */
    boolean verifyAt(String secret, String code, long epochSeconds) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long step = epochSeconds / TIME_STEP_SECONDS;
        for (long offset = -1; offset <= 1; offset++) {
            if (generateCode(secret, step + offset).equals(code)) {
                return true;
            }
        }
        return false;
    }

    /** Package-visible for tests: the code for a given time step. */
    String generateCode(String secret, long step) {
        try {
            byte[] key = base32Decode(secret);
            byte[] data = new byte[8];
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (step & 0xFF);
                step >>= 8;
            }
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            int code = binary % 1_000_000;
            return String.format("%06d", code);
        } catch (Exception ex) {
            throw new IllegalStateException("TOTP generation failed", ex);
        }
    }

    static String base32Encode(byte[] bytes) {
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                out.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return out.toString();
    }

    static byte[] base32Decode(String encoded) {
        String clean = encoded.trim().replace("=", "").toUpperCase(java.util.Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character in TOTP secret");
            }
            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
