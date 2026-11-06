import { describe, it, expect, beforeEach, vi } from "vitest";

function validToken(): string {
  const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 }));
  return `header.${payload}.signature`;
}

describe("installAuthFetch", () => {
  let fetchMock: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    vi.resetModules();
    fetchMock = vi.fn().mockResolvedValue(new Response("{}", { status: 200 }));
    vi.stubGlobal("fetch", fetchMock);
    localStorage.clear();
    sessionStorage.clear();
  });

  it("is idempotent", async () => {
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    const first = window.fetch;
    installAuthFetch();
    expect(window.fetch).toBe(first);
  });

  it("attaches Authorization to same-origin /api string requests", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/api/v1/foo");
    expect(fetchMock).toHaveBeenCalled();
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toBe(`Bearer ${localStorage.getItem("access_token")}`);
  });

  it("handles URL input", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    const url = new URL("/api/v1/foo", window.location.origin);
    await window.fetch(url);
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toContain("Bearer");
  });

  it("handles Request input", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    const req = new Request(new URL("/api/v1/foo", window.location.origin), { headers: { "X-Test": "1" } });
    await window.fetch(req);
    const call = fetchMock.mock.calls[0];
    const headers = new Headers(call[1]?.headers);
    expect(headers.get("Authorization")).toContain("Bearer");
    expect(headers.get("X-Test")).toBe("1");
  });

  it("does not add a header for cross-origin URLs", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("https://example.com/api/v1/foo");
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toBeNull();
  });

  it("does not add a header for non-/api paths", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/foo");
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toBeNull();
  });

  it("never overrides an existing Authorization header", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/api/v1/foo", { headers: { Authorization: "Bearer existing" } });
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toBe("Bearer existing");
  });

  it("does not add a header to login or refresh", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/api/v1/auth/login");
    await window.fetch("/api/v1/auth/refresh");
    for (const call of fetchMock.mock.calls) {
      expect(new Headers(call[1]?.headers).get("Authorization")).toBeNull();
    }
  });

  it("sends no header when there is no valid session", async () => {
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/api/v1/foo");
    const call = fetchMock.mock.calls[0];
    expect(new Headers(call[1]?.headers).get("Authorization")).toBeNull();
  });

  it("preserves method, body and other headers", async () => {
    localStorage.setItem("access_token", validToken());
    const { installAuthFetch } = await import("./authFetch");
    installAuthFetch();
    await window.fetch("/api/v1/foo", {
      method: "POST",
      body: JSON.stringify({ a: 1 }),
      headers: { "Content-Type": "application/json", "X-Test": "1" },
    });
    const call = fetchMock.mock.calls[0];
    expect(call[1]?.method).toBe("POST");
    expect(call[1]?.body).toBe(JSON.stringify({ a: 1 }));
    const headers = new Headers(call[1]?.headers);
    expect(headers.get("Content-Type")).toBe("application/json");
    expect(headers.get("X-Test")).toBe("1");
    expect(headers.get("Authorization")).toContain("Bearer");
  });
});
