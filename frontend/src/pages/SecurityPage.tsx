import { useState } from 'react';
import { Alert, Box, Button, CircularProgress, Paper, TextField, Typography } from '@mui/material';
import { apiErrorMessage, mfaDisable, mfaSetup, mfaVerify } from '../api/api';

export default function SecurityPage() {
  const [setup, setSetup] = useState<{ secret: string; otpauthUrl: string } | null>(null);
  const [code, setCode] = useState('');
  const [disableCode, setDisableCode] = useState('');
  const [enabled, setEnabled] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const startSetup = async () => {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      // The secret is shown ONCE here. Save it in your authenticator app
      // now — it is never returned again after verification.
      setSetup(await mfaSetup());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const verify = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (code.length !== 6) { setError('Enter the 6-digit code.'); return; }
    setLoading(true);
    try {
      const res = await mfaVerify(code);
      setEnabled(res.mfaEnabled);
      setSetup(null);
      setMessage('Two-factor authentication is now enabled for your account.');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const disable = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setMessage('');
    if (disableCode.length !== 6) { setError('Enter the 6-digit code.'); return; }
    setLoading(true);
    try {
      const res = await mfaDisable(disableCode);
      setEnabled(res.mfaEnabled);
      setMessage('Two-factor authentication has been disabled.');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>Security</Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        Two-factor authentication (TOTP) adds a second step to login: a 6-digit code from an authenticator
        app such as Google Authenticator, Microsoft Authenticator or Authy. It is optional.
      </Typography>
      {error && <Alert severity="error" sx={{ my: 2 }}>{error}</Alert>}
      {message && <Alert severity="success" sx={{ my: 2 }}>{message}</Alert>}
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography variant="h6" gutterBottom>Two-factor authentication</Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Status: {enabled ? 'Enabled' : 'Not enabled'}
        </Typography>
        {!setup && (
          <Button variant="contained" onClick={() => void startSetup()} disabled={loading}>
            {loading ? <CircularProgress size={24} /> : 'Set up authenticator'}
          </Button>
        )}
        {setup && (
          <Box sx={{ mt: 2 }}>
            <Alert severity="warning" sx={{ mb: 2 }}>
              This secret is shown once. Add it to your authenticator app now — if you close this page
              before verifying, run setup again to get a fresh secret.
            </Alert>
            <Typography variant="body2" sx={{ wordBreak: 'break-all' }}>
              <strong>Secret:</strong> {setup.secret}
            </Typography>
            <Typography variant="body2" sx={{ wordBreak: 'break-all', mt: 1 }}>
              <strong>Authenticator URL:</strong> {setup.otpauthUrl}
            </Typography>
            <form onSubmit={verify}>
              <TextField label="Authenticator code (6 digits)" fullWidth margin="normal" inputProps={{ maxLength: 6, inputMode: 'numeric' }} value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ''))} />
              <Button type="submit" variant="contained" disabled={loading}>Verify and enable</Button>
            </form>
          </Box>
        )}
      </Paper>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>Disable two-factor authentication</Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Enter a current code from your authenticator app to confirm.
        </Typography>
        <form onSubmit={disable}>
          <TextField label="Authenticator code (6 digits)" fullWidth margin="normal" inputProps={{ maxLength: 6, inputMode: 'numeric' }} value={disableCode} onChange={(e) => setDisableCode(e.target.value.replace(/\D/g, ''))} />
          <Button type="submit" variant="contained" color="error" disabled={loading}>Disable MFA</Button>
        </form>
      </Paper>
    </Box>
  );
}
