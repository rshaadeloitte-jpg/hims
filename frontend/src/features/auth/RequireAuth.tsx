import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { clearSession, isSessionValid } from './session';

export interface RequireAuthProps {
  children: ReactNode;
}

export function RequireAuth({ children }: RequireAuthProps) {
  const location = useLocation();

  if (!isSessionValid()) {
    clearSession();
    return <Navigate to="/auth/login" replace state={{ from: location }} />;
  }

  return <>{children}</>;
}
