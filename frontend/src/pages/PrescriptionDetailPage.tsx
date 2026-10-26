import { useEffect, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Paper, Typography } from '@mui/material';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiErrorMessage, deletePrescription, fetchPrescription } from '../api/api';
import type { Prescription } from '../types';

export default function PrescriptionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [prescription, setPrescription] = useState<Prescription | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async () => {
    if (!id) return;
    setLoading(true);
    setError('');
    try {
      setPrescription(await fetchPrescription(Number(id)));
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
        <Button component={Link} to="/prescriptions">Back to prescriptions</Button>
      </Box>
    );
  }
  if (!prescription) return <Typography>Prescription not found.</Typography>;

  return (
    <Box>
      <Button component={Link} to="/prescriptions" sx={{ mb: 2 }}>← Back to prescriptions</Button>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h4" gutterBottom>{prescription.medication}</Typography>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          Patient: {prescription.patientName ?? `#${prescription.patientId}`} · Doctor: {prescription.doctorName ?? '—'}
        </Typography>
        {prescription.dosage && <Typography paragraph><strong>Dosage:</strong> {prescription.dosage}</Typography>}
        {prescription.instructions && <Typography paragraph><strong>Instructions:</strong> {prescription.instructions}</Typography>}
        <Typography variant="body2" color="text.secondary">
          {prescription.startDate ? `From ${prescription.startDate}` : ''} {prescription.endDate ? `to ${prescription.endDate}` : ''}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Created: {prescription.createdAt ?? '—'} · Updated: {prescription.updatedAt ?? '—'}
        </Typography>
        <Box sx={{ mt: 3 }}>
          <Button
            color="error"
            onClick={async () => {
              if (!window.confirm('Delete this prescription?')) return;
              try {
                await deletePrescription(prescription.id);
                navigate('/prescriptions');
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
