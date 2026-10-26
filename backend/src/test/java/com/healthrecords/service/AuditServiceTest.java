package com.healthrecords.service;

import com.healthrecords.model.AuditLog;
import com.healthrecords.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock AuditLogRepository repository;

    @Test
    void recordWritesActionEntityAndActorWithoutHealthData() {
        AuditService auditService = new AuditService(repository);
        auditService.recordForActor("doctor@example.com", "RECORD_CREATE", "MedicalRecord", 42L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        AuditLog log = captor.getValue();
        assertThat(log.getActorEmail()).isEqualTo("doctor@example.com");
        assertThat(log.getAction()).isEqualTo("RECORD_CREATE");
        assertThat(log.getEntityType()).isEqualTo("MedicalRecord");
        assertThat(log.getEntityId()).isEqualTo(42L);
    }

    @Test
    void auditFailureDoesNotThrow() {
        when(repository.save(any())).thenThrow(new RuntimeException("db down"));
        AuditService auditService = new AuditService(repository);
        // Must not propagate: audit is best-effort by design.
        auditService.recordForActor("a@example.com", "LOGIN", "User", 1L);
        verify(repository).save(any());
    }
}
