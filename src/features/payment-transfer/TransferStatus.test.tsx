import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TransferStatus } from "./TransferStatus";

const mockUseTransferList = vi.fn(() => ({ data: undefined, isLoading: true, isError: false, error: null }));
const mockUseTransferDetail = vi.fn(() => ({ data: undefined }));

vi.mock("./queries", () => ({
  useTransferList: () => mockUseTransferList(),
  useTransferDetail: () => mockUseTransferDetail(),
}));

function renderTransferStatus() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <TransferStatus />
    </QueryClientProvider>,
  );
}

describe("TransferStatus", () => {
  it("renders status page header", () => {
    renderTransferStatus();
    expect(screen.getByText("Transfer Status")).toBeInTheDocument();
    expect(screen.getByText(/track the status/i)).toBeInTheDocument();
  });

  it("shows filter fields", () => {
    renderTransferStatus();
    expect(screen.getByLabelText(/from/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/to/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/status/i)).toBeInTheDocument();
  });

  it("shows apply button", () => {
    renderTransferStatus();
    expect(screen.getByRole("button", { name: /apply/i })).toBeInTheDocument();
  });

  it("shows loading state", () => {
    renderTransferStatus();
    expect(screen.getByText(/loading transfers/i)).toBeInTheDocument();
  });
});
