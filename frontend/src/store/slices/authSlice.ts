import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { fetchMe, login, logoutApi, register } from '../../api/api';
import type { User } from '../../types';

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  mfaRequired: boolean;
  loading: boolean;
  error: string | null;
}

const initialState: AuthState = {
  user: null,
  token: localStorage.getItem('hrms_token'),
  refreshToken: localStorage.getItem('hrms_refresh_token'),
  mfaRequired: false,
  loading: false,
  error: null,
};

function storeTokens(data: { token: string | null; refreshToken: string | null }) {
  if (data.token) localStorage.setItem('hrms_token', data.token);
  if (data.refreshToken) localStorage.setItem('hrms_refresh_token', data.refreshToken);
}

export const loginThunk = createAsyncThunk(
  'auth/login',
  async ({ email, password, totpCode }: { email: string; password: string; totpCode?: string }) => {
    const data = await login(email, password, totpCode);
    if (data.mfaRequired) {
      // No session yet: the backend deliberately returned no tokens.
      return { user: null, token: null, refreshToken: null, mfaRequired: true };
    }
    storeTokens(data);
    return {
      user: { id: data.id, name: data.name, email: data.email, role: data.role } as User,
      token: data.token,
      refreshToken: data.refreshToken,
      mfaRequired: false,
    };
  },
);

export const registerThunk = createAsyncThunk(
  'auth/register',
  async ({ name, email, password, role }: { name: string; email: string; password: string; role: User['role'] }) => {
    const data = await register(name, email, password, role);
    storeTokens(data);
    return {
      user: { id: data.id, name: data.name, email: data.email, role: data.role } as User,
      token: data.token,
      refreshToken: data.refreshToken,
      mfaRequired: false,
    };
  },
);

export const loadMeThunk = createAsyncThunk('auth/me', async () => fetchMe());

/** Revokes the refresh token server-side (best-effort), then clears local state. */
export const logoutThunk = createAsyncThunk('auth/logout', async () => {
  await logoutApi();
});

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout(state) {
      state.user = null;
      state.token = null;
      state.refreshToken = null;
      state.mfaRequired = false;
      localStorage.removeItem('hrms_token');
      localStorage.removeItem('hrms_refresh_token');
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginThunk.pending, (s) => { s.loading = true; s.error = null; })
      .addCase(loginThunk.fulfilled, (s, a) => {
        s.loading = false;
        s.user = a.payload.user;
        s.token = a.payload.token;
        s.refreshToken = a.payload.refreshToken;
        s.mfaRequired = a.payload.mfaRequired;
      })
      .addCase(loginThunk.rejected, (s, a) => { s.loading = false; s.error = a.error.message ?? 'Login failed'; })
      .addCase(registerThunk.pending, (s) => { s.loading = true; s.error = null; })
      .addCase(registerThunk.fulfilled, (s, a) => {
        s.loading = false;
        s.user = a.payload.user;
        s.token = a.payload.token;
        s.refreshToken = a.payload.refreshToken;
        s.mfaRequired = false;
      })
      .addCase(registerThunk.rejected, (s, a) => { s.loading = false; s.error = a.error.message ?? 'Registration failed'; })
      .addCase(loadMeThunk.fulfilled, (s, a) => { s.user = a.payload; })
      .addCase(loadMeThunk.rejected, (s) => {
        s.user = null; s.token = null; s.refreshToken = null;
        localStorage.removeItem('hrms_token');
        localStorage.removeItem('hrms_refresh_token');
      })
      .addCase(logoutThunk.fulfilled, (s) => {
        s.user = null; s.token = null; s.refreshToken = null; s.mfaRequired = false;
        localStorage.removeItem('hrms_token');
        localStorage.removeItem('hrms_refresh_token');
      })
      .addCase(logoutThunk.rejected, (s) => {
        // Even if the server call failed, the local session is gone.
        s.user = null; s.token = null; s.refreshToken = null; s.mfaRequired = false;
        localStorage.removeItem('hrms_token');
        localStorage.removeItem('hrms_refresh_token');
      });
  },
});

export const { logout } = authSlice.actions;
export default authSlice.reducer;
