import { useState } from 'react';
import { Alert, Box, Button, CircularProgress, Paper, TextField, Typography } from '@mui/material';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from '../store/store';
import { loginThunk } from '../store/slices/authSlice';
import { apiErrorMessage } from '../api/api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [mfaStep, setMfaStep] = useState(false);
  const [error, setError] = useState('');
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const loading = useSelector((state: RootState) => state.auth.loading);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (!email.includes('@')) { setError('Enter a valid email address.'); return; }
    if (password.length < 1) { setError('Enter your password.'); return; }
    if (mfaStep && totpCode.length !== 6) { setError('Enter the 6-digit code from your authenticator app.'); return; }
    try {
      const result = await dispatch(loginThunk({ email, password, totpCode: mfaStep ? totpCode : undefined })).unwrap();
      if (result.mfaRequired) {
        // Password was correct but this account has MFA enabled: ask for
        // the authenticator code. No session exists yet.
        setMfaStep(true);
        return;
      }
      navigate('/');
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  return (
    <Box display="flex" justifyContent="center" mt={6}>
      <Paper sx={{ p: 4, width: 420, maxWidth: '100%' }}>
        <Typography variant="h5" gutterBottom>Log in</Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Access your medical records, lab reports and prescriptions.
        </Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={submit}>
          <TextField label="Email" type="email" fullWidth required margin="normal" value={email} onChange={(e) => setEmail(e.target.value)} disabled={mfaStep} />
          <TextField label="Password" type="password" fullWidth required margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} disabled={mfaStep} />
          {mfaStep && (
            <TextField label="Authenticator code (6 digits)" fullWidth required margin="normal" inputProps={{ maxLength: 6, inputMode: 'numeric' }} helperText="Open your authenticator app and enter the current code." value={totpCode} onChange={(e) => setTotpCode(e.target.value.replace(/\D/g, ''))} autoFocus />
          )}
          <Button type="submit" variant="contained" fullWidth sx={{ mt: 2 }} disabled={loading}>
            {loading ? <CircularProgress size={24} /> : mfaStep ? 'Verify code' : 'Log in'}
          </Button>
        </form>
        <Typography variant="body2" sx={{ mt: 2 }}>
          No account? <Link to="/register">Register</Link>
        </Typography>
      </Paper>
    </Box>
  );
}
