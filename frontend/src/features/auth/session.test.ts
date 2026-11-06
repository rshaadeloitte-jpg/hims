import { beforeEach, describe, expect, it } from 'vitest';
import {
  clearSession,
  decodeJwtPayload,
  getAccessToken,
  isSessionValid,
  persistToken,
} from './session';

function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

describe('session utils', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    document.cookie = 'access_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    document.cookie = 'auth_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  describe('getAccessToken', () => {
    it('reads access_token from localStorage first', () => {
      localStorage.setItem('access_token', 'local_token');
      sessionStorage.setItem('access_token', 'session_token');
      document.cookie = 'access_token=cookie_token; path=/';

      expect(getAccessToken()).toBe('local_token');
    });

    it('falls back to auth_token from localStorage', () => {
      localStorage.setItem('auth_token', 'local_auth_token');
      sessionStorage.setItem('access_token', 'session_token');

      expect(getAccessToken()).toBe('local_auth_token');
    });

    it('reads access_token from sessionStorage when localStorage is empty', () => {
      sessionStorage.setItem('access_token', 'session_token');
      document.cookie = 'access_token=cookie_token; path=/';

      expect(getAccessToken()).toBe('session_token');
    });

    it('reads auth_token from sessionStorage when access_token is absent', () => {
      sessionStorage.setItem('auth_token', 'session_auth_token');

      expect(getAccessToken()).toBe('session_auth_token');
    });

    it('reads access_token from document.cookie when storages are empty', () => {
      document.cookie = 'access_token=cookie_token; path=/';

      expect(getAccessToken()).toBe('cookie_token');
    });

    it('returns null when no token is present anywhere', () => {
      expect(getAccessToken()).toBeNull();
    });
  });

  describe('isSessionValid', () => {
    it('returns false when no token is present', () => {
      expect(isSessionValid()).toBe(false);
    });

    it('returns false when token cannot be decoded', () => {
      localStorage.setItem('access_token', 'invalid-token-string');
      expect(isSessionValid()).toBe(false);
    });

    it('returns false when token has no exp claim', () => {
      const token = makeJwt({ sub: 'user123', name: 'Test User' });
      localStorage.setItem('access_token', token);
      expect(isSessionValid()).toBe(false);
    });

    it('returns false when token exp is in the past', () => {
      const pastExp = Math.floor(Date.now() / 1000) - 60;
      const token = makeJwt({ sub: 'user123', exp: pastExp });
      localStorage.setItem('access_token', token);
      expect(isSessionValid()).toBe(false);
    });

    it('returns false when token exp is not a number', () => {
      const token = makeJwt({ sub: 'user123', exp: 'tomorrow' });
      localStorage.setItem('access_token', token);
      expect(isSessionValid()).toBe(false);
    });

    it('returns true when token has a future exp claim', () => {
      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const token = makeJwt({ sub: 'user123', exp: futureExp });
      localStorage.setItem('access_token', token);
      expect(isSessionValid()).toBe(true);
    });
  });

  describe('clearSession', () => {
    it('removes tokens from localStorage, sessionStorage and cookie', () => {
      persistToken(makeJwt({ sub: 'u1', exp: Math.floor(Date.now() / 1000) + 100 }));
      localStorage.setItem('auth_token', 'legacy_local');
      sessionStorage.setItem('auth_token', 'legacy_session');

      expect(getAccessToken()).not.toBeNull();

      clearSession();

      expect(localStorage.getItem('access_token')).toBeNull();
      expect(localStorage.getItem('auth_token')).toBeNull();
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(sessionStorage.getItem('auth_token')).toBeNull();
      expect(getAccessToken()).toBeNull();
    });
  });

  describe('decodeJwtPayload', () => {
    it('handles base64url encoding and padding correctly', () => {
      const payload = { sub: 'abc_123-xyz', role: 'admin' };
      const token = makeJwt(payload);
      expect(decodeJwtPayload(token)).toEqual(payload);
    });

    it('returns null for non-JWT strings', () => {
      expect(decodeJwtPayload('not.a.valid.jwt.string')).toBeNull();
      expect(decodeJwtPayload('')).toBeNull();
    });
  });
});
