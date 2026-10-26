package com.healthrecords.dto;

import com.healthrecords.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public record RegisterRequest(
            @NotBlank @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters") String name,
            @Email @NotBlank @Size(max = 255) String email,
            // Server-side password policy (mirrored in PasswordPolicy and the frontend hint):
            // at least 12 chars, with upper, lower, digit and symbol.
            @NotBlank @Size(min = 12, max = 100, message = "Password must be between 12 and 100 characters")
            @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,100}$",
                    message = "Password must contain an upper-case letter, a lower-case letter, a digit and a symbol")
            String password,
            Role role) {
    }

    public record LoginRequest(
            @Email @NotBlank @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String password,
            /** Optional TOTP code, required only when the account has MFA enabled. */
            String totpCode) {
        public LoginRequest(String email, String password) {
            this(email, password, null);
        }
    }

    /**
     * Backwards-compatible response: the original fields (token, id, name,
     * email, role) keep their positions. New fields are appended:
     * refreshToken (null when MFA is still pending), mfaRequired (true when
     * the password was correct but a TOTP code is still needed — in that
     * case token is null and no session is created), mfaEnabled.
     */
    public record AuthResponse(String token, Long id, String name, String email, Role role,
                               String refreshToken, boolean mfaRequired, boolean mfaEnabled) {
        public AuthResponse(String token, Long id, String name, String email, Role role) {
            this(token, id, name, email, role, null, false, false);
        }
    }

    public record UserResponse(Long id, String name, String email, Role role) {
    }

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record MfaSetupResponse(String secret, String otpauthUrl, boolean mfaEnabled) {
    }

    public record MfaVerifyRequest(@NotBlank @Size(min = 6, max = 6) String code) {
    }

    public record AuditLogResponse(Long id, String actorEmail, String action, String entityType,
                                   Long entityId, String ipAddress, String userAgent,
                                   java.time.LocalDateTime createdAt) {
    }
}
