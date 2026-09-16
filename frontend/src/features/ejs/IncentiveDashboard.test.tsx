import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { IncentiveDashboard } from "./IncentiveDashboard";

/**
 * Component smoke test for IncentiveDashboard.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useIncentiveList: vi.fn(),
    useAshaWorkers: vi.fn(),
  };
});

const queries = (await import("./queries")) as unknown as Record<
  string,
  ReturnType<typeof vi.fn>
>;

function renderDashboard() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <IncentiveDashboard />
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

describe("IncentiveDashboard", () => {
  it("renders summary cards and incentive records", async () => {
    queries.useIncentiveList.mockReturnValue({
      data: [
        {
          id: 1,
          ashaId: 10,
          ashaName: "Sunita Devi",
          month: "2026-09",
          ancVisits: 12,
          pncVisits: 8,
          childCareVisits: 15,
          immunizationReferrals: 6,
          institutionalDeliveryReferrals: 4,
          ancIncentive: 1800,
          pncIncentive: 1200,
          childCareIncentive: 1500,
          immunizationIncentive: 600,
          deliveryIncentive: 2000,
          totalIncentive: 7100,
          status: "APPROVED",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
        {
          id: 2,
          ashaId: 11,
          ashaName: "Meena Yadav",
          month: "2026-09",
          ancVisits: 10,
          pncVisits: 6,
          childCareVisits: 12,
          immunizationReferrals: 5,
          institutionalDeliveryReferrals: 3,
          ancIncentive: 1500,
          pncIncentive: 900,
          childCareIncentive: 1200,
          immunizationIncentive: 500,
          deliveryIncentive: 1500,
          totalIncentive: 5600,
          status: "PENDING",
          createdAt: "2026-09-01T00:00:00Z",
          updatedAt: "2026-09-01T00:00:00Z",
        },
      ],
      isLoading: false,
    });

    renderDashboard();

    // Summary cards
    const totalEls = await screen.findAllByText("Total");
    expect(totalEls.length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Pending").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Approved/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Paid").length).toBeGreaterThanOrEqual(1);

    // Records
    expect(screen.getAllByText("Sunita Devi").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Meena Yadav").length).toBeGreaterThanOrEqual(1);

    // Amounts
    expect(screen.getAllByText(/7,100/).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/5,600/).length).toBeGreaterThanOrEqual(1);

    // Status badges
    expect(screen.getAllByText("APPROVED").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("PENDING").length).toBeGreaterThanOrEqual(1);
  });

  it("shows empty state when no records", async () => {
    queries.useIncentiveList.mockReturnValue({ data: [], isLoading: false });

    renderDashboard();

    expect(await screen.findByText(/No incentive records found/)).toBeInTheDocument();
  });

  it("shows loading state", () => {
    queries.useIncentiveList.mockReturnValue({ data: undefined, isLoading: true });

    renderDashboard();

    expect(screen.getByText(/Loading incentives/)).toBeInTheDocument();
  });
});
