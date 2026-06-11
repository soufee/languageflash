import axios, { AxiosError } from 'axios';

const api = axios.create({ baseURL: '/api/v1' });

let accessToken: string | null = localStorage.getItem('accessToken');
let refreshToken: string | null = localStorage.getItem('refreshToken');

export function setTokens(access: string | null, refresh: string | null) {
  accessToken = access;
  refreshToken = refresh;
  if (access) localStorage.setItem('accessToken', access);
  else localStorage.removeItem('accessToken');
  if (refresh) localStorage.setItem('refreshToken', refresh);
  else localStorage.removeItem('refreshToken');
}

export function hasTokens() {
  return !!accessToken;
}

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

let refreshing: Promise<void> | null = null;

async function doRefresh() {
  const res = await axios.post('/api/v1/auth/refresh', { refreshToken });
  setTokens(res.data.accessToken, res.data.refreshToken);
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original: any = error.config;
    if (error.response?.status === 401 && refreshToken && !original._retried) {
      original._retried = true;
      try {
        refreshing = refreshing ?? doRefresh();
        await refreshing;
        refreshing = null;
        return api(original);
      } catch {
        refreshing = null;
        setTokens(null, null);
        window.dispatchEvent(new Event('auth:logout'));
      }
    }
    return Promise.reject(error);
  }
);

export default api;

export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const data = e.response?.data as any;
    return data?.message || data?.error || e.message;
  }
  return String(e);
}

export function errorCode(e: unknown): string | null {
  if (axios.isAxiosError(e)) {
    return (e.response?.data as any)?.error ?? null;
  }
  return null;
}
