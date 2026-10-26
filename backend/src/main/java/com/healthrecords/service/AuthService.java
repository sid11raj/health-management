package com.healthrecords.service;

import com.healthrecords.dto.AuthDtos.AuthResponse;
import com.healthrecords.dto.AuthDtos.LoginRequest;
import com.healthrecords.dto.AuthDtos.MfaSetupResponse;
import com.healthrecords.dto.AuthDtos.RegisterRequest;
import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import com.healthrecords.repository.UserRepository;
import com.healthrecords.security.JwtService;
import com.healthrecords.security.PasswordPolicy;
import com.healthrecords.security.TotpService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Authentication, with the security features added in the cybersecurity
 * upgrade. Access tokens are short-lived (15 min by default); refresh
 * tokens are rotated and revocable. Failed logins lock the account
 * temporarily. MFA is optional per user, never mandatory for all accounts.
 *
 * Note on access-token revocation: access tokens are stateless JWTs and
 * cannot be individually revoked before expiry — the mitigation is the
 * short 15-minute lifetime plus refresh-token revocation on logout.
 * That limitation is stated honestly in README/SECURITY.md.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;
    private final AuditService auditService;
    private final int maxFailedAttempts;
    private final int lockoutMinutes;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService, TotpService totpService,
                       AuditService auditService,
                       @Value("${app.lockout.max-attempts:5}") int maxFailedAttempts,
                       @Value("${app.lockout.duration-minutes:15}") int lockoutMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
        this.auditService = auditService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!PasswordPolicy.isValid(request.password())) {
            throw new IllegalArgumentException(PasswordPolicy.requirementMessage());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("An account already exists for this email.");
        }
        Role role = request.role() != null ? request.role() : Role.PATIENT;
        User user = new User(request.name(), request.email(),
                passwordEncoder.encode(request.password()), role);
        userRepository.save(user);
        auditService.recordForActor(user.getEmail(), "REGISTER", "User", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);
        if (user != null && user.isLocked()) {
            auditService.recordForActor(request.email(), "LOGIN_FAILURE_LOCKED", "User", user.getId());
            throw new LockedException("Account is temporarily locked after too many failed login attempts. "
                    + "Please try again later.");
        }
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            recordFailedLogin(user, request.email());
            throw ex;
        }
        User authenticated = (User) auth.getPrincipal();
        // Password was correct: clear any previous failure counter.
        if (authenticated.getFailedLoginAttempts() != 0 || authenticated.getLockedUntil() != null) {
            authenticated.setFailedLoginAttempts(0);
            authenticated.setLockedUntil(null);
            userRepository.save(authenticated);
        }
        if (authenticated.isMfaEnabled()) {
            if (request.totpCode() == null || request.totpCode().isBlank()) {
                // No session is created yet: token stays null.
                return new AuthResponse(null, authenticated.getId(), authenticated.getName(),
                        authenticated.getEmail(), authenticated.getRole(), null, true, true);
            }
            if (!totpService.verify(authenticated.getMfaSecret(), request.totpCode().trim())) {
                recordFailedLogin(authenticated, request.email());
                throw new BadCredentialsException("Invalid email, password or MFA code.");
            }
        }
        auditService.recordForActor(authenticated.getEmail(), "LOGIN", "User", authenticated.getId());
        return issueTokens(authenticated);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String newRaw = refreshTokenService.rotate(rawRefreshToken);
        User user = refreshTokenService.find(newRaw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."))
                .getUser();
        auditService.recordForActor(user.getEmail(), "TOKEN_REFRESH", "User", user.getId());
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(),
                user.getEmail(), user.getRole(), newRaw, false, user.isMfaEnabled());
    }

    @Transactional
    public void logout(String rawRefreshToken, User currentUser) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
            if (currentUser != null) {
                auditService.recordForActor(currentUser.getEmail(), "LOGOUT", "User", currentUser.getId());
            }
        } else if (currentUser != null) {
            refreshTokenService.revokeAll(currentUser);
            auditService.recordForActor(currentUser.getEmail(), "LOGOUT_ALL", "User", currentUser.getId());
        }
    }

    @Transactional
    public MfaSetupResponse setupMfa(User user) {
        if (user.isMfaEnabled()) {
            throw new IllegalArgumentException("MFA is already enabled for this account.");
        }
        // Generates (or replaces) a pending secret. It is shown once by the
        // frontend at setup; it is never returned again after verification.
        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);
        return new MfaSetupResponse(secret,
                totpService.otpauthUrl("Health Records", user.getEmail(), secret), false);
    }

    @Transactional
    public void verifyMfa(User user, String code) {
        if (user.getMfaSecret() == null || !totpService.verify(user.getMfaSecret(), code)) {
            throw new BadCredentialsException("Invalid MFA code.");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.recordForActor(user.getEmail(), "MFA_ENABLED", "User", user.getId());
    }

    @Transactional
    public void disableMfa(User user, String code) {
        if (!user.isMfaEnabled() || user.getMfaSecret() == null
                || !totpService.verify(user.getMfaSecret(), code)) {
            throw new BadCredentialsException("Invalid MFA code.");
        }
        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        auditService.recordForActor(user.getEmail(), "MFA_DISABLED", "User", user.getId());
    }

    private AuthResponse issueTokens(User user) {
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(),
                user.getEmail(), user.getRole(), refreshTokenService.create(user),
                false, user.isMfaEnabled());
    }

    private void recordFailedLogin(User user, String email) {
        if (user == null) {
            auditService.recordForActor(email, "LOGIN_FAILURE", "User", null);
            return;
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutMinutes));
        }
        userRepository.save(user);
        auditService.recordForActor(email, "LOGIN_FAILURE", "User", user.getId());
    }
}
