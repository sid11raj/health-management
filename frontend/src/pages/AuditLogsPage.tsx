import { useEffect, useState } from 'react';
import { Alert, Box, Button, CircularProgress, Paper, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { apiErrorMessage, fetchAuditLogs } from '../api/api';
import type { AuditLog } from '../types';

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [actionFilter, setActionFilter] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (p: number, action: string) => {
    setLoading(true);
    setError('');
    try {
      const data = await fetchAuditLogs(p, 20, action);
      setLogs(data.content);
      setTotalPages(data.totalPages);
      setPage(data.number);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { void load(0, ''); }, []);

  const applyFilter = () => {
    setActionFilter(searchInput);
    void load(0, searchInput);
  };

  return (
    <Box>
      <Typography variant="h4" gutterBottom>Audit Logs</Typography>
      <Typography variant="body2" color="text.secondary" gutterBottom>
        Admin only. Records who did what, when, and from which IP — login events, record and prescription
        changes, lab-report uploads/downloads, and role changes. Health data itself is never stored here.
      </Typography>
      <Box sx={{ display: 'flex', gap: 1, my: 2 }}>
        <TextField label="Filter by action (e.g. LOGIN)" size="small" value={searchInput} onChange={(e) => setSearchInput(e.target.value)} />
        <Button variant="contained" onClick={applyFilter}>Filter</Button>
      </Box>
      {error && (
        <Alert severity="error" sx={{ my: 2 }} action={<Button color="inherit" size="small" onClick={() => void load(page, actionFilter)}>Retry</Button>}>
          {error}
        </Alert>
      )}
      {loading ? <CircularProgress /> : (
        <Paper>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Time</TableCell>
                <TableCell>Actor</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>IP</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {logs.length === 0 && (
                <TableRow><TableCell colSpan={5}>No audit logs yet.</TableCell></TableRow>
              )}
              {logs.map((log) => (
                <TableRow key={log.id}>
                  <TableCell>{log.createdAt}</TableCell>
                  <TableCell>{log.actorEmail}</TableCell>
                  <TableCell>{log.action}</TableCell>
                  <TableCell>{log.entityType} {log.entityId ?? ''}</TableCell>
                  <TableCell>{log.ipAddress ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
      <Box sx={{ display: 'flex', gap: 1, mt: 2, alignItems: 'center' }}>
        <Button disabled={page <= 0} onClick={() => void load(page - 1, actionFilter)}>Previous</Button>
        <Typography variant="body2">Page {page + 1} of {Math.max(totalPages, 1)}</Typography>
        <Button disabled={page + 1 >= totalPages} onClick={() => void load(page + 1, actionFilter)}>Next</Button>
      </Box>
    </Box>
  );
}
