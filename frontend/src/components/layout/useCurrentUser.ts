import { useMemo } from "react";

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

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const segment = token.split(".")[1];
    if (!segment) return null;
    const base64 = segment.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(base64);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/**
 * Best-effort current-user resolver.
 * - Tries to decode `access_token` JWT claims (sub / name / role / email).
 * - Falls back to a clearly-labelled demo identity so the header
 *   never renders empty in dev / when the backend is unreachable.
 */
export function useCurrentUser(): CurrentUser {
  return useMemo(() => {
    try {
      const token = localStorage.getItem("access_token");
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
        return {
          name: "HIMS User",
          username: "hims-user",
          role: "Staff",
          email: "",
          initials: "HU",
          authenticated: true,
        };
      }
    } catch {
      /* localStorage unavailable (SSR/tests) — fall through to demo */
    }
    return {
      name: "Demo User",
      username: "demo",
      role: "Administrator",
      email: "demo.user@deloitte.com",
      initials: "DU",
      authenticated: false,
    };
  }, []);
}

export function signOut(navigate: (path: string) => void) {
  try {
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
  } catch {
    /* ignore */
  }
  navigate("/auth/login");
}
