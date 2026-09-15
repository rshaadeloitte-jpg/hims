import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PaymentForm } from "./PaymentForm";

const mockMutateAsync = vi.fn();
const mockAdvancesMutateAsync = vi.fn();
const mockUsePatientAdvances = vi.fn(() => ({ data: [] }));

vi.mock("./hooks", () => ({
  useRecordPayment: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false, error: null }),
  usePatientAdvances: () => mockUsePatientAdvances(),
  useApplyAdvance: () => ({ mutateAsync: mockAdvancesMutateAsync, isPending: false, isError: false, error: null }),
}));

function renderPaymentForm(props: {
  billId: number;
  patientId: number;
  balanceAmount: number;
  onPaymentRecorded?: () => void;
  onCancel?: () => void;
}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <PaymentForm {...props} />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ paymentId: 1 });
  mockAdvancesMutateAsync.mockResolvedValue({});
});

describe("PaymentForm", () => {
  it("renders payment form with balance", () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1500 });
    expect(screen.getByText("Record Payment")).toBeInTheDocument();
    expect(screen.getByText("Balance Due")).toBeInTheDocument();
  });

  it("shows payment method buttons", () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1000 });
    expect(screen.getByRole("button", { name: /cash/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /card/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /upi/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cheque/i })).toBeInTheDocument();
  });

  it("shows card fields when card method selected", async () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1000 });
    fireEvent.click(screen.getByRole("button", { name: /card/i }));
    await waitFor(() => {
      expect(screen.getByLabelText(/card number/i)).toBeInTheDocument();
    });
  });

  it("shows UPI fields when UPI method selected", async () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1000 });
    fireEvent.click(screen.getByRole("button", { name: /upi/i }));
    await waitFor(() => {
      expect(screen.getByLabelText(/upi id/i)).toBeInTheDocument();
    });
  });

  it("shows cheque fields when cheque method selected", async () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1000 });
    fireEvent.click(screen.getByRole("button", { name: /cheque/i }));
    await waitFor(() => {
      expect(screen.getByLabelText(/bank name/i)).toBeInTheDocument();
    });
  });

  it("disables submit when amount is 0", () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 0 });
    const submitBtn = screen.getByRole("button", { name: /collect/i });
    expect(submitBtn).toBeDisabled();
  });

  it("shows full amount button", () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 2000 });
    expect(screen.getByRole("button", { name: /full amount/i })).toBeInTheDocument();
  });

  it("shows generate receipt checkbox", () => {
    renderPaymentForm({ billId: 1, patientId: 1, balanceAmount: 1000 });
    expect(screen.getByLabelText(/generate receipt/i)).toBeInTheDocument();
  });
});
