package com.healthrecords.mapper;

import com.healthrecords.dto.AuthDtos.UserResponse;
import com.healthrecords.dto.RecordDtos.LabReportResponse;
import com.healthrecords.dto.RecordDtos.MedicalRecordResponse;
import com.healthrecords.dto.RecordDtos.PrescriptionResponse;
import com.healthrecords.model.LabReport;
import com.healthrecords.model.MedicalRecord;
import com.healthrecords.model.Prescription;
import com.healthrecords.model.User;

/**
 * Single place for entity -> response mapping, so services no longer each
 * carry their own inline toResponse() copy. Field values are already
 * decrypted by the JPA converter before they reach this mapper.
 */
public final class RecordMapper {

    private RecordMapper() {
    }

    public static UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
    }

    public static MedicalRecordResponse toMedicalRecordResponse(MedicalRecord r) {
        return new MedicalRecordResponse(r.getId(), r.getTitle(), r.getDescription(), r.getDiagnosis(),
                r.getTreatment(), r.getRecordDate(),
                r.getPatient() != null ? r.getPatient().getId() : null,
                r.getPatient() != null ? r.getPatient().getName() : null,
                r.getDoctor() != null ? r.getDoctor().getId() : null,
                r.getDoctor() != null ? r.getDoctor().getName() : null,
                r.getCreatedAt(), r.getUpdatedAt());
    }

    public static PrescriptionResponse toPrescriptionResponse(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getMedication(), p.getDosage(), p.getInstructions(),
                p.getStartDate(), p.getEndDate(),
                p.getPatient() != null ? p.getPatient().getId() : null,
                p.getPatient() != null ? p.getPatient().getName() : null,
                p.getDoctor() != null ? p.getDoctor().getId() : null,
                p.getDoctor() != null ? p.getDoctor().getName() : null,
                p.getMedicalRecord() != null ? p.getMedicalRecord().getId() : null,
                p.getCreatedAt(), p.getUpdatedAt());
    }

    public static LabReportResponse toLabReportResponse(LabReport r) {
        return new LabReportResponse(r.getId(), r.getTitle(), r.getDescription(), r.getOriginalFileName(),
                r.getContentType(), r.getFileSize(),
                r.getPatient() != null ? r.getPatient().getId() : null,
                r.getPatient() != null ? r.getPatient().getName() : null,
                r.getMedicalRecord() != null ? r.getMedicalRecord().getId() : null,
                r.getUploadedAt());
    }
}
