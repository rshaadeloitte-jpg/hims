import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ReportViewer } from "./ReportViewer";


vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useExecuteReport: vi.fn(),
    parseReportResult: actual.parseReportResult,
  };
});

const { useExecuteReport } = await import("./queries");

const mockMutateAsync = vi.fn();

function renderViewer() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ReportViewer />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({
    executionId: 1,
    reportId: 1,
    status: 1,
    rowCount: 2,
    result: JSON.stringify([
      { date: "2025-09-01", count: 42 },
      { date: "2025-09-02", count: 58 },
    ]),
    executedAt: "2025-09-11T10:00:00",
  });
  (useExecuteReport as ReturnType<typeof vi.fn>).mockReturnValue({
    mutateAsync: mockMutateAsync,
    isPending: false,
  });
});

describe("ReportViewer", () => {
  it("renders the filter form", () => {
    renderViewer();
    expect(screen.getByText("Report Viewer")).toBeInTheDocument();
    expect(screen.getByLabelText("Report Code")).toBeInTheDocument();
    expect(screen.getByLabelText("From")).toBeInTheDocument();
    expect(screen.getByLabelText("To")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Run Report/ })).toBeInTheDocument();
  });

  it("shows error when running without selecting a report", async () => {
    renderViewer();
    fireEvent.click(screen.getByRole("button", { name: /Run Report/ }));
    expect(await screen.findByText(/Select a report first/)).toBeInTheDocument();
  });

  it("executes report and renders result table", async () => {
    renderViewer();
    const codeInput = screen.getByLabelText("Report Code");
    fireEvent.change(codeInput, { target: { value: "opd_summary" } });
    fireEvent.click(screen.getByRole("button", { name: /Run Report/ }));

    await waitFor(() => {
      expect(mockMutateAsync).toHaveBeenCalledWith(
        expect.objectContaining({ code: "opd_summary" }),
      );
    });

    expect(await screen.findByText("Result")).toBeInTheDocument();
    expect(screen.getByText("date")).toBeInTheDocument();
    expect(screen.getByText("count")).toBeInTheDocument();
    expect(screen.getByText("2025-09-01")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
  });

  it("renders Export CSV button when rows are present", async () => {
    renderViewer();
    const codeInput = screen.getByLabelText("Report Code");
    fireEvent.change(codeInput, { target: { value: "opd_summary" } });
    fireEvent.click(screen.getByRole("button", { name: /Run Report/ }));

    expect(await screen.findByText("Export CSV")).toBeInTheDocument();
  });

  it("shows error when execution fails", async () => {
    mockMutateAsync.mockRejectedValue(new Error("Execution failed"));
    renderViewer();
    const codeInput = screen.getByLabelText("Report Code");
    fireEvent.change(codeInput, { target: { value: "bad_report" } });
    fireEvent.click(screen.getByRole("button", { name: /Run Report/ }));

    expect(await screen.findByRole("alert")).toHaveTextContent(/Execution failed/);
  });
});
