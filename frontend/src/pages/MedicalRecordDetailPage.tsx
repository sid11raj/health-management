import { useEffect, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Paper, Typography } from '@mui/material';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiErrorMessage, deleteMedicalRecord, fetchMedicalRecord } from '../api/api';
import type { MedicalRecord } from '../types';

export default function MedicalRecordDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [record, setRecord] = useState<MedicalRecord | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      setRecord(await fetchMedicalRecord(Number(id)));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(); }, [id]);

  if (loading) return <CircularProgress />;
  if (error) {
    return (
      <Box>
        <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" size="small" onClick={() => void load()}>Retry</Button>}>{error}</Alert>
        <Button component={Link} to="/records">Back to records</Button>
      </Box>
    );
  }
  if (!record) return <Typography>Record not found.</Typography>;

  return (
    <Box>
      <Button component={Link} to="/records" sx={{ mb: 2 }}>← Back to records</Button>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>{record.title}</Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Patient: {record.patientName ?? `#${record.patientId}`} · Doctor: {record.doctorName ?? '—'} · Date: {record.recordDate ?? '—'}
        </Typography>
        {record.description && <Typography paragraph>{record.description}</Typography>}
        {record.diagnosis && <Typography paragraph><strong>Diagnosis:</strong> {record.diagnosis}</Typography>}
        {record.treatment && <Typography paragraph><strong>Treatment:</strong> {record.treatment}</Typography>}
        <Typography variant="body2" color="text.secondary">
          Created: {record.createdAt ?? '—'} · Updated: {record.updatedAt ?? '—'}
        </Typography>
        <Box sx={{ mt: 3, display: 'flex', gap: 1 }}>
          <Button variant="outlined" component={Link} to={`/records?edit=${record.id}`}>Edit</Button>
          <Button
            color="error"
            onClick={async () => {
              if (!window.confirm('Delete this medical record?')) return;
              try {
                await deleteMedicalRecord(record.id);
                navigate('/records');
              } catch (err) {
                setError(apiErrorMessage(err));
              }
            }}
          >
            Delete
          </Button>
        </Box>
      </Paper>
    </Box>
  );
}
