import axios from 'axios';
import type { LabReport, MedicalRecord, Prescription, User } from '../types';
import type { AuditLog, AuthResponse, Paged } from '../types';

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('hrms_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Refresh-on-401: access tokens live only 15 minutes. On a 401, try the
// stored refresh token once, then retry the original request with the new
// access token. If refresh fails (revoked/expired), clear both tokens and
// send the user back to /login. Auth requests and the refresh call itself
// are never retried, to avoid an infinite loop.
let refreshPromise: Promise<AuthResponse> | null = null;

async function doRefresh(): Promise<AuthResponse> {
  if (!refreshPromise) {
    const refreshToken = localStorage.getItem('hrms_refresh_token');
    refreshPromise = api
      .post('/auth/refresh', { refreshToken })
      .then((res) => res.data as AuthResponse)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config as (typeof error.config & { _retried?: boolean }) | undefined;
    const status = error.response?.status;
    const url: string = original?.url ?? '';
    const isAuthRequest = url.startsWith('/auth/');
    if (status === 401 && original && !original._retried && !isAuthRequest) {
      const refreshToken = localStorage.getItem('hrms_refresh_token');
      if (refreshToken) {
        try {
          const data = await doRefresh();
          if (data.token) {
            localStorage.setItem('hrms_token', data.token);
            if (data.refreshToken) {
              localStorage.setItem('hrms_refresh_token', data.refreshToken);
            }
            original._retried = true;
            original.headers.Authorization = `Bearer ${data.token}`;
            return api(original);
          }
        } catch {
          // Refresh failed: fall through to clean logout below.
        }
      }
      localStorage.removeItem('hrms_token');
      localStorage.removeItem('hrms_refresh_token');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export async function login(email: string, password: string, totpCode?: string): Promise<AuthResponse> {
  const { data } = await api.post('/auth/login', { email, password, totpCode: totpCode ?? null });
  return data as AuthResponse;
}

export async function register(name: string, email: string, password: string, role: User['role']): Promise<AuthResponse> {
  const { data } = await api.post('/auth/register', { name, email, password, role });
  return data as AuthResponse;
}

export async function logoutApi(): Promise<void> {
  // Best-effort: the backend revokes the refresh token, but logout must
  // still clear local state even if this call fails (e.g. offline).
  try {
    await api.post('/auth/logout', { refreshToken: localStorage.getItem('hrms_refresh_token') });
  } catch {
    // Ignored: local tokens are cleared by the caller regardless.
  }
}

export async function mfaSetup(): Promise<{ secret: string; otpauthUrl: string }> {
  const { data } = await api.post('/auth/mfa/setup');
  return data;
}

export async function mfaVerify(totpCode: string): Promise<{ mfaEnabled: boolean }> {
  const { data } = await api.post('/auth/mfa/verify', { totpCode });
  return data;
}

export async function mfaDisable(totpCode: string): Promise<{ mfaEnabled: boolean }> {
  const { data } = await api.post('/auth/mfa/disable', { totpCode });
  return data;
}

export async function fetchMe(): Promise<User> {
  const { data } = await api.get('/users/me');
  return data;
}

export async function fetchPatients(): Promise<User[]> {
  const { data } = await api.get('/users/patients');
  return data;
}

export async function fetchMedicalRecords(): Promise<MedicalRecord[]> {
  const { data } = await api.get('/medical-records');
  return data;
}

export async function fetchMedicalRecordsPaged(page: number, size: number, search?: string): Promise<Paged<MedicalRecord>> {
  const { data } = await api.get('/medical-records/paged', { params: { page, size, search: search || undefined } });
  return data;
}

export async function fetchMedicalRecord(id: number): Promise<MedicalRecord> {
  const { data } = await api.get(`/medical-records/${id}`);
  return data;
}

export async function createMedicalRecord(payload: Partial<MedicalRecord> & { patientId: number }) {
  const { data } = await api.post('/medical-records', payload);
  return data as MedicalRecord;
}

export async function updateMedicalRecord(id: number, payload: Partial<MedicalRecord> & { patientId: number }) {
  const { data } = await api.put(`/medical-records/${id}`, payload);
  return data as MedicalRecord;
}

export async function deleteMedicalRecord(id: number) {
  await api.delete(`/medical-records/${id}`);
}

export async function fetchLabReports(): Promise<LabReport[]> {
  const { data } = await api.get('/lab-reports');
  return data;
}

export async function fetchLabReportsPaged(page: number, size: number, search?: string): Promise<Paged<LabReport>> {
  const { data } = await api.get('/lab-reports/paged', { params: { page, size, search: search || undefined } });
  return data;
}

export async function uploadLabReport(form: FormData): Promise<LabReport> {
  const { data } = await api.post('/lab-reports', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

export async function downloadLabReport(id: number, filename: string) {
  const { data } = await api.get(`/lab-reports/${id}/download`, { responseType: 'blob' });
  const url = window.URL.createObjectURL(new Blob([data]));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  window.URL.revokeObjectURL(url);
}

export async function deleteLabReport(id: number) {
  await api.delete(`/lab-reports/${id}`);
}

export async function fetchPrescriptions(): Promise<Prescription[]> {
  const { data } = await api.get('/prescriptions');
  return data;
}

export async function fetchPrescriptionsPaged(page: number, size: number, search?: string): Promise<Paged<Prescription>> {
  const { data } = await api.get('/prescriptions/paged', { params: { page, size, search: search || undefined } });
  return data;
}

export async function fetchPrescription(id: number): Promise<Prescription> {
  const { data } = await api.get(`/prescriptions/${id}`);
  return data;
}

export async function createPrescription(payload: Partial<Prescription> & { patientId: number }) {
  const { data } = await api.post('/prescriptions', payload);
  return data as Prescription;
}

export async function deletePrescription(id: number) {
  await api.delete(`/prescriptions/${id}`);
}

export async function fetchAuditLogs(page: number, size: number, action?: string): Promise<Paged<AuditLog>> {
  const { data } = await api.get('/audit-logs', { params: { page, size, action: action || undefined } });
  return data;
}

export function apiErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as { error?: string; fields?: Record<string, string> } | undefined;
    if (data?.fields) return Object.values(data.fields).join(' ');
    if (data?.error) return data.error;
    if (err.response?.status === 423) return 'Account is temporarily locked after too many failed login attempts. Please try again later.';
    if (err.response?.status === 403) return 'You do not have permission for this action.';
    if (err.response?.status === 401) return 'Your session has expired. Please log in again.';
  }
  return 'Something went wrong. Please try again.';
}
