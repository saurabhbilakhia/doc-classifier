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
