import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { QueueBoard } from "../QueueBoard";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("QueueBoard", () => {
  it("renders loading state initially", () => {
    fetchMock.mockReturnValue(new Promise(() => {})); // never resolves
    render(<QueueBoard />, { wrapper: wrapper() });
    expect(screen.getByText(/loading queue board/i)).toBeTruthy();
  });

  it("renders counters from dashboard data", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [
        {
          counterId: 1,
          counterName: "OPD-1",
          nowServing: { tokenId: 1, tokenNo: 42, patientId: 7, counterId: 1, priority: "NORMAL", status: 2, createdAt: "2027-01-01T09:00:00Z" },
          waiting: [
            { tokenId: 2, tokenNo: 43, patientId: 8, counterId: 1, priority: "EMERGENCY", status: 1, createdAt: "2027-01-01T09:05:00Z" },
          ],
          called: [],
        },
      ],
    });

    render(<QueueBoard />, { wrapper: wrapper() });

    // Wait for data
    expect(await screen.findByText("OPD-1")).toBeTruthy();
    expect(await screen.findByText("#42")).toBeTruthy();
    expect(await screen.findByText("#43")).toBeTruthy();
    expect(screen.getByText("Queue Board")).toBeTruthy();
  });

  it("shows empty state when no counters", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });

    render(<QueueBoard />, { wrapper: wrapper() });
    expect(await screen.findByText(/no active counters/i)).toBeTruthy();
  });
});