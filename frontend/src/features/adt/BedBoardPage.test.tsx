import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BedBoardPage } from "./BedBoardPage";

const fetchMock = vi.fn();

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/adt"]}>
        <BedBoardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  fetchMock.mockImplementation(async (url: RequestInfo | URL) => {
    const u = String(url);
    if (u.includes("/api/v1/adt/census")) {
      return { ok: true, status: 200, json: async () => ({ wards: [], totalActiveAdmissions: 0, interimCount: 0 }) };
    }
    if (u.includes("/api/v1/beds")) {
      return { ok: true, status: 200, json: async () => [] };
    }
    if (u.includes("/api/v1/admissions")) {
      return { ok: true, status: 200, json: async () => [] };
    }
    return { ok: true, status: 200, json: async () => [] };
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("BedBoardPage", () => {
  it("renders with empty census data without crashing", async () => {
    renderPage();
    expect(await screen.findByText("ADT — Bed Board")).toBeInTheDocument();
    expect(await screen.findByText("No census data.")).toBeInTheDocument();
  });
});
