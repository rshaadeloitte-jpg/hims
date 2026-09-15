import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ReportDashboard } from "./ReportDashboard";

/**
 * Component smoke test for ReportDashboard.
 * The queries module is mocked; TanStack Query runs for real on a test client.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useDashboardCounts: vi.fn(),
    useReportExecutions: vi.fn(),
  };
});

const { useDashboardCounts, useReportExecutions } = await import("./queries");

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ReportDashboard />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useDashboardCounts as ReturnType<typeof vi.fn>).mockReturnValue({
    data: { patients: 1234, encounters: 567, revenueTotal: 890000 },
    isLoading: false,
    isError: false,
    error: null,
  });
  (useReportExecutions as ReturnType<typeof vi.fn>).mockReturnValue({
    data: [
      { executionId: 1, reportId: 1, status: 1, rowCount: 42, executedAt: "2025-09-11T10:00:00" },
      { executionId: 2, reportId: 2, status: 0, rowCount: 0, executedAt: "2025-09-11T11:00:00" },
    ],
  });
});

describe("ReportDashboard", () => {
  it("renders KPI count widgets with data", () => {
    renderDashboard();
    expect(screen.getByText("Patients Registered")).toBeInTheDocument();
    expect(screen.getByText("1,234")).toBeInTheDocument();
    expect(screen.getByText("Encounters (today)")).toBeInTheDocument();
    expect(screen.getByText("567")).toBeInTheDocument();
    expect(screen.getByText("Revenue (₹)")).toBeInTheDocument();
  });

  it("renders chart panels", () => {
    renderDashboard();
    expect(screen.getByText("Patient Registrations")).toBeInTheDocument();
    expect(screen.getByText("Encounter Volume")).toBeInTheDocument();
    expect(screen.getByText("Revenue by Department")).toBeInTheDocument();
  });

  it("renders recent executions strip", () => {
    renderDashboard();
    expect(screen.getByText("Recent Report Runs")).toBeInTheDocument();
    expect(screen.getByText("DONE")).toBeInTheDocument();
    expect(screen.getByText("QUEUED")).toBeInTheDocument();
  });

  it("shows loading state when data is loading", () => {
    (useDashboardCounts as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    });
    renderDashboard();
    // KPI values show "…" when loading
    expect(screen.getAllByText("…").length).toBeGreaterThan(0);
  });

  it("shows error state when query fails", () => {
    (useDashboardCounts as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error("Network error"),
    });
    renderDashboard();
    expect(screen.getByRole("alert")).toHaveTextContent(/Failed to load dashboard counts/);
  });

  it("renders as-of date input", () => {
    renderDashboard();
    expect(screen.getByLabelText("As of date")).toBeInTheDocument();
  });
});
