import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AasmaanPage } from "./AasmaanPage";

const fetchMock = vi.fn();

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/aasmaan"]}>
        <AasmaanPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("AasmaanPage", () => {
  it("does not call the removed records endpoint", async () => {
    renderPage();
    expect(await screen.findByRole("heading", { name: "Aasmaan Field Sync" })).toBeInTheDocument();
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => u.includes("/records"))).toBe(false);
    expect(screen.queryByText("Field Sync Records")).not.toBeInTheDocument();
  });
});
