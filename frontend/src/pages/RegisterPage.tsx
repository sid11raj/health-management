import { useState } from 'react';
import { Alert, Box, Button, CircularProgress, FormControl, InputLabel, List, ListItem, ListItemIcon, ListItemText, MenuItem, Paper, Select, TextField, Typography } from '@mui/material';
// Plain text marks, not @mui/icons-material, to avoid a new dependency
// for two small checklist glyphs.
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from '../store/store';
import { registerThunk } from '../store/slices/authSlice';
import { apiErrorMessage } from '../api/api';
import type { User } from '../types';

export default function RegisterPage() {
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<User['role']>('PATIENT');
  const [error, setError] = useState('');
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();
  const loading = useSelector((state: RootState) => state.auth.loading);

  // Mirrors the server-side policy (AuthService/PasswordPolicy): 12+ chars,
  // upper, lower, digit, symbol. The server still enforces it — this is
  // only a live hint so users are not surprised by a rejection.
  const checks = [
    { label: 'At least 12 characters', ok: password.length >= 12 },
    { label: 'An upper-case letter (A–Z)', ok: /[A-Z]/.test(password) },
    { label: 'A lower-case letter (a–z)', ok: /[a-z]/.test(password) },
    { label: 'A digit (0–9)', ok: /[0-9]/.test(password) },
    { label: 'A symbol (e.g. ! @ # $ %)', ok: /[^A-Za-z0-9]/.test(password) },
  ];
  const met = checks.filter((c) => c.ok).length;
  const strength = password.length === 0 ? '' : met <= 2 ? 'Weak' : met <= 4 ? 'Fair' : 'Strong';

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (name.trim().length < 2) { setError('Enter your full name.'); return; }
    if (!email.includes('@')) { setError('Enter a valid email address.'); return; }
    if (checks.some((c) => !c.ok)) { setError('Password must be at least 12 characters with upper-case, lower-case, a digit and a symbol.'); return; }
    try {
      await dispatch(registerThunk({ name, email, password, role })).unwrap();
      navigate('/');
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  return (
    <Box display="flex" justifyContent="center" mt={6}>
      <Paper sx={{ p: 4, width: 420, maxWidth: '100%' }}>
        <Typography variant="h5" gutterBottom>Create an account</Typography>
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
        <form onSubmit={submit}>
          <TextField label="Full name" fullWidth required margin="normal" value={name} onChange={(e) => setName(e.target.value)} />
          <TextField label="Email" type="email" fullWidth required margin="normal" value={email} onChange={(e) => setEmail(e.target.value)} />
          <TextField label="Password" type="password" fullWidth required margin="normal" value={password} onChange={(e) => setPassword(e.target.value)} />
          <Typography variant="body2" color={strength === 'Strong' ? 'success.main' : 'text.secondary'} sx={{ mt: 1 }}>
            Password strength: {strength || '—'}
          </Typography>
          <List dense sx={{ py: 0 }}>
            {checks.map((c) => (
              <ListItem key={c.label} sx={{ py: 0 }}>
                <ListItemIcon sx={{ minWidth: 28 }}>
                  <Typography variant="body2" color={c.ok ? 'success.main' : 'error.main'}>{c.ok ? '✓' : '✗'}</Typography>
                </ListItemIcon>
                <ListItemText primary={c.label} primaryTypographyProps={{ variant: 'body2', color: c.ok ? 'success.main' : 'text.secondary' }} />
              </ListItem>
            ))}
          </List>
          <FormControl fullWidth margin="normal">
            <InputLabel>Role</InputLabel>
            <Select label="Role" value={role} onChange={(e) => setRole(e.target.value as User['role'])}>
              <MenuItem value="PATIENT">Patient</MenuItem>
              <MenuItem value="DOCTOR">Doctor</MenuItem>
              <MenuItem value="ADMIN">Admin</MenuItem>
            </Select>
          </FormControl>
          <Button type="submit" variant="contained" fullWidth sx={{ mt: 2 }} disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Register'}
          </Button>
        </form>
        <Typography variant="body2" sx={{ mt: 2 }}>
          Already have an account? <Link to="/login">Log in</Link>
        </Typography>
      </Paper>
    </Box>
  );
}
