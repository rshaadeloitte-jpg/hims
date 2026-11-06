import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DMApproval } from "./DMApproval";

function renderComp(claimId: number) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <DMApproval claimId={claimId} />
    </QueryClientProvider>,
  );
}

describe("DMApproval", () => {
  it("renders disabled approval controls", () => {
    renderComp(1);
    expect(screen.getByText("DM Approval — Claim #1")).toBeInTheDocument();
    expect(screen.getByText(/not available/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Approve/i })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Reject/i })).toBeDisabled();
  });
});
