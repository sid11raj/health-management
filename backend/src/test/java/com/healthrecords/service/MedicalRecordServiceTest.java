package com.healthrecords.service;

import com.healthrecords.dto.RecordDtos.MedicalRecordRequest;
import com.healthrecords.dto.RecordDtos.MedicalRecordResponse;
import com.healthrecords.model.MedicalRecord;
import com.healthrecords.model.Role;
import com.healthrecords.model.User;
import com.healthrecords.repository.MedicalRecordRepository;
import com.healthrecords.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock MedicalRecordRepository recordRepository;
    @Mock UserRepository userRepository;
    @Mock AccessService accessService;
    @Mock AuditService auditService;
    @InjectMocks MedicalRecordService service;

    private static User user(Long id, Role role) {
        User u = new User("Test", "test@example.com", "hashed", role);
        u.setId(id);
        return u;
    }

    @Test
    void patientListIsScopedToOwnRecords() {
        User patient = user(5L, Role.PATIENT);
        when(accessService.currentUser()).thenReturn(patient);
        when(accessService.isStaff()).thenReturn(false);
        MedicalRecord record = new MedicalRecord();
        record.setId(1L);
        record.setTitle("Check-up");
        record.setPatient(patient);
        when(recordRepository.findByPatientIdOrderByRecordDateDescCreatedAtDesc(5L))
                .thenReturn(List.of(record));

        List<MedicalRecordResponse> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Check-up");
        verify(recordRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void createSetsPatientAndCreatedBy() {
        User doctor = user(2L, Role.DOCTOR);
        User patient = user(5L, Role.PATIENT);
        when(accessService.currentUser()).thenReturn(doctor);
        when(userRepository.findById(5L)).thenReturn(Optional.of(patient));
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));
        when(recordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> {
            MedicalRecord r = inv.getArgument(0);
            r.setId(9L);
            return r;
        });

        MedicalRecordResponse response = service.create(new MedicalRecordRequest(
                "Annual check-up", "desc", "Healthy", "None",
                LocalDate.of(2026, 10, 26), 5L, 2L));

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.patientId()).isEqualTo(5L);
        verify(accessService).requireAccessToPatient(5L);
    }
}
