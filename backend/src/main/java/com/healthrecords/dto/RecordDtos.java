package com.healthrecords.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RecordDtos {

    public record MedicalRecordRequest(
            @NotBlank @Size(min = 3, max = 200) String title,
            @Size(max = 5000) String description,
            @Size(max = 500) String diagnosis,
            @Size(max = 5000) String treatment,
            @PastOrPresent(message = "Record date cannot be in the future") LocalDate recordDate,
            @NotNull Long patientId,
            Long doctorId) {
    }

    public record MedicalRecordResponse(
            Long id, String title, String description, String diagnosis, String treatment,
            LocalDate recordDate, Long patientId, String patientName,
            Long doctorId, String doctorName, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record LabReportResponse(
            Long id, String title, String description, String originalFileName,
            String contentType, long fileSize, Long patientId, String patientName,
            Long medicalRecordId, LocalDateTime uploadedAt) {
    }

    public record PrescriptionRequest(
            @NotBlank @Size(min = 2, max = 200) String medication,
            @Size(max = 200) String dosage,
            @Size(max = 5000) String instructions,
            LocalDate startDate,
            LocalDate endDate,
            @NotNull Long patientId,
            Long doctorId,
            Long medicalRecordId) {
    }

    public record PrescriptionResponse(
            Long id, String medication, String dosage, String instructions,
            LocalDate startDate, LocalDate endDate, Long patientId, String patientName,
            Long doctorId, String doctorName, Long medicalRecordId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
    }
}
