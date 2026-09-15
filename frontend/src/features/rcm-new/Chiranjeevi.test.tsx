import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Chiranjeevi } from "./Chiranjeevi";

const mockRcmJson = vi.fn();

vi.mock("./queries", () => ({
  useQuery: vi.fn(),
  useMutation: vi.fn(),
  useQueryClient: vi.fn(() => ({ invalidateQueries: vi.fn() })),
  rcmJson: (...args: unknown[]) => mockRcmJson(...args),
  Claim: {} as never,
}));

function renderComp() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <Chiranjeevi />
    </QueryClientProvider>,
  );
}

function scheme(status: string) {
  return { id: 1, patientName: "Ramesh", patientId: 101, schemeName: "Chiranjeevi",
    eligibilityDate: "2025-01-01", approvalStatus: status, amount: 50000 };
}

describe("Chiranjeevi", () => {
  it("renders heading and scheme count", async () => {
    mockRcmJson.mockResolvedValue([scheme("pending")]);
    renderComp();
    await waitFor(() => expect(screen.getByText("Chiranjeevi Scheme Approval")).toBeInTheDocument());
    expect(screen.getByText("1 Scheme(s)")).toBeInTheDocument();
  });

  it("shows patient name and approval badge", async () => {
    mockRcmJson.mockResolvedValue([scheme("approved")]);
    renderComp();
    await waitFor(() => expect(screen.getByText("Ramesh")).toBeInTheDocument());
    expect(screen.getByText("Approved")).toBeInTheDocument();
  });

  it("renders empty state", async () => {
    mockRcmJson.mockResolvedValue([]);
    renderComp();
    await waitFor(() => expect(screen.getByText("No schemes found.")).toBeInTheDocument());
  });
});
