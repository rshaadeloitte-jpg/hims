import { useMemo } from "react";
import {
  clearSession,
  decodeJwtPayload,
  getAccessToken,
  isSessionValid,
} from "../../features/auth/session";

export interface CurrentUser {
  name: string;
  username: string;
  role: string;
  email: string;
  initials: string;
  authenticated: boolean;
}

function initialsOf(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "U";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/**
 * Best-effort current-user resolver.
 * - Decodes `access_token` JWT claims if the session is valid.
 * - Falls back to a harmless guest default (authenticated: false) when signed out.
 */
export function useCurrentUser(): CurrentUser {
  return useMemo(() => {
    try {
      if (isSessionValid()) {
        const token = getAccessToken();
        if (token) {
          const claims = decodeJwtPayload(token);
          if (claims) {
            const name =
              (claims["name"] as string) ||
              (claims["fullName"] as string) ||
              (claims["preferred_username"] as string) ||
              (claims["sub"] as string) ||
              "HIMS User";
            const role =
              (claims["role"] as string) ||
              (Array.isArray(claims["roles"])
                ? String((claims["roles"] as string[])[0])
                : undefined) ||
              "Staff";
            const email = (claims["email"] as string) || "";
            const username =
              (claims["preferred_username"] as string) ||
              (claims["sub"] as string) ||
              name;
            return {
              name: String(name),
              username: String(username),
              role: String(role),
              email: String(email),
              initials: initialsOf(String(name)),
              authenticated: true,
            };
          }
        }
      }
    } catch {
      /* storage unavailable (SSR/tests) — fall through to harmless default */
    }
    return {
      name: "Demo User",
      username: "demo",
      role: "Guest",
      email: "",
      initials: "DU",
      authenticated: false,
    };
  }, []);
}

export function signOut(navigate?: (path: string) => void) {
  clearSession();
  if (navigate) {
    navigate("/auth/login");
  } else {
    window.location.href = "/auth/login";
  }
}
