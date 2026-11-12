import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { BillDetailPage } from "./BillDetailPage";

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

function renderBillDetailPage(billId = 101) {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={[`/billing/${billId}`]}>
        <Routes>
          <Route path="/billing/:id" element={<BillDetailPage />} />
          <Route path="/billing" element={<div>Bills List Page</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

const mockBill = {
  billId: 101,
  patientId: 505,
  billNo: "OPDBILL-000101",
  isInterim: false,
  isSettled: false,
  isCancelled: false,
  totalAmount: 1000.0,
  taxAmount: 180.0,
  concessionAmount: 0.0,
  selfAmount: 580.0,
  nonSelfAmount: 600.0,
  balanceSelf: 580.0,
  balanceNonSelf: 600.0,
  changeAmount: 0.0,
  charges: [
    {
      chargeId: 1,
      serviceId: 900,
      serviceName: "X-Ray Chest",
      rate: 1000.0,
      qty: 1,
      totalBeforeFinalization: 1000.0,
      selfBeforeFinalization: 580.0,
      nonselfBeforeFinalization: 600.0,
      calculations: [
        {
          lineTotal: 1000.0,
          selfRate: 400.0,
          nonSelfRate: 600.0,
          concession: 0.0,
          taxTotal: 180.0,
          selfAmount: 580.0,
          nonSelfAmount: 600.0,
        },
      ],
      isCancelled: false,
    },
  ],
};

const mockPayments = [
  {
    paymentId: 1,
    billId: 101,
    patientId: 505,
    amount: 200.0,
    transactionType: 2,
    changeAmount: 0.0,
    addedDatetime: "2026-11-12T08:30:00Z",
    modes: [{ modeDetailId: 10, paymentId: 1, mode: 1, amount: 200.0 }],
  },
  {
    paymentId: 2,
    billId: 101,
    patientId: 505,
    amount: 150.0,
    transactionType: 2,
    changeAmount: 0.0,
    addedDatetime: "2026-11-12T09:00:00Z",
    modes: [{ modeDetailId: 11, paymentId: 2, mode: 7, amount: 150.0 }],
  },
];

describe("BillDetailPage", () => {
  it("renders bill details, charges table with non-blank AMOUNT / TAX / TOTAL, and payments list", async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    renderBillDetailPage();

    // Check bill header
    await waitFor(() => {
      expect(screen.getByText("Bill OPDBILL-000101")).toBeInTheDocument();
    });

    // Check charges table - AMOUNT / TAX / TOTAL columns are mapped from calculations
    expect(screen.getByText("X-Ray Chest")).toBeInTheDocument();
    expect(screen.getAllByText("1000.00").length).toBeGreaterThanOrEqual(2); // In card and table cells (rate & amount)
    expect(screen.getByText("180.00")).toBeInTheDocument(); // Tax
    expect(screen.getByText("1180.00")).toBeInTheDocument(); // Total

    // Check payments table - mapped from real payment modes
    expect(screen.getByText("Cash")).toBeInTheDocument();
    expect(screen.getByText("200.00")).toBeInTheDocument();
    expect(screen.getByText("Advance")).toBeInTheDocument();
    expect(screen.getByText("150.00")).toBeInTheDocument();
    expect(screen.getAllByText("2026-11-12")).toHaveLength(2);
  });

  it("posts + Payment to POST /api/v1/payments with structured body mapping cash mode", async () => {
    let paymentPostCall: any = null;

    fetchMock.mockImplementation(async (url: string, options: any) => {
      if (url.endsWith("/api/v1/payments") && options?.method === "POST") {
        paymentPostCall = { url, options, body: JSON.parse(options.body) };
        return {
          ok: true,
          status: 201,
          json: async () => ({
            paymentId: 3,
            billId: 101,
            patientId: 505,
            amount: 250,
            transactionType: 2,
            modes: [{ modeDetailId: 12, paymentId: 3, mode: 1, amount: 250 }],
          }),
        };
      }
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    renderBillDetailPage();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "+ Payment" })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));

    // Fill in amount
    const amountInput = screen.getByPlaceholderText("Amount");
    fireEvent.change(amountInput, { target: { value: "250" } });

    // Method defaults to Cash
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(paymentPostCall).not.toBeNull();
    });

    expect(paymentPostCall.url).toMatch(/\/api\/v1\/payments$/);
    expect(paymentPostCall.body).toEqual({
      billId: 101,
      patientId: 505,
      amount: 250,
      advanceUse: false,
      modes: [{ mode: 1, amount: 250 }],
    });
  });

  it("posts + Payment with Advance method using advanceUse: true and empty modes", async () => {
    let paymentPostCall: any = null;

    fetchMock.mockImplementation(async (url: string, options: any) => {
      if (url.endsWith("/api/v1/payments") && options?.method === "POST") {
        paymentPostCall = { url, options, body: JSON.parse(options.body) };
        return {
          ok: true,
          status: 201,
          json: async () => ({
            paymentId: 4,
            billId: 101,
            patientId: 505,
            amount: 150,
            transactionType: 2,
            modes: [{ modeDetailId: 13, paymentId: 4, mode: 7, amount: 150 }],
          }),
        };
      }
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    renderBillDetailPage();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "+ Payment" })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));

    const amountInput = screen.getByPlaceholderText("Amount");
    fireEvent.change(amountInput, { target: { value: "150" } });

    const methodSelect = screen.getByRole("combobox");
    fireEvent.change(methodSelect, { target: { value: "ADVANCE" } });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(paymentPostCall).not.toBeNull();
    });

    expect(paymentPostCall.body).toEqual({
      billId: 101,
      patientId: 505,
      amount: 150,
      advanceUse: true,
      modes: [],
    });
  });

  it("posts + Payment with Card, UPI, and Cheque mapped to modes 2, 4, 3", async () => {
    const postCalls: any[] = [];

    fetchMock.mockImplementation(async (url: string, options: any) => {
      if (url.endsWith("/api/v1/payments") && options?.method === "POST") {
        const body = JSON.parse(options.body);
        postCalls.push(body);
        return {
          ok: true,
          status: 201,
          json: async () => ({
            paymentId: 5,
            billId: 101,
            patientId: 505,
            amount: body.amount,
            transactionType: 2,
            modes: body.modes,
          }),
        };
      }
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    const { unmount } = renderBillDetailPage();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "+ Payment" })).toBeInTheDocument();
    });

    // Test Card (mode 2)
    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));
    fireEvent.change(screen.getByPlaceholderText("Amount"), { target: { value: "300" } });
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "CARD" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(postCalls.length).toBe(1);
    });
    expect(postCalls[0].modes).toEqual([{ mode: 2, amount: 300 }]);

    // Test UPI (mode 4)
    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));
    fireEvent.change(screen.getByPlaceholderText("Amount"), { target: { value: "120" } });
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "UPI" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(postCalls.length).toBe(2);
    });
    expect(postCalls[1].modes).toEqual([{ mode: 4, amount: 120 }]);

    // Test Cheque (mode 3)
    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));
    fireEvent.change(screen.getByPlaceholderText("Amount"), { target: { value: "450" } });
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "CHEQUE" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(postCalls.length).toBe(3);
    });
    expect(postCalls[2].modes).toEqual([{ mode: 3, amount: 450 }]);

    unmount();
  });

  it("keeps working calls: finalize, credit notes, and debit notes", async () => {
    let finalizeCalled = false;
    let creditNoteCalled = false;
    let debitNoteCalled = false;

    fetchMock.mockImplementation(async (url: string, options: any) => {
      if (url.includes("/api/v1/bills/101/finalize") && options?.method === "POST") {
        finalizeCalled = true;
        return { ok: true, status: 200, json: async () => ({ ...mockBill, isInterim: false, billNo: "OPDBILL-999" }) };
      }
      if (url.includes("/api/v1/bills/101/credit-notes") && options?.method === "POST") {
        creditNoteCalled = true;
        return { ok: true, status: 201, json: async () => ({ creditNoteId: 1, billId: 101, amount: 50, reason: "Adjustment" }) };
      }
      if (url.includes("/api/v1/bills/101/debit-notes") && options?.method === "POST") {
        debitNoteCalled = true;
        return { ok: true, status: 201, json: async () => ({ debitNoteId: 1, billId: 101, amount: 50, reason: "Extra", status: 1 }) };
      }
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    renderBillDetailPage();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Finalize" })).toBeInTheDocument();
    });

    // Test Finalize call
    fireEvent.click(screen.getByRole("button", { name: "Finalize" }));
    await waitFor(() => {
      expect(finalizeCalled).toBe(true);
    });

    // Test Credit Note call
    fireEvent.click(screen.getByRole("button", { name: "Credit Note" }));
    fireEvent.change(screen.getByPlaceholderText("Amount"), { target: { value: "50" } });
    fireEvent.change(screen.getByPlaceholderText("Reason"), { target: { value: "Adjustment" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => {
      expect(creditNoteCalled).toBe(true);
    });

    // Test Debit Note call
    fireEvent.click(screen.getByRole("button", { name: "Debit Note" }));
    fireEvent.change(screen.getByPlaceholderText("Amount"), { target: { value: "50" } });
    fireEvent.change(screen.getByPlaceholderText("Reason"), { target: { value: "Extra" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => {
      expect(debitNoteCalled).toBe(true);
    });
  });

  it("displays backend error message on payment failure", async () => {
    fetchMock.mockImplementation(async (url: string, options: any) => {
      if (url.endsWith("/api/v1/payments") && options?.method === "POST") {
        return {
          ok: false,
          status: 400,
          json: async () => ({
            status: "ERROR",
            errorCode: "PAY_MODE_MISMATCH",
            message: "Sum of modes (90.00) must equal amount (100.00)",
          }),
        };
      }
      if (url.includes("/api/v1/bills/101/credit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/bills/101/debit-notes")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/payments?billId=101")) {
        return { ok: true, status: 200, json: async () => mockPayments };
      }
      if (url.includes("/api/v1/bills/101")) {
        return { ok: true, status: 200, json: async () => mockBill };
      }
      return { ok: false, status: 404, json: async () => ({ message: "Not found" }) };
    });

    renderBillDetailPage();

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "+ Payment" })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: "+ Payment" }));

    const amountInput = screen.getByPlaceholderText("Amount");
    fireEvent.change(amountInput, { target: { value: "100" } });

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(
        screen.getByText("Sum of modes (90.00) must equal amount (100.00)"),
      ).toBeInTheDocument();
    });
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });
});
