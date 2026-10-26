package com.healthrecords.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_records", indexes = {
        @Index(name = "idx_medical_records_patient", columnList = "patient_id"),
        @Index(name = "idx_medical_records_doctor", columnList = "doctor_id"),
        @Index(name = "idx_medical_records_record_date", columnList = "record_date")
})
public class MedicalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Encrypted at rest (AES-GCM) when ENCRYPTION_KEY is set; see EncryptedStringConverter. */
    @Convert(converter = com.healthrecords.security.EncryptedStringConverter.class)
    private String diagnosis;

    @Convert(converter = com.healthrecords.security.EncryptedStringConverter.class)
    @Column(columnDefinition = "TEXT")
    private String treatment;

    private LocalDate recordDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Email of the authenticated user who created this record (audit field). */
    @Column(name = "created_by")
    private String createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getTreatment() { return treatment; }
    public void setTreatment(String treatment) { this.treatment = treatment; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public User getPatient() { return patient; }
    public void setPatient(User patient) { this.patient = patient; }
    public User getDoctor() { return doctor; }
    public void setDoctor(User doctor) { this.doctor = doctor; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
