export type Role = 'ADMIN' | 'DOCTOR' | 'PATIENT';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}

export interface MedicalRecord {
  id: number;
  title: string;
  description?: string;
  diagnosis?: string;
  treatment?: string;
  recordDate?: string;
  patientId: number;
  patientName?: string;
  doctorId?: number;
  doctorName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface LabReport {
  id: number;
  title: string;
  description?: string;
  originalFileName: string;
  contentType?: string;
  fileSize: number;
  patientId: number;
  patientName?: string;
  medicalRecordId?: number;
  uploadedAt?: string;
}

export interface AuthResponse {
  token: string | null;
  refreshToken: string | null;
  id: number | null;
  name: string | null;
  email: string | null;
  role: Role | null;
  mfaRequired: boolean;
  mfaEnabled: boolean;
}

export interface AuditLog {
  id: number;
  actorEmail: string;
  action: string;
  entityType: string;
  entityId: number | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}

/** Spring Data Page<T> shape returned by /paged endpoints. */
export interface Paged<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface Prescription {
  id: number;
  medication: string;
  dosage?: string;
  instructions?: string;
  startDate?: string;
  endDate?: string;
  patientId: number;
  patientName?: string;
  doctorId?: number;
  doctorName?: string;
  medicalRecordId?: number;
  createdAt?: string;
  updatedAt?: string;
}
