import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ReportCatalog } from "./ReportCatalog";

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useReports: vi.fn(),
  };
});

const { useReports } = await import("./queries");

const MOCK_REPORTS = [
  { reportId: 1, reportCode: "opd_summary", title: "OPD Summary", kind: 1, definition: "{}", isActive: true },
  { reportId: 2, reportCode: "ipd_daily", title: "IPD Daily", kind: 1, definition: "{}", isActive: true },
  { reportId: 3, reportCode: "revenue_total", title: "Revenue Total", kind: 2, definition: "{}", isActive: false },
  { reportId: 4, reportCode: "lab_orders", title: "Lab Orders", kind: 1, definition: "{}", isActive: true },
];

function renderCatalog() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ReportCatalog />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useReports as ReturnType<typeof vi.fn>).mockReturnValue({
    data: MOCK_REPORTS,
    isLoading: false,
    isError: false,
    error: null,
  });
});

describe("ReportCatalog", () => {
  it("renders the catalog header", () => {
    renderCatalog();
    expect(screen.getByText("Report Catalog")).toBeInTheDocument();
    expect(screen.getByText("Browse and run available reports — M25 Reporting Module.")).toBeInTheDocument();
  });

  it("renders search input and active-only filter", () => {
    renderCatalog();
    expect(screen.getByPlaceholderText("Search by code or title…")).toBeInTheDocument();
    expect(screen.getByLabelText("Active only")).toBeInTheDocument();
  });

  it("renders only active reports by default", () => {
    renderCatalog();
    // 3 of 4 mock reports are active
    expect(screen.getByText("opd_summary")).toBeInTheDocument();
    expect(screen.getByText("ipd_daily")).toBeInTheDocument();
    expect(screen.getByText("lab_orders")).toBeInTheDocument();
    // Inactive report should not be visible
    expect(screen.queryByText("revenue_total")).not.toBeInTheDocument();
  });

  it("shows correct count text", () => {
    renderCatalog();
    expect(screen.getByText("Showing 3 of 4 reports.")).toBeInTheDocument();
  });

  it("filters reports by search term", () => {
    renderCatalog();
    const searchInput = screen.getByPlaceholderText("Search by code or title…");
    fireEvent.change(searchInput, { target: { value: "opd" } });
    expect(screen.getByText("opd_summary")).toBeInTheDocument();
    expect(screen.queryByText("ipd_daily")).not.toBeInTheDocument();
    expect(screen.getByText("Showing 1 of 4 reports.")).toBeInTheDocument();
  });

  it("shows inactive reports when active-only is unchecked", () => {
    renderCatalog();
    const activeCheckbox = screen.getByLabelText("Active only");
    fireEvent.click(activeCheckbox);
    // Now all 4 reports should be visible
    expect(screen.getByText("opd_summary")).toBeInTheDocument();
    expect(screen.getByText("ipd_daily")).toBeInTheDocument();
    expect(screen.getByText("revenue_total")).toBeInTheDocument();
    expect(screen.getByText("lab_orders")).toBeInTheDocument();
    expect(screen.getByText("Showing 4 of 4 reports.")).toBeInTheDocument();
  });

  it("renders Run and History action links for each report", () => {
    renderCatalog();
    const runButtons = screen.getAllByText("Run");
    expect(runButtons.length).toBe(3); // 3 active reports
    const historyButtons = screen.getAllByText("History");
    expect(historyButtons.length).toBe(3);
  });

  it("shows loading state", () => {
    (useReports as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: true,
      isError: false,
      error: null,
    });
    renderCatalog();
    expect(screen.getByText("Loading reports…")).toBeInTheDocument();
  });

  it("shows error state", () => {
    (useReports as ReturnType<typeof vi.fn>).mockReturnValue({
      data: undefined,
      isLoading: false,
      isError: true,
      error: new Error("Failed to fetch"),
    });
    renderCatalog();
    expect(screen.getByRole("alert")).toHaveTextContent(/Failed to load reports/);
  });

  it("shows empty state when no reports match", () => {
    (useReports as ReturnType<typeof vi.fn>).mockReturnValue({
      data: [],
      isLoading: false,
      isError: false,
      error: null,
    });
    renderCatalog();
    expect(screen.getByText("No reports match your search.")).toBeInTheDocument();
  });
});
