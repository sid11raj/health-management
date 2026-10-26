package com.healthrecords.service;

import com.healthrecords.dto.RecordDtos.PrescriptionRequest;
import com.healthrecords.dto.RecordDtos.PrescriptionResponse;
import com.healthrecords.mapper.RecordMapper;
import com.healthrecords.exception.ResourceNotFoundException;
import com.healthrecords.model.MedicalRecord;
import com.healthrecords.model.Prescription;
import com.healthrecords.model.User;
import com.healthrecords.repository.MedicalRecordRepository;
import com.healthrecords.repository.PrescriptionRepository;
import com.healthrecords.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final MedicalRecordRepository recordRepository;
    private final AccessService accessService;
    private final EmailService emailService;
    private final AuditService auditService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, UserRepository userRepository,
                               MedicalRecordRepository recordRepository, AccessService accessService,
                               EmailService emailService, AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.accessService = accessService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public List<PrescriptionResponse> list() {
        return listFiltered(null, null);
    }

    /** Backwards-compatible filtered list (plain array, as the frontend expects). */
    public List<PrescriptionResponse> listFiltered(String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        List<Prescription> items;
        if (effectivePatientId != null) {
            items = hasSearch
                    ? prescriptionRepository.findByPatientIdAndMedicationContainingIgnoreCaseOrderByCreatedAtDesc(
                            effectivePatientId, search.trim())
                    : prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(effectivePatientId);
        } else {
            items = hasSearch
                    ? prescriptionRepository.findByMedicationContainingIgnoreCaseOrderByCreatedAtDesc(search.trim())
                    : prescriptionRepository.findAllByOrderByCreatedAtDesc();
        }
        return items.stream().map(RecordMapper::toPrescriptionResponse).toList();
    }

    /** Paged variant: GET /api/prescriptions/paged?page=0&size=20&sort=createdAt,desc */
    public Page<PrescriptionResponse> listPaged(Pageable pageable, String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        Page<Prescription> page;
        if (effectivePatientId != null) {
            page = hasSearch
                    ? prescriptionRepository.findByPatientIdAndMedicationContainingIgnoreCase(
                            effectivePatientId, search.trim(), pageable)
                    : prescriptionRepository.findByPatientId(effectivePatientId, pageable);
        } else {
            page = hasSearch
                    ? prescriptionRepository.findByMedicationContainingIgnoreCase(search.trim(), pageable)
                    : prescriptionRepository.findAll(pageable);
        }
        return page.map(RecordMapper::toPrescriptionResponse);
    }

    public PrescriptionResponse get(Long id) {
        Prescription p = find(id);
        accessService.requireAccessToPatient(p.getPatient().getId());
        return RecordMapper.toPrescriptionResponse(p);
    }

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request) {
        accessService.requireAccessToPatient(request.patientId());
        Prescription p = new Prescription();
        p.setCreatedBy(accessService.currentUser().getEmail());
        apply(request, p);
        Prescription saved = prescriptionRepository.save(p);
        audit("PRESCRIPTION_CREATE", saved.getId());
        emailService.sendNotification(saved.getPatient().getEmail(),
                "New prescription: " + saved.getMedication(),
                "A new prescription for " + saved.getMedication()
                        + (saved.getDosage() != null ? " (" + saved.getDosage() + ")" : "")
                        + " has been added to your health records.");
        return RecordMapper.toPrescriptionResponse(saved);
    }

    @Transactional
    public PrescriptionResponse update(Long id, PrescriptionRequest request) {
        Prescription p = find(id);
        accessService.requireAccessToPatient(p.getPatient().getId());
        apply(request, p);
        Prescription saved = prescriptionRepository.save(p);
        audit("PRESCRIPTION_UPDATE", saved.getId());
        return RecordMapper.toPrescriptionResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Prescription p = find(id);
        accessService.requireAccessToPatient(p.getPatient().getId());
        prescriptionRepository.delete(p);
        audit("PRESCRIPTION_DELETE", id);
    }

    private Prescription find(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    }

    private void apply(PrescriptionRequest request, Prescription p) {
        if (request.startDate() != null && request.endDate() != null
                && request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Prescription end date cannot be before start date.");
        }
        User patient = userRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        p.setPatient(patient);
        p.setDoctor(request.doctorId() == null ? null : userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId())));
        MedicalRecord record = null;
        if (request.medicalRecordId() != null) {
            record = recordRepository.findById(request.medicalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medical record not found: " + request.medicalRecordId()));
        }
        p.setMedicalRecord(record);
        p.setMedication(request.medication());
        p.setDosage(request.dosage());
        p.setInstructions(request.instructions());
        p.setStartDate(request.startDate());
        p.setEndDate(request.endDate());
    }

    private void audit(String action, Long entityId) {
        if (auditService != null) {
            auditService.record(action, "Prescription", entityId);
        }
    }
}
