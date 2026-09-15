import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TransferRequest } from "./TransferRequest";

const mockMutateAsync = vi.fn();
const mockUseTransferSchemes = vi.fn(() => ({ data: [] }));

vi.mock("./queries", () => ({
  useInitiateTransfer: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false, error: null }),
  useTransferSchemes: () => mockUseTransferSchemes(),
}));

function renderTransferRequest() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <TransferRequest />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ transferId: "TRF-123" });
});

describe("TransferRequest", () => {
  it("renders transfer request form", () => {
    renderTransferRequest();
    expect(screen.getByText("New Payment Transfer")).toBeInTheDocument();
    expect(screen.getByText("Beneficiary Details")).toBeInTheDocument();
    expect(screen.getByText("Transfer Details")).toBeInTheDocument();
  });

  it("shows all input fields", () => {
    renderTransferRequest();
    expect(screen.getByLabelText(/beneficiary name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/account number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/ifsc code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/transfer amount/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/scheme/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/purpose/i)).toBeInTheDocument();
  });

  it("shows quick amount buttons", () => {
    renderTransferRequest();
    expect(screen.getByRole("button", { name: /₹500/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /₹1,000/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /₹2,000/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /₹5,000/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /₹10,000/ })).toBeInTheDocument();
  });

  it("disables submit when amount is 0", () => {
    renderTransferRequest();
    const submitBtn = screen.getByRole("button", { name: /initiate transfer/i });
    expect(submitBtn).toBeDisabled();
  });

  it("shows clear button", () => {
    renderTransferRequest();
    expect(screen.getByRole("button", { name: /clear/i })).toBeInTheDocument();
  });

  it("shows remarks as optional", () => {
    renderTransferRequest();
    expect(screen.getByText("(optional)")).toBeInTheDocument();
  });
});
