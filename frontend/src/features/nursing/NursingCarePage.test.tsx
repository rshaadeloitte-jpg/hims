import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { NursingCarePage } from "./NursingCarePage";

const fetchMock = vi.fn();

function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/nursing"]}>
        <NursingCarePage />
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

describe("NursingCarePage", () => {
  it("renders empty states without firing admissionId-less queries", async () => {
    renderPage();
    expect(await screen.findByText("Nursing — Care Plans & Handovers")).toBeInTheDocument();
    expect(screen.getByText("No care plans.")).toBeInTheDocument();
    expect(screen.getByText("No handovers.")).toBeInTheDocument();
    // no care-plans/shift-handovers fetches without admissionId
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => u.includes("/care-plans"))).toBe(false);
    expect(urls.some((u) => u.includes("/shift-handovers"))).toBe(false);
  });
});
