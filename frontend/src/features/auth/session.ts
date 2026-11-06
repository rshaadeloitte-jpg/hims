/**
 * Single source of truth for authentication tokens and session validity.
 */

export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const segment = parts[1];
    if (!segment) return null;
    let base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
    const pad = base64.length % 4;
    if (pad) {
      base64 += '='.repeat(4 - pad);
    }
    const json = atob(base64);
    const parsed = JSON.parse(json);
    if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
      return parsed as Record<string, unknown>;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Reads the access token from storage / cookies in the same priority order
 * as features/auth/queries.ts:
 * 1. localStorage ("access_token" ?? "auth_token")
 * 2. sessionStorage ("access_token" ?? "auth_token")
 * 3. document.cookie ("access_token")
 */
export function getAccessToken(): string | null {
  try {
    const fromLocal = localStorage.getItem('access_token') ?? localStorage.getItem('auth_token');
    if (fromLocal) return fromLocal;
    const fromSession = sessionStorage.getItem('access_token') ?? sessionStorage.getItem('auth_token');
    if (fromSession) return fromSession;
    const m = document.cookie.match(/(?:^|;\s*)access_token=([^;]+)/);
    if (m) return decodeURIComponent(m[1]);
  } catch {
    /* storage unavailable */
  }
  return null;
}

/**
 * Returns false when:
 * - there is no token
 * - the JWT cannot be decoded
 * - it has no `exp` claim (or invalid exp)
 * - its `exp` is in the past
 */
export function isSessionValid(): boolean {
  const token = getAccessToken();
  if (!token || typeof token !== 'string' || token.trim() === '') {
    return false;
  }

  const payload = decodeJwtPayload(token);
  if (!payload) {
    return false;
  }

  if (typeof payload.exp !== 'number' || !Number.isFinite(payload.exp)) {
    return false;
  }

  const nowSeconds = Date.now() / 1000;
  if (payload.exp <= nowSeconds) {
    return false;
  }

  return true;
}

/**
 * Clears authentication tokens from localStorage, sessionStorage, and cookies.
 */
export function clearSession(): void {
  try {
    localStorage.removeItem('access_token');
    localStorage.removeItem('auth_token');
    localStorage.removeItem('refresh_token');
  } catch {
    /* ignore */
  }
  try {
    sessionStorage.removeItem('access_token');
    sessionStorage.removeItem('auth_token');
    sessionStorage.removeItem('refresh_token');
  } catch {
    /* ignore */
  }
  try {
    document.cookie = 'access_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax';
    document.cookie = 'auth_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Lax';
  } catch {
    /* ignore */
  }
}

/**
 * Persists the access token to localStorage, sessionStorage, and cookie.
 */
export function persistToken(token: string): void {
  try {
    localStorage.setItem('access_token', token);
  } catch {
    /* ignore */
  }
  try {
    sessionStorage.setItem('access_token', token);
  } catch {
    /* ignore */
  }
  try {
    document.cookie = `access_token=${encodeURIComponent(token)}; path=/; SameSite=Lax`;
  } catch {
    /* ignore */
  }
}
