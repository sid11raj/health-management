import { useEffect } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from './store/store';
import { loadMeThunk } from './store/slices/authSlice';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import MedicalRecordsPage from './pages/MedicalRecordsPage';
import LabReportsPage from './pages/LabReportsPage';
import PrescriptionsPage from './pages/PrescriptionsPage';
import MedicalRecordDetailPage from './pages/MedicalRecordDetailPage';
import PrescriptionDetailPage from './pages/PrescriptionDetailPage';
import SecurityPage from './pages/SecurityPage';
import AuditLogsPage from './pages/AuditLogsPage';

export default function App() {
  const dispatch = useDispatch<AppDispatch>();
  const token = useSelector((state: RootState) => state.auth.token);

  useEffect(() => {
    if (token) dispatch(loadMeThunk());
  }, [dispatch, token]);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/records" element={<MedicalRecordsPage />} />
          <Route path="/records/:id" element={<MedicalRecordDetailPage />} />
          <Route path="/lab-reports" element={<LabReportsPage />} />
          <Route path="/prescriptions" element={<PrescriptionsPage />} />
          <Route path="/prescriptions/:id" element={<PrescriptionDetailPage />} />
          <Route path="/security" element={<SecurityPage />} />
          <Route path="/audit-logs" element={<AuditLogsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
