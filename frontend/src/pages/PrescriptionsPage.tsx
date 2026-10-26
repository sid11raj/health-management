import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { apiErrorMessage, createPrescription, deletePrescription, fetchPatients, fetchPrescriptionsPaged } from '../api/api';
import type { Prescription, User } from '../types';

export default function PrescriptionsPage() {
  const user = useSelector((state: RootState) => state.auth.user);
  const [items, setItems] = useState<Prescription[]>([]);
  const [patients, setPatients] = useState<User[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ medication: '', dosage: '', instructions: '', startDate: '', endDate: '', patientId: 0 });
  const isStaff = user?.role === 'ADMIN' || user?.role === 'DOCTOR';

  const load = async (p = page, s = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchPrescriptionsPaged(p, 10, s);
      setItems(data.content);
      setTotalPages(data.totalPages);
      setPage(data.number);
      if (isStaff) setPatients(await fetchPatients());
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(0, ''); }, []);

  const submit = async () => {
    if (!form.medication.trim()) { setError('Medication is required.'); return; }
    const pid = user?.role === 'PATIENT' ? user.id : form.patientId;
    if (!pid) { setError('Choose a patient.'); return; }
    try {
      await createPrescription({ ...form, patientId: pid });
      setOpen(false); setForm({ medication: '', dosage: '', instructions: '', startDate: '', endDate: '', patientId: 0 });
      await load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  if (loading) return <CircularProgress />;

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Typography variant="h4">Prescriptions</Typography>
        <Button variant="contained" onClick={() => setOpen(true)}>New prescription</Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ my: 2 }} action={<Button color="inherit" size="small" onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
        <TextField label="Search by medication" size="small" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') { setSearch(searchInput); void load(0, searchInput); } }} />
        <Button variant="outlined" onClick={() => { setSearch(searchInput); void load(0, searchInput); }}>Search</Button>
      </Box>
      {!loading && items.length === 0 && <Typography sx={{ mt: 3 }}>No prescriptions yet.</Typography>}
      <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 2 }}>
        {items.map((item) => (
          <Card key={item.id}>
            <CardContent>
              <Typography variant="h6">{item.medication} {item.dosage ? `— ${item.dosage}` : ''}</Typography>
              <Typography variant="body2" color="text.secondary">
                Patient: {item.patientName ?? item.patientId}
                {item.startDate ? ` • From ${item.startDate}` : ''}{item.endDate ? ` to ${item.endDate}` : ''}
              </Typography>
              {item.instructions && <Typography>{item.instructions}</Typography>}
              <Button size="small" component={Link} to={`/prescriptions/${item.id}`} sx={{ mt: 1 }}>View</Button>
              <Button size="small" color="error" sx={{ mt: 1 }} onClick={async () => { if (window.confirm('Delete this prescription?')) { await deletePrescription(item.id); await load(); } }}>Delete</Button>
            </CardContent>
          </Card>
        ))}
      </Box>
      <Box sx={{ display: 'flex', gap: 1, mt: 2, alignItems: 'center' }}>
        <Button disabled={page <= 0} onClick={() => void load(page - 1)}>Previous</Button>
        <Typography variant="body2">Page {page + 1} of {Math.max(totalPages, 1)}</Typography>
        <Button disabled={page + 1 >= totalPages} onClick={() => void load(page + 1)}>Next</Button>
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm">
        <DialogTitle>New prescription</DialogTitle>
        <DialogContent>
          <TextField label="Medication" fullWidth required margin="normal" value={form.medication} onChange={(e) => setForm({ ...form, medication: e.target.value })} />
          <TextField label="Dosage" fullWidth margin="normal" value={form.dosage} onChange={(e) => setForm({ ...form, dosage: e.target.value })} />
          <TextField label="Instructions" fullWidth multiline rows={3} margin="normal" value={form.instructions} onChange={(e) => setForm({ ...form, instructions: e.target.value })} />
          {isStaff && (
            <FormControl fullWidth margin="normal">
              <InputLabel>Patient</InputLabel>
              <Select label="Patient" value={form.patientId || ''} onChange={(e) => setForm({ ...form, patientId: Number(e.target.value) })}>
                {patients.map((p) => <MenuItem key={p.id} value={p.id}>{p.name} ({p.email})</MenuItem>)}
              </Select>
            </FormControl>
          )}
          <TextField label="Start date" type="date" fullWidth margin="normal" InputLabelProps={{ shrink: true }} value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} />
          <TextField label="End date" type="date" fullWidth margin="normal" InputLabelProps={{ shrink: true }} value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={submit}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
