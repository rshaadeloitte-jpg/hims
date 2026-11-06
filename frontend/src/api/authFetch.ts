import { getAccessToken, isSessionValid } from "../features/auth/session";

const SKIP_PATHS = new Set(["/api/v1/auth/login", "/api/v1/auth/refresh"]);

let installed = false;
let originalFetch: typeof window.fetch | null = null;

export function installAuthFetch(): void {
  if (installed) return;
  if (typeof window === "undefined" || typeof window.fetch !== "function") return;
  installed = true;
  originalFetch = window.fetch.bind(window);

  window.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const fetchImpl = originalFetch as typeof window.fetch;
    if (!isSessionValid()) {
      return fetchImpl(input, init);
    }

    const token = getAccessToken();
    if (!token) {
      return fetchImpl(input, init);
    }

    let url: URL;
    try {
      if (input instanceof Request) {
        url = new URL(input.url);
      } else if (input instanceof URL) {
        url = new URL(input.href);
      } else {
        url = new URL(String(input), window.location.origin);
      }
    } catch {
      return fetchImpl(input, init);
    }

    if (url.origin !== window.location.origin) {
      return fetchImpl(input, init);
    }
    if (!url.pathname.startsWith("/api/")) {
      return fetchImpl(input, init);
    }
    if (SKIP_PATHS.has(url.pathname)) {
      return fetchImpl(input, init);
    }

    const headers = new Headers(init?.headers ?? (input instanceof Request ? input.headers : undefined));
    if (headers.has("Authorization")) {
      return fetchImpl(input, init);
    }
    headers.set("Authorization", `Bearer ${token}`);
    return fetchImpl(input, { ...init, headers });
  }) as typeof window.fetch;
}
