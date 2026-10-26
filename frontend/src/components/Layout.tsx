import { AppBar, Box, Button, Container, Toolbar, Typography } from '@mui/material';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import type { AppDispatch, RootState } from '../store/store';
import { logoutThunk } from '../store/slices/authSlice';

export default function Layout() {
  const user = useSelector((state: RootState) => state.auth.user);
  const dispatch = useDispatch<AppDispatch>();
  const navigate = useNavigate();

  const handleLogout = () => {
    void dispatch(logoutThunk());
    navigate('/login');
  };

  return (
    <Box>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Health Records
          </Typography>
          <Button color="inherit" component={Link} to="/">Dashboard</Button>
          <Button color="inherit" component={Link} to="/records">Medical Records</Button>
          <Button color="inherit" component={Link} to="/lab-reports">Lab Reports</Button>
          <Button color="inherit" component={Link} to="/prescriptions">Prescriptions</Button>
          <Button color="inherit" component={Link} to="/security">Security</Button>
          {user?.role === 'ADMIN' && (
            <Button color="inherit" component={Link} to="/audit-logs">Audit Logs</Button>
          )}
          {user && (
            <Typography variant="body2" sx={{ ml: 2, mr: 1 }}>
              {user.name} ({user.role})
            </Typography>
          )}
          <Button color="inherit" onClick={handleLogout}>Logout</Button>
        </Toolbar>
      </AppBar>
      <Container sx={{ py: 4 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
