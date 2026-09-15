import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GeographyMaster } from "./GeographyMaster";

/**
 * Component smoke test for GeographyMaster.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useGeographyList: vi.fn(),
    useAshaWorkers: vi.fn(),
  };
});

const queries = (await import("./queries")) as unknown as Record<
  string,
  ReturnType<typeof vi.fn>
>;

function renderMaster() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <GeographyMaster />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  queries.useAshaWorkers.mockResolvedValue([
    { ashaId: 10, ashaName: "Sunita Devi" },
  ]);
});

describe("GeographyMaster", () => {
  it("renders summary cards and geography records", async () => {
    queries.useGeographyList.mockResolvedValue([
      {
        id: 1,
        ashaId: 10,
        ashaName: "Sunita Devi",
        state: "Rajasthan",
        district: "Jaipur",
        block: "Sanganer",
        village: "Pragpura",
        subCenter: "Pragpura SC",
        totalPopulation: 2450,
        targetCouples: 320,
        pregnantWomen: 45,
        childrenUnder5: 180,
        households: 420,
        eligibleCouples: 280,
        isActive: true,
        lastEnumerationDate: "2026-08-15",
        createdAt: "2026-01-01T00:00:00Z",
        updatedAt: "2026-08-15T00:00:00Z",
      },
    ]);

    renderMaster();

    // Summary cards
    expect(await screen.findByText("Total Population")).toBeInTheDocument();
    expect(screen.getByText("Pregnant Women")).toBeInTheDocument();
    expect(screen.getByText("Children <5")).toBeInTheDocument();
    expect(screen.getByText("Households")).toBeInTheDocument();

    // Record data
    expect(screen.getByText("2,450")).toBeInTheDocument();
    expect(screen.getByText("420")).toBeInTheDocument();
    expect(screen.getByText("45")).toBeInTheDocument();
    expect(screen.getByText("180")).toBeInTheDocument();

    // Village name
    expect(screen.getByText("Pragpura")).toBeInTheDocument();

    // Status badge
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("shows empty state when no records", async () => {
    queries.useGeographyList.mockResolvedValue([]);

    renderMaster();

    expect(await screen.findByText(/No geography records found/)).toBeInTheDocument();
  });

  it("shows loading state", () => {
    queries.useGeographyList.mockReturnValue({ data: undefined, isLoading: true });

    renderMaster();

    expect(screen.getByText(/Loading geography records/)).toBeInTheDocument();
  });
});
