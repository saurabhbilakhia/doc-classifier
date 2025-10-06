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
