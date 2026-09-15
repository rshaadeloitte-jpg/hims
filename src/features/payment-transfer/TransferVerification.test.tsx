import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TransferVerification } from "./TransferVerification";

const mockVerifyMutateAsync = vi.fn();
const mockResendMutateAsync = vi.fn();

vi.mock("./queries", () => ({
  useVerifyOtp: () => ({ mutateAsync: mockVerifyMutateAsync, isPending: false, isError: false, error: null }),
  useResendOtp: () => ({ mutateAsync: mockResendMutateAsync, isPending: false }),
}));

function renderTransferVerification(props: {
  transferId: string;
  onSuccess?: () => void;
  onCancel?: () => void;
  onBack?: () => void;
}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <TransferVerification {...props} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockVerifyMutateAsync.mockResolvedValue({ transferId: "TRF-123" });
  mockResendMutateAsync.mockResolvedValue({ success: true });
});

describe("TransferVerification", () => {
  it("renders verification form", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByText("Verify OTP")).toBeInTheDocument();
    expect(screen.getByText(/enter the 6-digit otp/i)).toBeInTheDocument();
  });

  it("shows OTP input", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByLabelText(/one-time password/i)).toBeInTheDocument();
  });

  it("shows resend OTP button", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByRole("button", { name: /resend otp/i })).toBeInTheDocument();
  });

  it("shows transfer ID badge", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByText("TRF-123")).toBeInTheDocument();
  });

  it("shows back button", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByRole("button", { name: /back/i })).toBeInTheDocument();
  });

  it("shows cancel button", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
  });

  it("disables submit when OTP is incomplete", () => {
    renderTransferVerification({ transferId: "TRF-123" });
    const submitBtn = screen.getByRole("button", { name: /verify & submit/i });
    expect(submitBtn).toBeDisabled();
  });
});
