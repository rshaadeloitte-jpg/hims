import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AshaWork } from "./AshaWork";

/**
 * Component smoke test for AshaWork.
 * The queries module is mocked; TanStack Query runs for real on a test client.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useAshaWorkList: vi.fn(),
    useAshaWorkers: vi.fn(),
  };
});

const queries = (await import("./queries")) as unknown as Record<
  string,
  ReturnType<typeof vi.fn>
>;

function renderAshaWork() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AshaWork />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  queries.useAshaWorkers.mockReturnValue({
    data: [
      { ashaId: 10, ashaName: "Sunita Devi" },
      { ashaId: 11, ashaName: "Meena Yadav" },
    ],
    isLoading: false,
  });
});

describe("AshaWork", () => {
  it("renders tracking type tabs and records", async () => {
    queries.useAshaWorkList.mockReturnValue({
      data: [
        {
          id: 1,
          beneficiaryId: 101,
          beneficiaryName: "Priya Sharma",
          trackingType: "ANC",
          visitDate: "2026-09-15",
          ashaId: 10,
          ashaName: "Sunita Devi",
          status: "SCHEDULED",
          gestationalWeek: 24,
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
        {
          id: 2,
          beneficiaryId: 102,
          beneficiaryName: "Anjali Kumari",
          trackingType: "PNC",
          visitDate: "2026-09-10",
          ashaId: 11,
          ashaName: "Meena Yadav",
          status: "COMPLETED",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
      ],
      isLoading: false,
    });

    renderAshaWork();

    // Tracking type tabs
    expect(screen.getByText("All")).toBeInTheDocument();
    expect(screen.getAllByText("ANC").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("PNC").length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText("Child Care")).toBeInTheDocument();

    // Records
    expect(await screen.findByText("Priya Sharma")).toBeInTheDocument();
    expect(screen.getByText("Anjali Kumari")).toBeInTheDocument();

    // Status badges
    expect(screen.getByText("SCHEDULED")).toBeInTheDocument();
    expect(screen.getByText("COMPLETED")).toBeInTheDocument();

    // Pagination
    expect(screen.getByText("Page 1")).toBeInTheDocument();
  });

  it("shows empty state when no records", async () => {
    queries.useAshaWorkList.mockReturnValue({ data: [], isLoading: false });

    renderAshaWork();

    expect(await screen.findByText(/No visit records found/)).toBeInTheDocument();
  });

  it("shows loading state", () => {
    queries.useAshaWorkList.mockReturnValue({ data: undefined, isLoading: true });

    renderAshaWork();

    expect(screen.getByText(/Loading records/)).toBeInTheDocument();
  });

  it("renders ASHA worker filter options", async () => {
    queries.useAshaWorkList.mockReturnValue({ data: [], isLoading: false });

    renderAshaWork();

    expect(await screen.findByText("Sunita Devi")).toBeInTheDocument();
    expect(screen.getByText("Meena Yadav")).toBeInTheDocument();
  });
});
