import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DischargeVerification } from "./DischargeVerification";

const mockRcmJson = vi.fn();

vi.mock("./queries", () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(() => ({ invalidateQueries: vi.fn() })),
  rcmJson: (...args: unknown[]) => mockRcmJson(...args),
  Claim: {} as never,
}));

function renderComp(pid: number) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DischargeVerification patientId={pid} />
    </QueryClientProvider>,
  );
}

describe("DischargeVerification", () => {
  it("renders patient header and cleared badge when bills settled", async () => {
    mockRcmJson.mockResolvedValue([
      { id: 1, patientId: 1, amount: 500, status: "settled" },
    ]);
    renderComp(1);
    await waitFor(() => expect(screen.getByText("Discharge Verification — Patient ID 1")).toBeInTheDocument());
    expect(screen.getByText("Cleared for Discharge")).toBeInTheDocument();
  });

  it("shows blocked when bills unpaid", async () => {
    mockRcmJson.mockResolvedValue([
      { id: 1, patientId: 1, amount: 500, status: "unpaid" },
    ]);
    renderComp(1);
    await waitFor(() => expect(screen.getByText("Blocked — Outstanding")).toBeInTheDocument());
  });

  it("renders bill table rows", async () => {
    mockRcmJson.mockResolvedValue([
      { id: 1, patientId: 1, amount: 500, status: "settled" },
      { id: 2, patientId: 1, amount: 300, status: "partial" },
    ]);
    renderComp(1);
    await waitFor(() => expect(screen.getByText("Verify & Clear")).toBeInTheDocument());
  });
});
