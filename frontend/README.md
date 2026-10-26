# Health Records Frontend

React 18 + TypeScript + Vite + Redux Toolkit + Material-UI.

## Run

```bash
npm install
npm run dev
```

The app runs on http://localhost:5173 by default and calls the backend at
`http://localhost:8080/api`. Override it with `VITE_API_URL` in a `.env` file:

```
VITE_API_URL=http://localhost:8080/api
```

## Pages

- Login / Register — JWT auth, role selection (ADMIN, DOCTOR, PATIENT)
- Dashboard — counts for medical records, lab reports and prescriptions
- Medical Records — list, create and edit (patients see only their own)
- Lab Reports — upload a file, list and download
- Prescriptions — list and create

Protected routes redirect to `/login` when no JWT token is stored. The axios
client in `src/api/api.ts` attaches the token to every request.
