package com.healthrecords.service;

import com.healthrecords.dto.AuthDtos.AuthResponse;
import com.healthrecords.dto.AuthDtos.LoginRequest;
import com.healthrecords.dto.AuthDtos.RegisterRequest;
import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import com.healthrecords.repository.UserRepository;
import com.healthrecords.security.JwtService;
import com.healthrecords.security.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock RefreshTokenService refreshTokenService;
    @Mock TotpService totpService;
    @Mock AuditService auditService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        // Explicit construction so lockout thresholds are real values, not
        // Mockito's default 0 for @Value-annotated int parameters.
        authService = new AuthService(userRepository, passwordEncoder, jwtService,
                authenticationManager, refreshTokenService, totpService, auditService, 5, 15);
    }

    @Test
    void registerHashesPasswordDefaultsToPatientAndReturnsToken() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(refreshTokenService.create(any(User.class))).thenReturn("refresh-token");

        AuthResponse response = authService.register(
                new RegisterRequest("Alice", "a@example.com", "Password123!", null));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.role()).isEqualTo(Role.PATIENT);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed");
        assertThat(captor.getValue().getEmail()).isEqualTo("a@example.com");
    }

    @Test
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Alice", "a@example.com", "weak", Role.DOCTOR)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerRejectsDuplicateEmailWithoutSaving() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Alice", "a@example.com", "Password123!", Role.DOCTOR)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginAuthenticatesAndReturnsToken() {
        User user = new User("Bob", "b@example.com", "hashed", Role.DOCTOR);
        user.setId(7L);
        when(userRepository.findByEmail("b@example.com")).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(refreshTokenService.create(user)).thenReturn("refresh-token");

        AuthResponse response = authService.login(new LoginRequest("b@example.com", "Password123!"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void failedLoginsLockAccountAfterFiveAttempts() {
        User user = new User("Bob", "b@example.com", "hashed", Role.PATIENT);
        user.setId(7L);
        when(userRepository.findByEmail("b@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("b@example.com", "Wrong123!")))
                    .isInstanceOf(BadCredentialsException.class);
        }
        assertThat(user.isLocked()).isTrue();

        // A sixth attempt is rejected as locked, without re-authenticating.
        assertThatThrownBy(() -> authService.login(new LoginRequest("b@example.com", "Password123!")))
                .isInstanceOf(LockedException.class);
        verify(authenticationManager, times(5)).authenticate(any());
    }

    @Test
    void loginWithMfaEnabledAndNoCodeReturnsMfaRequiredWithoutToken() {
        User user = new User("Bob", "b@example.com", "hashed", Role.PATIENT);
        user.setId(7L);
        user.setMfaEnabled(true);
        user.setMfaSecret("JBSWY3DPEHPK3PXP");
        when(userRepository.findByEmail("b@example.com")).thenReturn(Optional.of(user));
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        AuthResponse response = authService.login(new LoginRequest("b@example.com", "Password123!"));

        assertThat(response.mfaRequired()).isTrue();
        assertThat(response.token()).isNull();
        verify(jwtService, never()).generateToken(any());
    }
}
