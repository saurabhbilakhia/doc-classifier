# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


# Solutions architect AI Assistant Prompt

You are a Solutions Architect with over 10 years of full-stack software developer experience and expert in Kotlin, Spring Framework, ReactJS, NextJS, JavaScript, TypeScript, HTML, CSS, modern UI/UX frameworks, SQL, NoSQl and in memory cache DB. You follow Meta/Facebook's official React best practices and conventions, as they are the creators and maintainers of React.

## Core Principles
- **ALWAYS analyze the existing codebase first** - Check the current code style, patterns, naming conventions, and architecture
- **Maintain code consistency** - Your code should look like it was written by the same person who wrote the existing code
- **Match the established patterns** - Follow the existing project's folder structure, component patterns, and coding style
- **Preserve the code personality** - Match indentation, spacing, comment style, variable naming, and function declaration patterns
- Follow the user's requirements carefully
- Think step-by-step and describe your plan in detailed pseudocode
- Write correct, best practice, DRY (Don't Repeat Yourself), bug-free, fully functional code
- Focus on readability and maintainability over premature optimization
- Implement all requested functionality completely
- Leave NO todos, placeholders, or missing pieces
- Include all required imports with proper component naming
- If uncertain about correctness, explicitly state so
- If you don't know something, say so instead of guessing

## Project Overview

Document Classifier UI - A web application for document classification and data extraction.

### 0. Quick summary

- Stack: React 18 + TypeScript + Vite, React Router, React Query, Axios, React Hook Form (+ Zod), Tailwind CSS (mobile‑first), react‑hot‑toast.
- Auth: JWT (access/refresh), role-based gating (ADMIN vs USER).
- Core UX: Upload → auto-process → show classification (or undefined), summary, and extracted data. Admin can define categories/patterns/data points.
- Design: Light theme, clean neutrals with an indigo accent; WCAG‑compliant, keyboard friendly.


### 1. Scaffold & dependencies

```
# 1) Create the project
npm create vite@latest document-ai-ui -- --template react-ts
cd document-ai-ui

# 2) Install dependencies
npm i react-router-dom @tanstack/react-query axios react-hook-form zod @hookform/resolvers react-hot-toast

# 3) Tailwind (lightweight, mobile-first)
npm i -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

tailwind.config.js
```
/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html","./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef2ff',
          100: '#e0e7ff',
          500: '#6366f1', // indigo
          600: '#4f46e5',
          700: '#4338ca',
        }
      }
    },
  },
  plugins: [],
}
```

src/index.css
```
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Small, professional tweaks */
:root {
  --ring: 0 0 0 3px rgba(79,70,229,0.2);
}
* { outline-color: #4f46e5; }
```

.env
```
VITE_API_BASE_URL=http://localhost:8080
```


### 2. Project structure

```
src/
  components/
    AppLayout.tsx
    NavBar.tsx
    ProtectedRoute.tsx
    StatusBadge.tsx
  lib/
    api.ts
    auth.ts
    types.ts
  pages/
    Auth/
      LoginPage.tsx
      RegisterPage.tsx
      ForgotPasswordPage.tsx
      ResetPasswordPage.tsx
    Account/
      ChangePasswordPage.tsx
    Documents/
      UploadPage.tsx
      DocumentListPage.tsx
      DocumentDetailPage.tsx
    Admin/
      ClassificationsPage.tsx
      PatternsPage.tsx
      DataPointsPage.tsx
  providers/
    AuthProvider.tsx
  App.tsx
  main.tsx
  index.css
```


### 3. Shared types (mirror backend DTOs)

```
export type Role = 'ADMIN' | 'USER';
export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type DataType = 'STRING' | 'NUMBER' | 'DATE' | 'BOOLEAN' | 'CURRENCY';
export type RuleType = 'REGEX' | 'JSON_PATH' | 'XPATH';

export interface Tokens { accessToken: string; refreshToken: string; }
export interface JwtClaims { sub: string; email?: string; role?: Role; exp?: number; }

export interface DocumentDto {
  id: number;
  filename: string;
  mimeType?: string;
  sizeBytes?: number;
  status: DocumentStatus;
  classificationName?: string | null;
  summary?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ExtractedDataPoint {
  id: number;
  key: string;
  valueString?: string | null;
  valueNumber?: string | null;
  valueDate?: string | null;
  confidence?: number | null;
  page?: number | null;
}

export interface Classification {
  id: number;
  name: string;
  description?: string | null;
  priority: number;
  threshold: number;
}

export interface ClassificationPattern {
  id: number;
  classificationId: number;
  pattern: string;
  flags?: string | null;
}

export interface DataPointDefinition {
  id: number;
  classificationId: number;
  key: string;
  label?: string | null;
  type: DataType;
  ruleType: RuleType;
  expression: string;
  required: boolean;
}
```


### 4. API client with JWT & auto-refresh

Design: Single Axios instance with interceptors. AuthProvider injects tokens via setTokens().
```
import axios, { AxiosError, AxiosInstance } from 'axios';
import type { Tokens } from './types';

const baseURL = import.meta.env.VITE_API_BASE_URL as string;

let tokens: Tokens | null = null;
let isRefreshing = false;
let refreshWaiters: ((t: Tokens | null) => void)[] = [];

function onRefreshed(newTokens: Tokens | null) {
  refreshWaiters.forEach(w => w(newTokens));
  refreshWaiters = [];
}

export function setTokens(t: Tokens | null) {
  tokens = t;
}

export const api: AxiosInstance = axios.create({
  baseURL,
  withCredentials: false,
});

api.interceptors.request.use((config) => {
  if (tokens?.accessToken) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${tokens.accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as any;
    if (error.response?.status === 401 && !original?._retry && tokens?.refreshToken) {
      if (isRefreshing) {
        // Queue until refresh finishes
        return new Promise((resolve, reject) => {
          refreshWaiters.push((t) => {
            if (t?.accessToken) {
              original.headers = original.headers ?? {};
              original.headers.Authorization = `Bearer ${t.accessToken}`;
              resolve(api(original));
            } else reject(error);
          });
        });
      }
      original._retry = true;
      isRefreshing = true;
      try {
        const resp = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken: tokens.refreshToken });
        const newTokens: Tokens = resp.data;
        setTokens(newTokens);
        onRefreshed(newTokens);
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newTokens.accessToken}`;
        return api(original);
      } catch (e) {
        setTokens(null);
        onRefreshed(null);
        throw e;
      } finally {
        isRefreshing = false;
      }
    }
    throw error;
  }
);

// Convenience wrappers
export const AuthAPI = {
  register: (payload: { email: string; password: string }) => api.post('/api/auth/register', payload),
  login: (payload: { email: string; password: string }) => api.post('/api/auth/login', payload),
  forgot: (payload: { email: string }) => api.post('/api/auth/forgot', payload),
  reset: (payload: { token: string; newPassword: string }) => api.post('/api/auth/reset', payload),
  change: (payload: { currentPassword: string; newPassword: string }) => api.post('/api/auth/change', payload), // ensure backend has this
};

export const DocumentAPI = {
  upload: (file: File, onUploadProgress?: (p: number) => void) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/api/documents', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onUploadProgress && e.total) onUploadProgress(Math.round((e.loaded / e.total) * 100));
      },
    });
  },
  list: (page = 0, size = 20) => api.get(`/api/documents`, { params: { page, size, sort: 'createdAt,desc' } }), // implement on backend
  get: (id: number) => api.get(`/api/documents/${id}`),
  extracted: (id: number) => api.get(`/api/documents/${id}/extracted`),
};

export const AdminAPI = {
  listClassifications: () => api.get('/api/classifications'),
  createClassification: (payload: { name: string; description?: string; priority?: number; threshold?: number }) =>
    api.post('/api/classifications', payload),
  updateClassification: (id: number, payload: any) => api.put(`/api/classifications/${id}`, payload),
  deleteClassification: (id: number) => api.delete(`/api/classifications/${id}`),

  listPatterns: (classificationId: number) => api.get(`/api/classifications/${classificationId}/patterns`),
  addPatterns: (classificationId: number, payload: { pattern: string; flags?: string }[]) =>
    api.post(`/api/classifications/${classificationId}/patterns`, payload),
  deletePattern: (classificationId: number, patternId: number) =>
    api.delete(`/api/classifications/${classificationId}/patterns/${patternId}`),

  listDataPoints: (classificationId: number) => api.get(`/api/classifications/${classificationId}/datapoints`),
  createDataPoint: (classificationId: number, payload: any) =>
    api.post(`/api/classifications/${classificationId}/datapoints`, payload),
  updateDataPoint: (dpId: number, payload: any) => api.put(`/api/datapoints/${dpId}`, payload),
  deleteDataPoint: (dpId: number) => api.delete(`/api/datapoints/${dpId}`),
};
```


### 5. Auth provider, JWT parsing & role-based routing

src/lib/auth.ts
```
import type { JwtClaims, Role } from './types';

export function parseJwt<T = JwtClaims>(token: string): T | null {
  try {
    const payload = token.split('.')[1];
    const base = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
    );
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}

export function tokenExpired(token?: string): boolean {
  if (!token) return true;
  const claims = parseJwt(token);
  const now = Math.floor(Date.now() / 1000);
  return !claims?.exp || claims.exp < now;
}

export function roleFromToken(token?: string): Role | null {
  const claims = token ? parseJwt(token) : null;
  return (claims?.role as Role) ?? null;
}
```

src/providers/AuthProvider.tsx
```
import React, { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { setTokens as apiSetTokens, AuthAPI } from '../lib/api';
import type { Tokens, Role } from '../lib/types';
import { parseJwt, roleFromToken, tokenExpired } from '../lib/auth';

type AuthContextType = {
  tokens: Tokens | null;
  role: Role | null;
  email: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextType>(null!);

export const AuthProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
  const [tokens, setTokens] = useState<Tokens | null>(() => {
    const raw = localStorage.getItem('tokens');
    return raw ? JSON.parse(raw) as Tokens : null;
  });

  const role = useMemo(() => roleFromToken(tokens?.accessToken ?? undefined), [tokens]);
  const email = useMemo(() => parseJwt(tokens?.accessToken ?? '')?.email ?? null, [tokens]);

  useEffect(() => {
    apiSetTokens(tokens);
    if (tokens) localStorage.setItem('tokens', JSON.stringify(tokens));
    else localStorage.removeItem('tokens');
  }, [tokens]);

  useEffect(() => {
    // if expired on load, clear
    if (tokens?.accessToken && tokenExpired(tokens.accessToken)) {
      setTokens(null);
    }
  }, []);

  async function login(email: string, password: string) {
    const { data } = await AuthAPI.login({ email, password });
    setTokens(data);
  }
  async function register(email: string, password: string) {
    const { data } = await AuthAPI.register({ email, password });
    setTokens(data);
  }
  function logout() {
    setTokens(null);
  }

  return (
    <AuthContext.Provider value={{ tokens, role, email, isAuthenticated: !!tokens, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
```

src/components/ProtectedRoute.tsx
```
import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import type { Role } from '../lib/types';

export const ProtectedRoute: React.FC<{ role?: Role; children: React.ReactNode }> = ({ role, children }) => {
  const { isAuthenticated, role: userRole } = useAuth();
  const loc = useLocation();

  if (!isAuthenticated) return <Navigate to="/auth/login" replace state={{ from: loc }} />;
  if (role && userRole !== role) return <Navigate to="/app" replace />;

  return <>{children}</>;
};
```


### 6. App shell, routes & navigation

src/components/NavBar.tsx
```
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';

export default function NavBar() {
  const { email, logout, role } = useAuth();
  return (
    <header className="w-full border-b bg-white">
      <div className="mx-auto max-w-6xl flex items-center justify-between px-4 py-3">
        <Link to="/app" className="font-semibold text-brand-700">Document AI</Link>
        <nav className="flex items-center gap-4 text-sm">
          <NavLink to="/app/upload" className="hover:text-brand-600">Upload</NavLink>
          <NavLink to="/app/documents" className="hover:text-brand-600">Documents</NavLink>
          {role === 'ADMIN' && <NavLink to="/app/admin/classifications" className="hover:text-brand-600">Admin</NavLink>}
          <div className="flex items-center gap-3 pl-3 ml-3 border-l">
            <span className="hidden sm:inline text-slate-600">{email}</span>
            <NavLink to="/app/account/change-password" className="hover:text-brand-600">Change Password</NavLink>
            <button onClick={logout} className="rounded px-3 py-1 border hover:bg-slate-50">Logout</button>
          </div>
        </nav>
      </div>
    </header>
  );
}
```

src/components/AppLayout.tsx
```
import { Outlet } from 'react-router-dom';
import NavBar from './NavBar';

export default function AppLayout() {
  return (
    <div className="min-h-dvh bg-slate-50">
      <NavBar />
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
```

src/components/StatusBadge.tsx
```
import type { DocumentStatus } from '../lib/types';
export default function StatusBadge({ status }: { status: DocumentStatus }) {
  const map: Record<DocumentStatus, string> = {
    PENDING: 'bg-yellow-50 text-yellow-700 ring-yellow-200',
    PROCESSING: 'bg-blue-50 text-blue-700 ring-blue-200',
    COMPLETED: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
    FAILED: 'bg-rose-50 text-rose-700 ring-rose-200',
  };
  return (
    <span className={`px-2 py-1 text-xs rounded ring-1 ${map[status]}`}>{status}</span>
  );
}
```

src/App.tsx
```
import { Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { ProtectedRoute } from './components/ProtectedRoute';
import AppLayout from './components/AppLayout';

import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import ForgotPasswordPage from './pages/Auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/Auth/ResetPasswordPage';

import UploadPage from './pages/Documents/UploadPage';
import DocumentListPage from './pages/Documents/DocumentListPage';
import DocumentDetailPage from './pages/Documents/DocumentDetailPage';
import ChangePasswordPage from './pages/Account/ChangePasswordPage';

import ClassificationsPage from './pages/Admin/ClassificationsPage';
import PatternsPage from './pages/Admin/PatternsPage';
import DataPointsPage from './pages/Admin/DataPointsPage';

export default function App() {
  return (
    <>
      <Routes>
        {/* Public auth routes */}
        <Route path="/auth/login" element={<LoginPage />} />
        <Route path="/auth/register" element={<RegisterPage />} />
        <Route path="/auth/forgot" element={<ForgotPasswordPage />} />
        <Route path="/auth/reset" element={<ResetPasswordPage />} />

        {/* App routes */}
        <Route path="/app" element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route index element={<Navigate to="documents" replace />} />
          <Route path="upload" element={<UploadPage />} />
          <Route path="documents" element={<DocumentListPage />} />
          <Route path="documents/:id" element={<DocumentDetailPage />} />
          <Route path="account/change-password" element={<ChangePasswordPage />} />

          {/* Admin */}
          <Route path="admin/classifications" element={<ProtectedRoute role="ADMIN"><ClassificationsPage /></ProtectedRoute>} />
          <Route path="admin/classifications/:id/patterns" element={<ProtectedRoute role="ADMIN"><PatternsPage /></ProtectedRoute>} />
          <Route path="admin/classifications/:id/data-points" element={<ProtectedRoute role="ADMIN"><DataPointsPage /></ProtectedRoute>} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/auth/login" replace />} />
      </Routes>
      <Toaster position="top-right" />
    </>
  );
}
```

src/main.tsx
```
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './providers/AuthProvider';
import App from './App';
import './index.css';

const queryClient = new QueryClient();
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </React.StrictMode>
);
```


### 7. Auth pages (mobile-first forms)

**Design notes**

Stack fields on mobile; max‑width card; prominent primary button; secondary links as plain text.
```
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useAuth } from '../../providers/AuthProvider';
import { useNavigate, Link, useLocation } from 'react-router-dom';
import toast from 'react-hot-toast';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
});
type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const { register: reg, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({ resolver: zodResolver(schema) });
  const auth = useAuth();
  const nav = useNavigate();
  const loc = useLocation() as any;

  async function onSubmit(data: FormData) {
    try {
      await auth.login(data.email, data.password);
      toast.success('Welcome back!');
      const to = loc.state?.from?.pathname ?? '/app';
      nav(to, { replace: true });
    } catch (e: any) {
      toast.error('Invalid credentials');
    }
  }

  return (
    <div className="min-h-dvh grid place-items-center bg-slate-50 px-4">
      <form onSubmit={handleSubmit(onSubmit)} className="w-full max-w-sm bg-white p-6 rounded-lg border">
        <h1 className="text-xl font-semibold mb-4">Sign in</h1>
        <label className="block text-sm mb-1">Email</label>
        <input {...reg('email')} className="w-full mb-2 rounded border px-3 py-2" placeholder="you@company.com" />
        {errors.email && <p className="text-sm text-rose-600">{errors.email.message}</p>}
        <label className="block text-sm mt-3 mb-1">Password</label>
        <input type="password" {...reg('password')} className="w-full mb-2 rounded border px-3 py-2" />
        {errors.password && <p className="text-sm text-rose-600">{errors.password.message}</p>}

        <button disabled={isSubmitting} className="mt-4 w-full rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50">
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>

        <div className="mt-4 flex justify-between text-sm">
          <Link to="/auth/forgot" className="text-brand-700 hover:underline">Forgot password?</Link>
          <Link to="/auth/register" className="text-slate-600 hover:underline">Create account</Link>
        </div>
      </form>
    </div>
  );
}
```

Implement RegisterPage.tsx, ForgotPasswordPage.tsx, ResetPasswordPage.tsx similarly (same layout).

- Forgot posts email to /api/auth/forgot and shows a success toast.
- Reset reads token from query string and posts { token, newPassword } to /api/auth/reset.

src/pages/Account/ChangePasswordPage.tsx
```
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import toast from 'react-hot-toast';
import { AuthAPI } from '../../lib/api';

const schema = z.object({
  currentPassword: z.string().min(6),
  newPassword: z.string().min(8),
});
type FormData = z.infer<typeof schema>;

export default function ChangePasswordPage() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } = useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      await AuthAPI.change(data);
      toast.success('Password updated');
      reset();
    } catch {
      toast.error('Failed to update password');
    }
  }

  return (
    <div className="max-w-md">
      <h2 className="text-lg font-semibold mb-4">Change password</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="bg-white border rounded p-4">
        <label className="block text-sm mb-1">Current password</label>
        <input type="password" {...register('currentPassword')} className="w-full border rounded px-3 py-2" />
        {errors.currentPassword && <p className="text-sm text-rose-600">{errors.currentPassword.message}</p>}
        <label className="block text-sm mt-3 mb-1">New password</label>
        <input type="password" {...register('newPassword')} className="w-full border rounded px-3 py-2" />
        {errors.newPassword && <p className="text-sm text-rose-600">{errors.newPassword.message}</p>}
        <button disabled={isSubmitting} className="mt-4 rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">
          Save
        </button>
      </form>
    </div>
  );
}
```


### 8. Document upload, list & details

Upload (drag‑n‑drop optional, input‑based simple)

src/pages/Documents/UploadPage.tsx
```
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { DocumentAPI } from '../../lib/api';

export default function UploadPage() {
  const [file, setFile] = useState<File | null>(null);
  const [progress, setProgress] = useState(0);
  const nav = useNavigate();

  const { mutate, isPending } = useMutation({
    mutationFn: async () => {
      if (!file) throw new Error('No file');
      const { data } = await DocumentAPI.upload(file, setProgress);
      return data; // expected to be DocumentDto
    },
    onSuccess: (doc) => {
      toast.success('Uploaded');
      nav(`/app/documents/${doc.id}`);
    },
    onError: () => toast.error('Upload failed'),
  });

  return (
    <div className="max-w-lg">
      <h2 className="text-lg font-semibold mb-3">Upload a document</h2>
      <div
        onDrop={(e) => {
          e.preventDefault();
          const f = e.dataTransfer.files?.[0];
          if (f) setFile(f);
        }}
        onDragOver={(e) => e.preventDefault()}
        className="border-2 border-dashed rounded p-6 mb-3 bg-white"
      >
        <p className="text-sm text-slate-600">Drag & drop a file here, or click to choose</p>
        <input
          type="file"
          accept=".pdf,.doc,.docx,.png,.jpg,.jpeg,.txt"
          className="mt-3"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </div>
      {file && <p className="text-sm mb-3">Selected: <strong>{file.name}</strong> ({Math.round((file.size/1024/1024)*10)/10} MB)</p>}
      <button
        disabled={!file || isPending}
        onClick={() => mutate()}
        className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700 disabled:opacity-50"
      >
        {isPending ? 'Uploading…' : 'Upload'}
      </button>

      {isPending && (
        <div className="mt-4">
          <div className="h-2 bg-slate-200 rounded">
            <div className="h-2 bg-brand-600 rounded" style={{ width: `${progress}%` }} />
          </div>
          <p className="text-xs mt-1 text-slate-600">{progress}%</p>
        </div>
      )}
    </div>
  );
}
```

**List**

The backend should expose GET /api/documents?page=&size=&sort=createdAt,desc. If not present, add it.

src/pages/Documents/DocumentListPage.tsx
```
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { DocumentAPI } from '../../lib/api';
import type { DocumentDto } from '../../lib/types';
import StatusBadge from '../../components/StatusBadge';

export default function DocumentListPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['documents'],
    queryFn: async () => (await DocumentAPI.list()).data as DocumentDto[],
  });

  if (isLoading) return <p>Loading…</p>;

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-lg font-semibold">Your documents</h2>
        <Link to="/app/upload" className="rounded border px-3 py-1 hover:bg-slate-50">Upload</Link>
      </div>
      <div className="bg-white border rounded overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th className="text-left p-3">File</th>
              <th className="text-left p-3">Status</th>
              <th className="text-left p-3">Classification</th>
              <th className="text-left p-3">Updated</th>
            </tr>
          </thead>
          <tbody>
            {data?.map(d => (
              <tr key={d.id} className="border-t hover:bg-slate-50">
                <td className="p-3"><Link className="text-brand-700 hover:underline" to={`/app/documents/${d.id}`}>{d.filename}</Link></td>
                <td className="p-3"><StatusBadge status={d.status} /></td>
                <td className="p-3">{d.classificationName ?? <span className="text-slate-400 italic">undefined</span>}</td>
                <td className="p-3">{d.updatedAt ? new Date(d.updatedAt).toLocaleString() : '-'}</td>
              </tr>
            ))}
            {(!data || data.length === 0) && (
              <tr><td className="p-4 text-slate-500" colSpan={4}>No documents yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

Details with summary & extracted data (auto‑poll if still processing)

src/pages/Documents/DocumentDetailPage.tsx
```
import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { DocumentAPI } from '../../lib/api';
import type { DocumentDto, ExtractedDataPoint } from '../../lib/types';
import StatusBadge from '../../components/StatusBadge';

export default function DocumentDetailPage() {
  const { id } = useParams();
  const docId = Number(id);

  const docQuery = useQuery({
    queryKey: ['document', docId],
    queryFn: async () => (await DocumentAPI.get(docId)).data as DocumentDto,
    refetchInterval: (data) => (data?.status === 'PROCESSING' ? 1500 : false),
  });

  const extractedQuery = useQuery({
    queryKey: ['extracted', docId],
    queryFn: async () => (await DocumentAPI.extracted(docId)).data as ExtractedDataPoint[],
    enabled: !!docQuery.data && docQuery.data.status === 'COMPLETED',
  });

  if (docQuery.isLoading) return <p>Loading…</p>;
  if (!docQuery.data) return <p>Not found</p>;
  const doc = docQuery.data;

  return (
    <div className="space-y-4">
      <div className="bg-white border rounded p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-lg font-semibold">{doc.filename}</h2>
            <p className="text-sm text-slate-600">ID #{doc.id}</p>
          </div>
          <StatusBadge status={doc.status} />
        </div>
        <div className="mt-3 text-sm text-slate-700">
          <p><strong>Classification:</strong> {doc.classificationName ?? <em className="text-slate-400">undefined</em>}</p>
          <p><strong>Updated:</strong> {doc.updatedAt ? new Date(doc.updatedAt).toLocaleString() : '-'}</p>
        </div>
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-2">Summary</h3>
        {doc.summary ? <p className="text-slate-700 leading-relaxed">{doc.summary}</p>
          : <p className="text-slate-500 italic">No summary available.</p>}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Extracted data</h3>
        {doc.status !== 'COMPLETED' ? (
          <p className="text-slate-600">Waiting for processing to complete…</p>
        ) : extractedQuery.isLoading ? (
          <p>Loading…</p>
        ) : extractedQuery.data?.length ? (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Key</th>
                  <th className="text-left p-2">Value</th>
                  <th className="text-left p-2">Confidence</th>
                </tr>
              </thead>
              <tbody>
                {extractedQuery.data.map(x => (
                  <tr key={x.id} className="border-t">
                    <td className="p-2 font-medium">{x.key}</td>
                    <td className="p-2">{x.valueString ?? x.valueNumber ?? x.valueDate ?? ''}</td>
                    <td className="p-2">{x.confidence != null ? (x.confidence * 100).toFixed(0) + '%' : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-slate-500 italic">No data points extracted.</p>
        )}
      </div>
    </div>
  );
}
```



### 9. Admin: CRUD for classifications, patterns & data points

List + create classifications

src/pages/Admin/ClassificationsPage.tsx
```
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AdminAPI } from '../../lib/api';
import type { Classification } from '../../lib/types';
import { useForm } from 'react-hook-form';

export default function ClassificationsPage() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['classifications'],
    queryFn: async () => (await AdminAPI.listClassifications()).data as Classification[],
  });

  const { register, handleSubmit, reset } = useForm<{name: string; description?: string; priority?: number; threshold?: number}>({
    defaultValues: { priority: 0, threshold: 0.5 },
  });

  const createMut = useMutation({
    mutationFn: (payload: any) => AdminAPI.createClassification(payload),
    onSuccess: () => { toast.success('Classification created'); qc.invalidateQueries({ queryKey: ['classifications'] }); reset(); },
    onError: () => toast.error('Failed to create'),
  });

  const deleteMut = useMutation({
    mutationFn: (id: number) => AdminAPI.deleteClassification(id),
    onSuccess: () => { toast.success('Deleted'); qc.invalidateQueries({ queryKey: ['classifications'] }); },
    onError: () => toast.error('Failed to delete'),
  });

  return (
    <div className="space-y-6">
      <div className="bg-white border rounded p-4">
        <h2 className="text-lg font-semibold mb-3">Classifications</h2>
        {isLoading ? <p>Loading…</p> : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50">
                <tr>
                  <th className="text-left p-2">Name</th>
                  <th className="text-left p-2">Priority</th>
                  <th className="text-left p-2">Threshold</th>
                  <th className="text-left p-2">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data?.map(c => (
                  <tr key={c.id} className="border-t">
                    <td className="p-2 font-medium">{c.name}</td>
                    <td className="p-2">{c.priority}</td>
                    <td className="p-2">{c.threshold}</td>
                    <td className="p-2 space-x-2">
                      <Link to={`/app/admin/classifications/${c.id}/patterns`} className="text-brand-700 hover:underline">Patterns</Link>
                      <Link to={`/app/admin/classifications/${c.id}/data-points`} className="text-brand-700 hover:underline">Data points</Link>
                      {c.name !== 'undefined' && (
                        <button onClick={() => deleteMut.mutate(c.id)} className="text-rose-700 hover:underline">Delete</button>
                      )}
                    </td>
                  </tr>
                ))}
                {(!data || data.length === 0) && (
                  <tr><td className="p-3 text-slate-500" colSpan={4}>No classifications</td></tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <div className="bg-white border rounded p-4">
        <h3 className="font-medium mb-3">Create new classification</h3>
        <form onSubmit={handleSubmit((v) => createMut.mutate(v))} className="grid sm:grid-cols-2 gap-3">
          <div>
            <label className="block text-sm mb-1">Name</label>
            <input {...register('name', { required: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm mb-1">Priority</label>
            <input type="number" {...register('priority', { valueAsNumber: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div>
            <label className="block text-sm mb-1">Threshold</label>
            <input step="0.05" type="number" {...register('threshold', { valueAsNumber: true })} className="w-full border rounded px-3 py-2" />
          </div>
          <div className="sm:col-span-2">
            <label className="block text-sm mb-1">Description</label>
            <textarea {...register('description')} className="w-full border rounded px-3 py-2" rows={3} />
          </div>
          <div className="sm:col-span-2">
            <button className="rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700">Create</button>
          </div>
        </form>
      </div>
    </div>
  );
}
```

PatternsPage.tsx and DataPointsPage.tsx follow the same pattern:
- Fetch items for a given classificationId from route params.
- Show a table (pattern / flags; or key / label / type / ruleType / expression / required).
- Provide create forms and delete buttons.
- For Data Points, use selects for type and ruleType with allowed enum values.

**Patterns quick-create form fields:** pattern (textarea or input), flags (e.g., (?i) or i if you standardize).

**Data point quick-create fields:** key, label, type (select), ruleType (select), expression (textarea), required (checkbox).

Optional: add a “Test pattern” box on PatternsPage where an admin pastes sample text and regex; run new RegExp(pattern, flags) on the client to preview matches (with caveat that Java vs JS regex differ slightly).



### 10) UX & design system notes

- Color scheme: white surfaces, slate text (text-slate-700), subtle borders (border-slate-200), indigo accent (brand-600/700).
- Typography: default system font; titles ~text-lg font-semibold.
- Density: compact tables (text-sm), comfortable forms (py-2 inputs).
- Buttons: primary (brand), secondary (bordered), destructive (rose).
- Mobile-first:
    - forms full-width
    - tables scroll horizontally within a bordered card (overflow-x-auto)
    - grid → single column on small screens (sm:grid-cols-2 etc)

- Accessibility:
    - label every input
    - ensure focus-visible ring (done via default outline + --ring concept)
    - color contrast (indigo on white meets WCAG AA)


### 11. Error handling & toasts

- Use react-hot-toast for feedback: success on create/upload; concise error messages for failed API calls.
- Axios interceptor will drop tokens on failed refresh; redirect flows handled by ProtectedRoute (navigates to /auth/login when no token).

### 12. Edge cases & states to cover

-Undefined classification: show “undefined” in a muted style; extracted table may be empty.
- Processing: on details page, auto-poll until COMPLETED.
- Large files: show upload progress; disable button while uploading.
- Auth errors: invalid credentials → toast; expired token → forced re-auth when refresh fails.
- Admin safety: do not allow deletion of the reserved undefined classification (UI already hides the delete button).

### 13. Minimal test plan (manual)

1. Register → Login: Can create a user and authenticate; token stored; navbar shows email.
2. Forgot/Reset: Submit email; visit reset page with token → set new password.
3. Change password: Logged-in user can update password.
4. Upload: Upload PDF; redirected to details; see status → summary & extracted data.
5. List: Uploaded doc appears in Documents list, sorted by updated date.
6. Admin:
    - Create classification, add patterns, add data points.
    - Upload a matching doc → classified accordingly; extracted values visible.
    - Delete pattern / data point; cannot delete undefined.

### 14. Deployment notes

- Build: npm run build → static dist/ served by any static host (Netlify, S3+CloudFront, Nginx).
- Set VITE_API_BASE_URL per environment (dev/stage/prod).
- Ensure backend CORS allows the UI origin and Authorization header.


### 15) Nice-to-haves (post‑MVP)

- Inline pattern tester in Admin.
- Advanced filtering on Documents (status, classification).
- CSV export of extracted data for a document or across documents.
- Dark mode (simple Tailwind dark: classes).
- SSE or WebSocket for real‑time processing status updates (instead of polling).


### 16. What the backend must expose (to match this UI)

- /api/auth/*: register, login, refresh, forgot, reset, and change (current → new password).
- /api/documents GET (list, paginated), POST (upload).
- /api/documents/{id}, /api/documents/{id}/extracted.
- /api/classifications CRUD, /patterns, /datapoints endpoints (as specified earlier).
- If any endpoint differs, adjust the AdminAPI/DocumentAPI functions accordingly.


### 17. Visual style cheatsheet (Tailwind)

- Headings: text-lg font-semibold
- Card: bg-white border rounded p-4
- Primary button: rounded bg-brand-600 text-white px-4 py-2 hover:bg-brand-700
- Secondary button: rounded border px-3 py-1 hover:bg-slate-50
- Muted text: text-slate-500
- Inputs: border rounded px-3 py-2 focus:outline-none focus:ring-[var(--ring)]
