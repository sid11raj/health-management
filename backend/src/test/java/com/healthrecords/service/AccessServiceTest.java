package com.healthrecords.service;

import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessServiceTest {

    private final AccessService accessService = new AccessService();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private static User user(Long id, Role role) {
        User u = new User("Test", "test@example.com", "hashed", role);
        u.setId(id);
        return u;
    }

    @Test
    void staffCanAccessAnyPatient() {
        authenticate(user(1L, Role.DOCTOR));
        accessService.requireAccessToPatient(99L); // must not throw
        assertThat(accessService.isStaff()).isTrue();
    }

    @Test
    void patientCanAccessOnlyOwnRecords() {
        authenticate(user(5L, Role.PATIENT));
        accessService.requireAccessToPatient(5L); // must not throw
        assertThat(accessService.isStaff()).isFalse();
        assertThatThrownBy(() -> accessService.requireAccessToPatient(6L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void unauthenticatedAccessIsDenied() {
        assertThatThrownBy(() -> accessService.currentUser())
                .isInstanceOf(Exception.class);
    }
}
