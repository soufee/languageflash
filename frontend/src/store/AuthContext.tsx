import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import api, { setTokens, hasTokens } from '../api/client';

export interface User {
  id: number;
  email: string;
  firstName?: string;
  lastName?: string;
  emailConfirmed: boolean;
  premium: boolean;
  premiumExpiresAt?: string;
  interfaceLanguage: string;
  roles: string[];
}

interface AuthCtx {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  reload: () => Promise<void>;
}

const Ctx = createContext<AuthCtx>(null!);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    if (!hasTokens()) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const res = await api.get('/user/me');
      setUser(res.data);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    reload();
    const onLogout = () => setUser(null);
    window.addEventListener('auth:logout', onLogout);
    return () => window.removeEventListener('auth:logout', onLogout);
  }, [reload]);

  const login = useCallback(async (email: string, password: string) => {
    const res = await api.post('/auth/login', { email, password });
    setTokens(res.data.accessToken, res.data.refreshToken);
    setUser(res.data.user);
  }, []);

  const logout = useCallback(() => {
    setTokens(null, null);
    setUser(null);
  }, []);

  return <Ctx.Provider value={{ user, loading, login, logout, reload }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
