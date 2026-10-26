package com.healthrecords.controller;

import com.healthrecords.dto.AuthDtos.AuthResponse;
import com.healthrecords.dto.AuthDtos.LoginRequest;
import com.healthrecords.dto.AuthDtos.MfaSetupResponse;
import com.healthrecords.dto.AuthDtos.MfaVerifyRequest;
import com.healthrecords.dto.AuthDtos.RefreshRequest;
import com.healthrecords.dto.AuthDtos.RegisterRequest;
import com.healthrecords.model.User;
import com.healthrecords.service.AccessService;
import com.healthrecords.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
// CORS is handled centrally in SecurityConfig from CORS_ORIGINS (env),
// not hardcoded per-controller, so the allowed origin can change safely.
// NOTE: /api/auth/mfa/** requires authentication (see SecurityConfig);
// login/register/refresh/logout are the only public auth endpoints.
public class AuthController {

    private final AuthService authService;
    private final AccessService accessService;

    public AuthController(AuthService authService, AccessService accessService) {
        this.authService = authService;
        this.accessService = accessService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Rotates the refresh token: the presented token is revoked, a new one is issued. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /**
     * Logs out by revoking the supplied refresh token. With no token, and an
     * authenticated caller, revokes all of that user's refresh tokens.
     * Access tokens are short-lived JWTs and cannot be individually revoked;
     * the frontend also clears its stored tokens on logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        User current = null;
        try {
            current = accessService.currentUser();
        } catch (Exception ignored) {
            // Unauthenticated logout (token-only) is allowed.
        }
        authService.logout(request != null ? request.refreshToken() : null, current);
        return ResponseEntity.noContent().build();
    }

    /** Starts optional TOTP MFA setup. Returns the secret once; verify it to enable MFA. */
    @PostMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa() {
        return ResponseEntity.ok(authService.setupMfa(accessService.currentUser()));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<Void> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        authService.verifyMfa(accessService.currentUser(), request.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Void> disableMfa(@Valid @RequestBody MfaVerifyRequest request) {
        authService.disableMfa(accessService.currentUser(), request.code());
        return ResponseEntity.noContent().build();
    }
}
