import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { GrievanceBoard } from "./GrievanceBoard";

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

describe("GrievanceBoard", () => {
  it("renders loading state initially", () => {
    fetchMock.mockReturnValue(new Promise(() => {})); // never resolves
    render(<GrievanceBoard />, { wrapper: wrapper() });
    expect(screen.getByText(/loading grievance board/i)).toBeTruthy();
  });

  it("renders kanban lanes from ticket data", async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes("/grievance-severities")) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [
            { id: 1, code: "LOW", description: "Low" },
            { id: 2, code: "MEDIUM", description: "Medium" },
          ],
        });
      }
      if (url.includes("/grievances/counts")) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({ TOTAL: 3, NEW: 1, ASSIGNED: 1, RESOLVED: 1, RAISED_BY_CITIZEN: 2, MY_LIST: 0, TODAYS_TICKET: 3 }),
        });
      }
      if (url.includes("/grievances")) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => [
            {
              id: 1, ticketNo: "IHMS/G/2025/00001", issueDate: "2025-01-01T09:00:00Z",
              name: "Alice", categoryId: 1, issue: "Test issue 1",
              issueStatus: 1, severityId: 1, isCitizen: true,
            },
            {
              id: 2, ticketNo: "IHMS/G/2025/00002", issueDate: "2025-01-01T10:00:00Z",
              name: "Bob", categoryId: 2, issue: "Test issue 2",
              issueStatus: 2, severityId: 3, isCitizen: true,
            },
            {
              id: 3, ticketNo: "IHMS/G/2025/00003", issueDate: "2025-01-01T11:00:00Z",
              name: "Carol", categoryId: 1, issue: "Test issue 3",
              issueStatus: 3, severityId: 2, isCitizen: false,
            },
          ],
        });
      }
      return Promise.resolve({ ok: true, status: 200, json: async () => [] });
    });

    render(<GrievanceBoard />, { wrapper: wrapper() });

    expect(await screen.findByText("Grievance Board")).toBeTruthy();
    expect(await screen.findByText("New")).toBeTruthy();
    expect(await screen.findByText("Assigned")).toBeTruthy();
    expect(await screen.findByText("Resolved")).toBeTruthy();
    expect(await screen.findByText("IHMS/G/2025/00001")).toBeTruthy();
    expect(await screen.findByText("LOW")).toBeTruthy();
    expect(await screen.findByText("HIGH")).toBeTruthy();
  });

  it("shows empty state when no tickets", async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes("/grievance-severities")) {
        return Promise.resolve({ ok: true, status: 200, json: async () => [] });
      }
      if (url.includes("/grievances/counts")) {
        return Promise.resolve({
          ok: true, status: 200,
          json: async () => ({ TOTAL: 0, NEW: 0, ASSIGNED: 0, RESOLVED: 0, RAISED_BY_CITIZEN: 0, MY_LIST: 0, TODAYS_TICKET: 0 }),
        });
      }
      return Promise.resolve({ ok: true, status: 200, json: async () => [] });
    });

    render(<GrievanceBoard />, { wrapper: wrapper() });
    expect(await screen.findByText("No tickets")).toBeTruthy();
  });

  it("renders dashboard counts", async () => {
    fetchMock.mockImplementation((url: string) => {
      if (url.includes("/grievance-severities")) {
        return Promise.resolve({ ok: true, status: 200, json: async () => [] });
      }
      if (url.includes("/grievances/counts")) {
        return Promise.resolve({
          ok: true, status: 200,
          json: async () => ({ TOTAL: 42, NEW: 5, ASSIGNED: 10, RESOLVED: 27, RAISED_BY_CITIZEN: 30, MY_LIST: 3, TODAYS_TICKET: 8 }),
        });
      }
      return Promise.resolve({ ok: true, status: 200, json: async () => [] });
    });

    render(<GrievanceBoard />, { wrapper: wrapper() });
    expect(await screen.findByText("42")).toBeTruthy();
    expect(await screen.findByText("Total")).toBeTruthy();
  });
});
