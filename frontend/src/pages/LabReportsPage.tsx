import { useEffect, useState } from 'react';
import { Alert, Box, Button, Card, CardContent, CircularProgress, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputLabel, MenuItem, Select, TextField, Typography } from '@mui/material';
import { useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { apiErrorMessage, deleteLabReport, downloadLabReport, fetchLabReportsPaged, fetchPatients, uploadLabReport } from '../api/api';
import type { LabReport, User } from '../types';

export default function LabReportsPage() {
  const user = useSelector((state: RootState) => state.auth.user);
  const [reports, setReports] = useState<LabReport[]>([]);
  const [patients, setPatients] = useState<User[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [patientId, setPatientId] = useState<number>(0);
  const [file, setFile] = useState<File | null>(null);
  const isStaff = user?.role === 'ADMIN' || user?.role === 'DOCTOR';

  const load = async (p = page, s = search) => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchLabReportsPaged(p, 10, s);
      setReports(data.content);
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
    if (!title.trim()) { setError('Title is required.'); return; }
    if (!file) { setError('Choose a file to upload.'); return; }
    const pid = user?.role === 'PATIENT' ? user.id : patientId;
    if (!pid) { setError('Choose a patient.'); return; }
    const form = new FormData();
    form.append('title', title);
    if (description) form.append('description', description);
    form.append('patientId', String(pid));
    form.append('file', file);
    try {
      await uploadLabReport(form);
      setOpen(false); setTitle(''); setDescription(''); setFile(null); setPatientId(0);
      await load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  if (loading) return <CircularProgress />;

  return (
    <Box>
      <Box display="flex" justifyContent="space-between" alignItems="center">
        <Typography variant="h4">Lab Reports</Typography>
        <Button variant="contained" onClick={() => setOpen(true)}>Upload report</Button>
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
      {!loading && reports.length === 0 && <Typography sx={{ mt: 3 }}>No lab reports yet.</Typography>}
      <Box display="flex" flexDirection="column" gap={2} sx={{ mt: 2 }}>
        {reports.map((report) => (
          <Card key={report.id}>
            <CardContent>
              <Typography variant="h6">{report.title}</Typography>
              <Typography variant="body2" color="text.secondary">
                {report.originalFileName} • {Math.round(report.fileSize / 1024)} KB • Patient: {report.patientName ?? report.patientId}
              </Typography>
              {report.description && <Typography>{report.description}</Typography>}
              <Box sx={{ mt: 1 }}>
                <Button size="small" onClick={() => downloadLabReport(report.id, report.originalFileName)}>Download</Button>
                <Button size="small" color="error" onClick={async () => { if (window.confirm('Delete this lab report?')) { await deleteLabReport(report.id); await load(); } }}>Delete</Button>
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
        <DialogTitle>Upload lab report</DialogTitle>
        <DialogContent>
          <TextField label="Title" fullWidth required margin="normal" value={title} onChange={(e) => setTitle(e.target.value)} />
          <TextField label="Description" fullWidth multiline rows={3} margin="normal" value={description} onChange={(e) => setDescription(e.target.value)} />
          {isStaff && (
            <FormControl fullWidth margin="normal">
              <InputLabel>Patient</InputLabel>
              <Select label="Patient" value={patientId || ''} onChange={(e) => setPatientId(Number(e.target.value))}>
                {patients.map((p) => <MenuItem key={p.id} value={p.id}>{p.name} ({p.email})</MenuItem>)}
              </Select>
            </FormControl>
          )}
          <Box sx={{ mt: 2 }}>
            <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={submit}>Upload</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
