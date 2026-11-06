import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from '../../App';
import { LoginPage, sanitizeRedirectPath } from './LoginPage';
import { RequireAuth } from './RequireAuth';
import { clearSession, getAccessToken } from './session';

function makeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(payload));
  return `${header}.${body}.sig`;
}

function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function LocationDisplay() {
  const location = useLocation();
  const from = location.state as { from?: { pathname: string } } | null;
  return (
    <div>
      <span data-testid="current-path">{location.pathname}</span>
      <span data-testid="from-path">{from?.from?.pathname ?? 'none'}</span>
    </div>
  );
}

describe('RequireAuth & Auth Routing', () => {
  beforeEach(() => {
    clearSession();
    localStorage.clear();
    sessionStorage.clear();
    document.cookie = 'access_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    document.cookie = 'auth_token=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    vi.restoreAllMocks();
  });

  it('no token redirects with state.from', () => {
    render(
      <MemoryRouter initialEntries={['/billing']}>
        <Routes>
          <Route
            path="/billing"
            element={
              <RequireAuth>
                <div>Protected Billing Content</div>
              </RequireAuth>
            }
          />
          <Route path="/auth/login" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.queryByText('Protected Billing Content')).not.toBeInTheDocument();
    expect(screen.getByTestId('current-path')).toHaveTextContent('/auth/login');
    expect(screen.getByTestId('from-path')).toHaveTextContent('/billing');
  });

  it('expired token redirects and clears', () => {
    const pastExp = Math.floor(Date.now() / 1000) - 300;
    const expiredToken = makeJwt({ sub: 'user1', exp: pastExp });
    localStorage.setItem('access_token', expiredToken);
    sessionStorage.setItem('access_token', expiredToken);
    document.cookie = `access_token=${expiredToken}; path=/`;

    render(
      <MemoryRouter initialEntries={['/billing']}>
        <Routes>
          <Route
            path="/billing"
            element={
              <RequireAuth>
                <div>Protected Billing Content</div>
              </RequireAuth>
            }
          />
          <Route path="/auth/login" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.queryByText('Protected Billing Content')).not.toBeInTheDocument();
    expect(screen.getByTestId('current-path')).toHaveTextContent('/auth/login');
    expect(screen.getByTestId('from-path')).toHaveTextContent('/billing');

    // Storage must be cleared
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(sessionStorage.getItem('access_token')).toBeNull();
    expect(getAccessToken()).toBeNull();
  });

  it('valid token renders', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const validToken = makeJwt({ sub: 'user1', exp: futureExp });
    localStorage.setItem('access_token', validToken);

    render(
      <MemoryRouter initialEntries={['/billing']}>
        <Routes>
          <Route
            path="/billing"
            element={
              <RequireAuth>
                <div>Protected Billing Content</div>
              </RequireAuth>
            }
          />
          <Route path="/auth/login" element={<LocationDisplay />} />
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText('Protected Billing Content')).toBeInTheDocument();
    expect(screen.queryByTestId('current-path')).not.toBeInTheDocument();
  });

  it('kiosk route renders with no token', async () => {
    const qc = createQueryClient();

    // 1. /kiosk
    const { unmount: u1 } = render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/kiosk']}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(screen.getByText('Patient Kiosk')).toBeInTheDocument();
    expect(screen.getByText('Self Registration')).toBeInTheDocument();
    u1();

    // 2. /kiosk/self-register
    const { unmount: u2 } = render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/kiosk/self-register']}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(screen.getByText('Patient Self-Registration')).toBeInTheDocument();
    u2();

    // 3. /kiosk/appointment
    const { unmount: u3 } = render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/kiosk/appointment']}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(screen.getByRole('heading', { name: 'Book Appointment' })).toBeInTheDocument();
    u3();

    // 4. /kiosk/lab-result
    const { unmount: u4 } = render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/kiosk/lab-result']}>
          <App />
        </MemoryRouter>
      </QueryClientProvider>,
    );
    expect(screen.getByText('Lab Result Upload')).toBeInTheDocument();
    u4();
  });

  describe('open redirect prevention', () => {
    it('an unsafe from such as //evil.com is ignored', () => {
      expect(sanitizeRedirectPath('//evil.com')).toBe('/');
      expect(sanitizeRedirectPath('//evil.com/phish')).toBe('/');
      expect(sanitizeRedirectPath('/\\evil.com')).toBe('/');
      expect(sanitizeRedirectPath('https://evil.com')).toBe('/');
      expect(sanitizeRedirectPath('javascript:alert(1)')).toBe('/');
      expect(sanitizeRedirectPath({ pathname: '//evil.com' })).toBe('/');
      expect(sanitizeRedirectPath(null)).toBe('/');
      expect(sanitizeRedirectPath(undefined)).toBe('/');

      // Safe internal paths are allowed
      expect(sanitizeRedirectPath('/billing')).toBe('/billing');
      expect(sanitizeRedirectPath('/m02/consult/10')).toBe('/m02/consult/10');
      expect(sanitizeRedirectPath({ pathname: '/billing', search: '?page=2' })).toBe('/billing?page=2');
    });

    it('navigates to "/" instead of unsafe //evil.com after successful login', async () => {
      const user = userEvent.setup();
      const qc = createQueryClient();

      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const validToken = makeJwt({ sub: 'user1', exp: futureExp });

      vi.spyOn(globalThis, 'fetch').mockImplementation(async (url: RequestInfo | URL) => {
        if (String(url).includes('/auth/login')) {
          return {
            ok: true,
            status: 200,
            json: async () => ({
              accessToken: validToken,
              refreshToken: 'ref-token',
            }),
          } as Response;
        }
        return { ok: true, status: 200, json: async () => ({}) } as Response;
      });

      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter
            initialEntries={[{ pathname: '/auth/login', state: { from: '//evil.com' } }]}
          >
            <Routes>
              <Route path="/auth/login" element={<LoginPage />} />
              <Route path="/" element={<div data-testid="home-destination">Home Page Target</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );

      await user.type(screen.getByLabelText(/username/i), 'admin');
      await user.type(screen.getByLabelText(/password/i), 'password123');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByTestId('home-destination')).toBeInTheDocument();
      });
    });

    it('navigates to safe internal from path after successful login', async () => {
      const user = userEvent.setup();
      const qc = createQueryClient();

      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const validToken = makeJwt({ sub: 'user1', exp: futureExp });

      vi.spyOn(globalThis, 'fetch').mockImplementation(async (url: RequestInfo | URL) => {
        if (String(url).includes('/auth/login')) {
          return {
            ok: true,
            status: 200,
            json: async () => ({
              accessToken: validToken,
              refreshToken: 'ref-token',
            }),
          } as Response;
        }
        return { ok: true, status: 200, json: async () => ({}) } as Response;
      });

      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter
            initialEntries={[{ pathname: '/auth/login', state: { from: '/billing' } }]}
          >
            <Routes>
              <Route path="/auth/login" element={<LoginPage />} />
              <Route path="/billing" element={<div data-testid="billing-destination">Billing Page Target</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>,
      );

      await user.type(screen.getByLabelText(/username/i), 'admin');
      await user.type(screen.getByLabelText(/password/i), 'password123');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByTestId('billing-destination')).toBeInTheDocument();
      });
    });
  });

  it('an already signed-in user opening /auth/login redirects to "/"', () => {
    const futureExp = Math.floor(Date.now() / 1000) + 3600;
    const validToken = makeJwt({ sub: 'user1', exp: futureExp });
    localStorage.setItem('access_token', validToken);

    const qc = createQueryClient();

    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter initialEntries={['/auth/login']}>
          <Routes>
            <Route path="/auth/login" element={<LoginPage />} />
            <Route path="/" element={<div data-testid="root-home">Root Home</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByTestId('root-home')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /sign in/i })).not.toBeInTheDocument();
  });
});
