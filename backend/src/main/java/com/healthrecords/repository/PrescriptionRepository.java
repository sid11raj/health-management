package com.healthrecords.repository;

import com.healthrecords.model.Prescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Prescription> findAllByOrderByCreatedAtDesc();

    List<Prescription> findByPatientIdAndMedicationContainingIgnoreCaseOrderByCreatedAtDesc(
            Long patientId, String medication);
    List<Prescription> findByMedicationContainingIgnoreCaseOrderByCreatedAtDesc(String medication);

    Page<Prescription> findByPatientId(Long patientId, Pageable pageable);
    Page<Prescription> findByPatientIdAndMedicationContainingIgnoreCase(Long patientId, String medication, Pageable pageable);
    Page<Prescription> findByMedicationContainingIgnoreCase(String medication, Pageable pageable);
}
