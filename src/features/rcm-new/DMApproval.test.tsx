import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DMApproval } from "./DMApproval";

const mockRcmJson = vi.fn();

vi.mock("./queries", () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(() => ({ invalidateQueries: vi.fn() })),
  rcmJson: (...args: unknown[]) => mockRcmJson(...args),
  Claim: {} as never,
}));

function renderComp(claimId: number) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DMApproval claimId={claimId} />
    </QueryClientProvider>,
  );
}

function claimData(id: number, status: string) {
  return { id, patientName: "Test", scheme: "ABC", amount: 1000, status, submittedAt: "2025-01-01" };
}

describe("DMApproval", () => {
  it("renders claim header and pipeline", async () => {
    mockRcmJson.mockResolvedValue(claimData(1, "dm_approval"));
    renderComp(1);
    await waitFor(() => expect(screen.getByText("DM Approval — Claim #1")).toBeInTheDocument());
    expect(screen.getByText("1. Pending")).toBeInTheDocument();
    expect(screen.getByText("2. DLH Approval")).toBeInTheDocument();
    expect(screen.getByText("3. DM Approval")).toBeInTheDocument();
  });

  it("shows approve/reject buttons in DM stage", async () => {
    mockRcmJson.mockResolvedValue(claimData(2, "dm_approval"));
    renderComp(2);
    await waitFor(() => expect(screen.getByText("Approve")).toBeInTheDocument());
    expect(screen.getByText("Reject")).toBeInTheDocument();
  });

  it("shows Stripe pending badge when not approved", async () => {
    mockRcmJson.mockResolvedValue(claimData(3, "pending"));
    renderComp(3);
    await waitFor(() => expect(screen.getByText("◷ Stripe Pending")).toBeInTheDocument());
  });
});
