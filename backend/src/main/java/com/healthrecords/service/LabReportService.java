package com.healthrecords.service;

import com.healthrecords.dto.RecordDtos.LabReportResponse;
import com.healthrecords.mapper.RecordMapper;
import com.healthrecords.exception.ResourceNotFoundException;
import com.healthrecords.model.LabReport;
import com.healthrecords.model.MedicalRecord;
import com.healthrecords.model.User;
import com.healthrecords.repository.LabReportRepository;
import com.healthrecords.repository.MedicalRecordRepository;
import com.healthrecords.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class LabReportService {

    private final LabReportRepository reportRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository recordRepository;
    private final AccessService accessService;
    private final EmailService emailService;
    private final FileUploadValidator fileUploadValidator;
    private final AuditService auditService;
    private final Path uploadDir;

    public LabReportService(LabReportRepository reportRepository, UserRepository userRepository,
                            MedicalRecordRepository recordRepository, AccessService accessService,
                            EmailService emailService, FileUploadValidator fileUploadValidator,
                            AuditService auditService,
                            @Value("${app.upload.dir:uploads}") String uploadDir) throws IOException {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.accessService = accessService;
        this.emailService = emailService;
        this.fileUploadValidator = fileUploadValidator;
        this.auditService = auditService;
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public List<LabReportResponse> list() {
        return listFiltered(null, null);
    }

    /** Backwards-compatible filtered list (plain array, as the frontend expects). */
    public List<LabReportResponse> listFiltered(String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        List<LabReport> reports;
        if (effectivePatientId != null) {
            reports = hasSearch
                    ? reportRepository.findByPatientIdAndTitleContainingIgnoreCaseOrderByUploadedAtDesc(
                            effectivePatientId, search.trim())
                    : reportRepository.findByPatientIdOrderByUploadedAtDesc(effectivePatientId);
        } else {
            reports = hasSearch
                    ? reportRepository.findByTitleContainingIgnoreCaseOrderByUploadedAtDesc(search.trim())
                    : reportRepository.findAllByOrderByUploadedAtDesc();
        }
        return reports.stream().map(RecordMapper::toLabReportResponse).toList();
    }

    /** Paged variant: GET /api/lab-reports/paged?page=0&size=20&sort=uploadedAt,desc */
    public Page<LabReportResponse> listPaged(Pageable pageable, String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        Page<LabReport> page;
        if (effectivePatientId != null) {
            page = hasSearch
                    ? reportRepository.findByPatientIdAndTitleContainingIgnoreCase(
                            effectivePatientId, search.trim(), pageable)
                    : reportRepository.findByPatientId(effectivePatientId, pageable);
        } else {
            page = hasSearch
                    ? reportRepository.findByTitleContainingIgnoreCase(search.trim(), pageable)
                    : reportRepository.findAll(pageable);
        }
        return page.map(RecordMapper::toLabReportResponse);
    }

    public LabReportResponse get(Long id) {
        LabReport report = find(id);
        accessService.requireAccessToPatient(report.getPatient().getId());
        return RecordMapper.toLabReportResponse(report);
    }

    @Transactional
    public LabReportResponse upload(String title, String description, Long patientId,
                                    Long medicalRecordId, MultipartFile file) throws IOException {
        String original = fileUploadValidator.validateAndCleanFilename(file);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required for a lab report.");
        }
        accessService.requireAccessToPatient(patientId);
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
        MedicalRecord record = null;
        if (medicalRecordId != null) {
            record = recordRepository.findById(medicalRecordId)
                    .orElseThrow(() -> new ResourceNotFoundException("Medical record not found: " + medicalRecordId));
        }
        String stored = UUID.randomUUID() + "_" + original;
        Path target = uploadDir.resolve(stored).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IllegalArgumentException("Invalid file name.");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        LabReport report = new LabReport();
        report.setTitle(title);
        report.setDescription(description);
        report.setPatient(patient);
        report.setUploadedBy(accessService.currentUser());
        report.setMedicalRecord(record);
        report.setFileName(stored);
        report.setOriginalFileName(original);
        report.setFilePath(target.toString());
        report.setContentType(file.getContentType());
        report.setFileSize(file.getSize());
        LabReport saved = reportRepository.save(report);
        audit("LAB_REPORT_UPLOAD", saved.getId());
        emailService.sendNotification(patient.getEmail(),
                "New lab report uploaded: " + title,
                "A new lab report titled '" + title + "' has been uploaded to your health records.");
        return RecordMapper.toLabReportResponse(saved);
    }

    public Resource download(Long id) throws MalformedURLException {
        LabReport report = find(id);
        accessService.requireAccessToPatient(report.getPatient().getId());
        audit("LAB_REPORT_DOWNLOAD", id);
        Path file = uploadDir.resolve(report.getFileName()).normalize();
        if (!file.startsWith(uploadDir)) {
            throw new ResourceNotFoundException("Lab report file not found on disk: " + id);
        }
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("Lab report file not found on disk: " + id);
        }
        return resource;
    }

    public LabReport findForDownload(Long id) {
        LabReport report = find(id);
        accessService.requireAccessToPatient(report.getPatient().getId());
        return report;
    }

    @Transactional
    public void delete(Long id) throws IOException {
        LabReport report = find(id);
        accessService.requireAccessToPatient(report.getPatient().getId());
        reportRepository.delete(report);
        audit("LAB_REPORT_DELETE", id);
        Path fileToDelete = uploadDir.resolve(report.getFileName()).normalize();
        if (fileToDelete.startsWith(uploadDir)) {
            Files.deleteIfExists(fileToDelete);
        }
    }

    private LabReport find(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab report not found: " + id));
    }

    private void audit(String action, Long entityId) {
        if (auditService != null) {
            auditService.record(action, "LabReport", entityId);
        }
    }
}
