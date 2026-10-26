package com.healthrecords.repository;

import com.healthrecords.model.LabReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabReportRepository extends JpaRepository<LabReport, Long> {
    List<LabReport> findByPatientIdOrderByUploadedAtDesc(Long patientId);
    List<LabReport> findAllByOrderByUploadedAtDesc();

    List<LabReport> findByPatientIdAndTitleContainingIgnoreCaseOrderByUploadedAtDesc(Long patientId, String title);
    List<LabReport> findByTitleContainingIgnoreCaseOrderByUploadedAtDesc(String title);

    Page<LabReport> findByPatientId(Long patientId, Pageable pageable);
    Page<LabReport> findByPatientIdAndTitleContainingIgnoreCase(Long patientId, String title, Pageable pageable);
    Page<LabReport> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
