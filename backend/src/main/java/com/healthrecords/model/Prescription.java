package com.healthrecords.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescriptions_patient", columnList = "patient_id"),
        @Index(name = "idx_prescriptions_doctor", columnList = "doctor_id"),
        @Index(name = "idx_prescriptions_medical_record", columnList = "medical_record_id")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String medication;

    private String dosage;

    /** Encrypted at rest (AES-GCM) when ENCRYPTION_KEY is set; see EncryptedStringConverter. */
    @Convert(converter = com.healthrecords.security.EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String instructions;

    private LocalDate startDate;

    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_record_id")
    private MedicalRecord medicalRecord;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Email of the authenticated user who created this prescription (audit field). */
    @Column(name = "created_by")
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public MedicalRecord getMedicalRecord() { return medicalRecord; }
    public void setMedicalRecord(MedicalRecord medicalRecord) { this.medicalRecord = medicalRecord; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
