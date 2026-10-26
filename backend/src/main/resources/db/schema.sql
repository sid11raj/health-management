-- ============================================================================
-- Health Records Management System — schema reference (MySQL 8)
-- ============================================================================
-- STATUS: REFERENCE ONLY. This file is NOT executed by the application.
-- application.properties sets spring.sql.init.mode=never, and the current
-- schema mechanism is JPA ddl-auto=update (see JPA_DDL_AUTO).
--
-- Purpose: a reviewed, version-controlled record of the tables and indexes
-- the JPA entities are expected to produce, and the starting point for a
-- real Flyway migration (e.g. db/migration/V1__initial_schema.sql) when
-- ddl-auto is retired. Any production schema change must be a reviewed
-- migration, never a silent ddl-auto change.
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash only, never plain text
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    INDEX idx_users_role (role)
);

CREATE TABLE IF NOT EXISTS medical_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    diagnosis VARCHAR(255) NULL,
    treatment TEXT NULL,
    record_date DATE NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_medical_records_doctor FOREIGN KEY (doctor_id) REFERENCES users (id),
    INDEX idx_medical_records_patient (patient_id),
    INDEX idx_medical_records_doctor (doctor_id),
    INDEX idx_medical_records_record_date (record_date)
);

CREATE TABLE IF NOT EXISTS lab_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    content_type VARCHAR(255) NULL,
    file_size BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    uploaded_by_id BIGINT NULL,
    medical_record_id BIGINT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT fk_lab_reports_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_lab_reports_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users (id),
    CONSTRAINT fk_lab_reports_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_records (id),
    INDEX idx_lab_reports_patient (patient_id),
    INDEX idx_lab_reports_uploaded_by (uploaded_by_id),
    INDEX idx_lab_reports_medical_record (medical_record_id)
);

CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medication VARCHAR(255) NOT NULL,
    dosage VARCHAR(255) NULL,
    instructions TEXT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    medical_record_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id) REFERENCES users (id),
    CONSTRAINT fk_prescriptions_medical_record FOREIGN KEY (medical_record_id) REFERENCES medical_records (id),
    INDEX idx_prescriptions_patient (patient_id),
    INDEX idx_prescriptions_doctor (doctor_id),
    INDEX idx_prescriptions_medical_record (medical_record_id)
);
