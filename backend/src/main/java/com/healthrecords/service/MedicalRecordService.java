package com.healthrecords.service;

import com.healthrecords.dto.RecordDtos.MedicalRecordRequest;
import com.healthrecords.dto.RecordDtos.MedicalRecordResponse;
import com.healthrecords.mapper.RecordMapper;
import com.healthrecords.exception.ResourceNotFoundException;
import com.healthrecords.model.MedicalRecord;
import com.healthrecords.model.User;
import com.healthrecords.repository.MedicalRecordRepository;
import com.healthrecords.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;
    private final AuditService auditService;

    public MedicalRecordService(MedicalRecordRepository recordRepository,
                                UserRepository userRepository, AccessService accessService,
                                AuditService auditService) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.auditService = auditService;
    }

    public List<MedicalRecordResponse> list() {
        return listFiltered(null, null);
    }

    /**
     * Backwards-compatible filtered list: still returns a plain JSON array,
     * exactly as the React frontend expects. A patient who asks for another
     * patient's id is denied by AccessService; staff may filter by patient.
     */
    public List<MedicalRecordResponse> listFiltered(String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        List<MedicalRecord> records;
        if (effectivePatientId != null) {
            records = hasSearch
                    ? recordRepository.findByPatientIdAndTitleContainingIgnoreCaseOrderByRecordDateDescCreatedAtDesc(
                            effectivePatientId, search.trim())
                    : recordRepository.findByPatientIdOrderByRecordDateDescCreatedAtDesc(effectivePatientId);
        } else {
            records = hasSearch
                    ? recordRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(search.trim())
                    : recordRepository.findAllByOrderByCreatedAtDesc();
        }
        return records.stream().map(RecordMapper::toMedicalRecordResponse).toList();
    }

    /** Paged variant: GET /api/medical-records/paged?page=0&size=20&sort=createdAt,desc */
    public Page<MedicalRecordResponse> listPaged(Pageable pageable, String search, Long patientId) {
        User current = accessService.currentUser();
        Long effectivePatientId = accessService.isStaff() ? patientId : current.getId();
        if (!accessService.isStaff() && patientId != null) {
            accessService.requireAccessToPatient(patientId);
        }
        boolean hasSearch = search != null && !search.isBlank();
        Page<MedicalRecord> page;
        if (effectivePatientId != null) {
            page = hasSearch
                    ? recordRepository.findByPatientIdAndTitleContainingIgnoreCase(effectivePatientId, search.trim(), pageable)
                    : recordRepository.findByPatientId(effectivePatientId, pageable);
        } else {
            page = hasSearch
                    ? recordRepository.findByTitleContainingIgnoreCase(search.trim(), pageable)
                    : recordRepository.findAll(pageable);
        }
        return page.map(RecordMapper::toMedicalRecordResponse);
    }

    public MedicalRecordResponse get(Long id) {
        MedicalRecord record = find(id);
        accessService.requireAccessToPatient(record.getPatient().getId());
        return RecordMapper.toMedicalRecordResponse(record);
    }

    @Transactional
    public MedicalRecordResponse create(MedicalRecordRequest request) {
        accessService.requireAccessToPatient(request.patientId());
        MedicalRecord record = new MedicalRecord();
        record.setCreatedBy(accessService.currentUser().getEmail());
        apply(request, record);
        MedicalRecord saved = recordRepository.save(record);
        audit("RECORD_CREATE", saved.getId());
        return RecordMapper.toMedicalRecordResponse(saved);
    }

    @Transactional
    public MedicalRecordResponse update(Long id, MedicalRecordRequest request) {
        MedicalRecord record = find(id);
        accessService.requireAccessToPatient(record.getPatient().getId());
        if (!record.getPatient().getId().equals(request.patientId())) {
            accessService.requireAccessToPatient(request.patientId());
        }
        apply(request, record);
        MedicalRecord saved = recordRepository.save(record);
        audit("RECORD_UPDATE", saved.getId());
        return RecordMapper.toMedicalRecordResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        MedicalRecord record = find(id);
        accessService.requireAccessToPatient(record.getPatient().getId());
        recordRepository.delete(record);
        audit("RECORD_DELETE", id);
    }

    private MedicalRecord find(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found: " + id));
    }

    private void apply(MedicalRecordRequest request, MedicalRecord record) {
        User patient = userRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.patientId()));
        record.setPatient(patient);
        record.setDoctor(request.doctorId() == null ? null : userRepository.findById(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.doctorId())));
        record.setTitle(request.title());
        record.setDescription(request.description());
        record.setDiagnosis(request.diagnosis());
        record.setTreatment(request.treatment());
        record.setRecordDate(request.recordDate());
    }

    private void audit(String action, Long entityId) {
        if (auditService != null) {
            auditService.record(action, "MedicalRecord", entityId);
        }
    }
}
