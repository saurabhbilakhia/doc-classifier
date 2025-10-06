import axios, { AxiosError } from 'axios';
import type { AxiosInstance } from 'axios';
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
  change: (payload: { currentPassword: string; newPassword: string }) => api.post('/api/auth/change', payload),
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
  list: (page = 0, size = 20) => api.get(`/api/documents`, { params: { page, size, sort: 'createdAt,desc' } }),
  get: (id: number) => api.get(`/api/documents/${id}`),
  extracted: (id: number) => api.get(`/api/documents/${id}/extracted`),
};

export const AdminAPI = {
  listClassifications: () => api.get('/api/admin/classifications'),
  createClassification: (payload: { name: string; description?: string; priority?: number; threshold?: number }) =>
    api.post('/api/admin/classifications', payload),
  updateClassification: (id: number, payload: any) => api.put(`/api/admin/classifications/${id}`, payload),
  deleteClassification: (id: number) => api.delete(`/api/admin/classifications/${id}`),

  listPatterns: (classificationId: number) => api.get(`/api/admin/classifications/${classificationId}/patterns`),
  addPatterns: (classificationId: number, payload: { pattern: string; flags?: string }[]) =>
    api.post(`/api/admin/classifications/${classificationId}/patterns`, payload),
  deletePattern: (classificationId: number, patternId: number) =>
    api.delete(`/api/admin/classifications/${classificationId}/patterns/${patternId}`),

  listDataPoints: (classificationId: number) => api.get(`/api/admin/classifications/${classificationId}/datapoints`),
  createDataPoint: (classificationId: number, payload: any) =>
    api.post(`/api/admin/classifications/${classificationId}/datapoints`, payload),
  updateDataPoint: (dpId: number, payload: any) => api.put(`/api/admin/datapoints/${dpId}`, payload),
  deleteDataPoint: (dpId: number) => api.delete(`/api/admin/datapoints/${dpId}`),
};
