package com.healthrecords.service;

import com.healthrecords.model.RefreshToken;
import com.healthrecords.model.User;
import com.healthrecords.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Refresh-token lifecycle. Raw tokens are 256-bit random values, returned
 * once; only their SHA-256 hash is stored. Rotation revokes the presented
 * token and issues a fresh one, so reuse of an already-rotated token is
 * rejected — a classic sign of token theft, and a reason to revoke all of
 * that user's tokens (see rotate()).
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long expirationMs;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.refresh-token.expiration-ms:604800000}") long expirationMs) {
        this.repository = repository;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public String create(User user) {
        String raw = generateRaw();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(raw));
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusNanos(expirationMs * 1_000_000L));
        repository.save(token);
        return raw;
    }

    /**
     * Validates and rotates a refresh token. Returns the new raw token.
     * If a revoked token is presented again, all of that user's refresh
     * tokens are revoked (possible theft) and rotation is refused.
     */
    @Transactional
    public String rotate(String rawToken) {
        RefreshToken token = find(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));
        if (token.isRevoked()) {
            revokeAll(token.getUser());
            throw new IllegalArgumentException("Invalid refresh token.");
        }
        if (token.isExpired()) {
            token.setRevoked(true);
            repository.save(token);
            throw new IllegalArgumentException("Refresh token has expired.");
        }
        token.setRevoked(true);
        repository.save(token);
        return create(token.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        find(rawToken).ifPresent(token -> {
            token.setRevoked(true);
            repository.save(token);
        });
    }

    @Transactional
    public void revokeAll(User user) {
        repository.findByUser(user).forEach(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                repository.save(token);
            }
        });
    }

    public Optional<RefreshToken> find(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return repository.findByTokenHash(hash(rawToken));
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Refresh token hashing failed", ex);
        }
    }

    private static String generateRaw() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
