import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, CardContent, CircularProgress, Grid, List, ListItem, ListItemButton, ListItemText, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { apiErrorMessage, fetchLabReports, fetchMedicalRecords, fetchPrescriptions } from '../api/api';
import type { LabReport, MedicalRecord, Prescription } from '../types';

export default function DashboardPage() {
  const user = useSelector((state: RootState) => state.auth.user);
  const [counts, setCounts] = useState({ records: 0, reports: 0, prescriptions: 0 });
  const [recentRecords, setRecentRecords] = useState<MedicalRecord[]>([]);
  const [recentReports, setRecentReports] = useState<LabReport[]>([]);
  const [recentPrescriptions, setRecentPrescriptions] = useState<Prescription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const [records, reports, prescriptions] = await Promise.all([
        fetchMedicalRecords(), fetchLabReports(), fetchPrescriptions(),
      ]);
      setCounts({ records: records.length, reports: reports.length, prescriptions: prescriptions.length });
      const byDate = (a: string | undefined, b: string | undefined) => (b ?? '').localeCompare(a ?? '');
      setRecentRecords([...records].sort((a, b) => byDate(a.createdAt, b.createdAt)).slice(0, 5));
      setRecentReports([...reports].sort((a, b) => byDate(a.uploadedAt, b.uploadedAt)).slice(0, 5));
      setRecentPrescriptions([...prescriptions].sort((a, b) => byDate(a.createdAt, b.createdAt)).slice(0, 5));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, []);

  if (loading) return <CircularProgress />;

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Welcome, {user?.name ?? 'there'}
      </Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        You are signed in as {user?.role}. Patients see only their own data; doctors and admins see all patients.
      </Typography>
      {error && (
        <Alert severity="error" sx={{ my: 2 }} action={<Button color="inherit" size="small" onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      <Grid container spacing={3} sx={{ mt: 1 }}>
        {[
          { label: 'Medical Records', value: counts.records, to: '/records' },
          { label: 'Lab Reports', value: counts.reports, to: '/lab-reports' },
          { label: 'Prescriptions', value: counts.prescriptions, to: '/prescriptions' },
        ].map((card) => (
          <Grid item xs={12} sm={4} key={card.label}>
            <Card>
              <CardContent>
                <Typography variant="h3">{card.value}</Typography>
                <Typography variant="subtitle1">{card.label}</Typography>
                <Button component={Link} to={card.to} size="small">View all</Button>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>
      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={4}>
          <Typography variant="h6" gutterBottom>Recent medical records</Typography>
          <List>
            {recentRecords.length === 0 && <ListItem><ListItemText primary="No records yet." /></ListItem>}
            {recentRecords.map((r) => (
              <ListItem key={r.id} disablePadding>
                <ListItemButton component={Link} to={`/records/${r.id}`}>
                  <ListItemText primary={r.title} secondary={r.recordDate ?? r.createdAt ?? ''} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Grid>
        <Grid item xs={12} md={4}>
          <Typography variant="h6" gutterBottom>Recent lab reports</Typography>
          <List>
            {recentReports.length === 0 && <ListItem><ListItemText primary="No lab reports yet." /></ListItem>}
            {recentReports.map((r) => (
              <ListItem key={r.id} disablePadding>
                <ListItemText primary={r.title} secondary={r.uploadedAt ?? ''} />
              </ListItem>
            ))}
          </List>
        </Grid>
        <Grid item xs={12} md={4}>
          <Typography variant="h6" gutterBottom>Recent prescriptions</Typography>
          <List>
            {recentPrescriptions.length === 0 && <ListItem><ListItemText primary="No prescriptions yet." /></ListItem>}
            {recentPrescriptions.map((p) => (
              <ListItem key={p.id} disablePadding>
                <ListItemButton component={Link} to={`/prescriptions/${p.id}`}>
                  <ListItemText primary={p.medication} secondary={p.dosage ?? ''} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        </Grid>
      </Grid>
    </Box>
  );
}
