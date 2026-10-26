package com.healthrecords.service;

import com.healthrecords.model.RefreshToken;
import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import com.healthrecords.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository repository;

    private RefreshTokenService service() {
        return new RefreshTokenService(repository, 604800000L);
    }

    private static User user() {
        User u = new User("Test", "t@example.com", "hashed", Role.PATIENT);
        u.setId(3L);
        return u;
    }

    private static RefreshToken token(User user, String raw, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken t = new RefreshToken();
        t.setTokenHash(RefreshTokenService.hash(raw));
        t.setUser(user);
        t.setRevoked(revoked);
        t.setExpiresAt(expiresAt);
        return t;
    }

    @Test
    void createStoresOnlyHashNeverRawToken() {
        RefreshTokenService service = service();
        String raw = service.create(user());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw));
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(raw);
    }

    @Test
    void rotateRevokesOldTokenAndIssuesNewOne() {
        User u = user();
        RefreshToken old = token(u, "old-raw-token", false, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("old-raw-token")))
                .thenReturn(Optional.of(old));

        String rotated = service().rotate("old-raw-token");

        assertThat(rotated).isNotBlank().isNotEqualTo("old-raw-token");
        assertThat(old.isRevoked()).isTrue();
    }

    @Test
    void revokedTokenCannotBeRotatedAndRevokesAllUserTokens() {
        User u = user();
        RefreshToken old = token(u, "old-raw-token", true, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("old-raw-token")))
                .thenReturn(Optional.of(old));
        when(repository.findByUser(u)).thenReturn(java.util.List.of(old));

        assertThatThrownBy(() -> service().rotate("old-raw-token"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository).findByUser(u);
    }

    @Test
    void expiredTokenIsRejected() {
        User u = user();
        RefreshToken old = token(u, "old-raw-token", false, LocalDateTime.now().minusHours(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("old-raw-token")))
                .thenReturn(Optional.of(old));

        assertThatThrownBy(() -> service().rotate("old-raw-token"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(old.isRevoked()).isTrue();
    }

    @Test
    void revokeMarksTokenRevoked() {
        User u = user();
        RefreshToken old = token(u, "raw", false, LocalDateTime.now().plusDays(1));
        when(repository.findByTokenHash(RefreshTokenService.hash("raw"))).thenReturn(Optional.of(old));
        service().revoke("raw");
        assertThat(old.isRevoked()).isTrue();
        verify(repository).save(old);
    }
}
