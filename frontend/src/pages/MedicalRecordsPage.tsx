import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import { Link } from 'react-router-dom';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { apiErrorMessage, createMedicalRecord, deleteMedicalRecord, fetchMedicalRecordsPaged, fetchPatients, updateMedicalRecord } from '../api/api';
import type { MedicalRecord, User } from '../types';

const emptyForm = { title: '', description: '', diagnosis: '', treatment: '', recordDate: '', patientId: 0 };

export default function MedicalRecordsPage() {
  const user = useSelector((state: RootState) => state.auth.user);
  const [records, setRecords] = useState<MedicalRecord[]>([]);
  const [patients, setPatients] = useState<User[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<MedicalRecord | null>(null);
  const [form, setForm] = useState(emptyForm);
  const isStaff = user?.role === 'ADMIN' || user?.role === 'DOCTOR';

  const load = async (p = page, s = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchMedicalRecordsPaged(p, 10, s);
      setRecords(data.content);
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

  const openCreate = () => {
    setEditing(null);
    setForm({ ...emptyForm, patientId: user?.role === 'PATIENT' ? user.id : 0 });
    setOpen(true);
  };

  const openEdit = (record: MedicalRecord) => {
    setEditing(record);
    setForm({
      title: record.title, description: record.description ?? '', diagnosis: record.diagnosis ?? '',
      treatment: record.treatment ?? '', recordDate: record.recordDate ?? '', patientId: record.patientId,
    });
    setOpen(true);
  };

  const submit = async () => {
    if (!form.title.trim()) { setError('Title is required.'); return; }
    if (!form.patientId) { setError('Choose a patient.'); return; }
    try {
      if (editing) await updateMedicalRecord(editing.id, form);
      else await createMedicalRecord(form);
      setOpen(false);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm('Delete this medical record?')) return;
    try { await deleteMedicalRecord(id); await load(); } catch (err) { setError(apiErrorMessage(err)); }
  };

  if (loading) return <CircularProgress />;

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Typography variant="h4">Medical Records</Typography>
        <Button variant="contained" onClick={openCreate}>New record</Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ my: 2 }} action={<Button color="inherit" size="small" onClick={() => void load()}>Retry</Button>}>
          {error}
        </Alert>
      )}
      <Box sx={{ display: 'flex', gap: 1, mt: 2 }}>
        <TextField label="Search by title" size="small" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} onKeyDown={(e) => { if (e.key === 'Enter') { setSearch(searchInput); void load(0, searchInput); } }} />
        <Button variant="outlined" onClick={() => { setSearch(searchInput); void load(0, searchInput); }}>Search</Button>
      </Box>
      {!loading && records.length === 0 && <Typography sx={{ mt: 3 }}>No medical records yet.</Typography>}
      <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 2 }}>
        {records.map((record) => (
          <Card key={record.id}>
            <CardContent>
              <Typography variant="h6">{record.title}</Typography>
              <Typography variant="body2" color="text.secondary">
                Patient: {record.patientName ?? record.patientId} {record.recordDate ? `• ${record.recordDate}` : ''}
              </Typography>
              {record.diagnosis && <Typography>Diagnosis: {record.diagnosis}</Typography>}
              {record.treatment && <Typography>Treatment: {record.treatment}</Typography>}
              {record.description && <Typography>{record.description}</Typography>}
              <Box sx={{ mt: 1 }}>
                <Button size="small" component={Link} to={`/records/${record.id}`}>View</Button>
                <Button size="small" onClick={() => openEdit(record)}>Edit</Button>
                <Button size="small" color="error" onClick={() => remove(record.id)}>Delete</Button>
              </Box>
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
        <DialogTitle>{editing ? 'Edit medical record' : 'New medical record'}</DialogTitle>
        <DialogContent>
          <TextField label="Title" fullWidth required margin="normal" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} />
          {isStaff ? (
            <FormControl fullWidth margin="normal">
              <InputLabel>Patient</InputLabel>
              <Select label="Patient" value={form.patientId || ''} onChange={(e) => setForm({ ...form, patientId: Number(e.target.value) })}>
                {patients.map((p) => <MenuItem key={p.id} value={p.id}>{p.name} ({p.email})</MenuItem>)}
              </Select>
            </FormControl>
          ) : (
            <Typography variant="body2" sx={{ mt: 1 }}>This record will be created for your own account.</Typography>
          )}
          <TextField label="Diagnosis" fullWidth margin="normal" value={form.diagnosis} onChange={(e) => setForm({ ...form, diagnosis: e.target.value })} />
          <TextField label="Treatment" fullWidth multiline rows={3} margin="normal" value={form.treatment} onChange={(e) => setForm({ ...form, treatment: e.target.value })} />
          <TextField label="Description" fullWidth multiline rows={3} margin="normal" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <TextField label="Record date" type="date" fullWidth margin="normal" InputLabelProps={{ shrink: true }} value={form.recordDate} onChange={(e) => setForm({ ...form, recordDate: e.target.value })} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={submit}>Save</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
