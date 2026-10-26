package com.healthrecords.repository;

import com.healthrecords.model.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdOrderByRecordDateDescCreatedAtDesc(Long patientId);
    List<MedicalRecord> findAllByOrderByCreatedAtDesc();

    // Filtered (backwards-compatible list) queries.
    List<MedicalRecord> findByPatientIdAndTitleContainingIgnoreCaseOrderByRecordDateDescCreatedAtDesc(
            Long patientId, String title);
    List<MedicalRecord> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);

    // Paged queries — see GET /api/medical-records/paged.
    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);
    Page<MedicalRecord> findByPatientIdAndTitleContainingIgnoreCase(Long patientId, String title, Pageable pageable);
    Page<MedicalRecord> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
