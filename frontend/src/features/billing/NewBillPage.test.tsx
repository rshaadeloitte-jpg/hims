import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { NewBillPage } from "./NewBillPage";

const mockCreateBill = vi.fn();
const mockCreateCharge = vi.fn();
const mockUsePatientSearch = vi.fn((): { data: any[] } => ({ data: [] }));

vi.mock("./queries", () => ({
  useCreateBill: () => ({
    mutateAsync: mockCreateBill,
    isPending: false,
    isError: false,
    error: null,
  }),
  useCreateCharge: () => ({
    mutateAsync: mockCreateCharge,
    isPending: false,
    isError: false,
    error: null,
  }),
  usePatientSearch: (_query: string) => mockUsePatientSearch(),
}));

function renderNewBillPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={["/billing/new"]}>
        <Routes>
          <Route path="/billing/new" element={<NewBillPage />} />
          <Route path="/billing/:id" element={<div data-testid="bill-detail">Bill Detail</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("NewBillPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCreateCharge.mockImplementation(async (charge: any) => ({
      chargeId: Math.floor(Math.random() * 1000) + 1,
      ...charge,
    }));
    mockCreateBill.mockResolvedValue({
      billId: 999,
      billNo: "OPDBILL-001",
    });
  });

  it("renders page header, patient search, bill options, and charge entry", () => {
    renderNewBillPage();
    expect(screen.getByRole("heading", { name: "New Bill" })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/search by name, mobile, abha/i)).toBeInTheDocument();
    expect(screen.getByText("Bill Options")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Service ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Service Name")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "+ Add" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create Bill" })).toBeInTheDocument();
  });

  it("selects a patient from search results and sets patientId", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [
        { id: 42, firstName: "Aarav", lastName: "Sharma", mobile: "9876543210" },
      ],
    });

    renderNewBillPage();
    const searchInput = screen.getByPlaceholderText(/search by name, mobile, abha/i);
    fireEvent.change(searchInput, { target: { value: "Aarav" } });

    await waitFor(() => {
      expect(screen.getByText(/Aarav Sharma — 9876543210/)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Aarav Sharma — 9876543210/));

    await waitFor(() => {
      expect(screen.getByText(/Aarav Sharma \(ID: 42\)/)).toBeInTheDocument();
    });

    // Clicking Change clears the patient
    fireEvent.click(screen.getByRole("button", { name: "Change" }));
    expect(screen.getByPlaceholderText(/search by name, mobile, abha/i)).toBeInTheDocument();
  });

  it("adds charge to local state via '+ Add' without submitting the form or reloading", async () => {
    renderNewBillPage();

    const serviceIdInput = screen.getByPlaceholderText("Service ID");
    const serviceNameInput = screen.getByPlaceholderText("Service Name");
    const rateInput = screen.getByPlaceholderText("Rate");
    const qtyInput = screen.getByPlaceholderText("Qty");
    const addBtn = screen.getByRole("button", { name: "+ Add" });

    // '+ Add' must be a button (type="button") so it doesn't trigger native form submit
    expect(addBtn).toHaveAttribute("type", "button");

    fireEvent.change(serviceIdInput, { target: { value: "101" } });
    fireEvent.change(serviceNameInput, { target: { value: "General Consultation" } });
    fireEvent.change(rateInput, { target: { value: "500" } });
    fireEvent.change(qtyInput, { target: { value: "1" } });

    fireEvent.click(addBtn);

    // Charge row appears in the table
    await waitFor(() => {
      expect(screen.getByText("General Consultation (#101)")).toBeInTheDocument();
      expect(screen.getAllByText("500.00").length).toBeGreaterThanOrEqual(1);
    });

    // Inputs reset
    expect(serviceNameInput).toHaveValue("");

    // Add a second charge
    fireEvent.change(serviceIdInput, { target: { value: "102" } });
    fireEvent.change(serviceNameInput, { target: { value: "Blood Test" } });
    fireEvent.change(rateInput, { target: { value: "250" } });
    fireEvent.change(qtyInput, { target: { value: "2" } });
    fireEvent.click(addBtn);

    await waitFor(() => {
      expect(screen.getByText("Blood Test (#102)")).toBeInTheDocument();
      expect(screen.getByText("250.00")).toBeInTheDocument();
    });

    // Remove first charge
    const removeButtons = screen.getAllByRole("button", { name: "✕" });
    fireEvent.click(removeButtons[0]);

    await waitFor(() => {
      expect(screen.queryByText("General Consultation (#101)")).not.toBeInTheDocument();
      expect(screen.getByText("Blood Test (#102)")).toBeInTheDocument();
    });
  });

  it("creates charges first then creates bill attaching chargeIds and navigates", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [{ patientId: 77, name: "Meera Patel", mobileNo: "9123456780" }],
    });

    mockCreateCharge
      .mockResolvedValueOnce({ chargeId: 201, serviceId: 10, serviceName: "ECG", rate: 300, qty: 1 })
      .mockResolvedValueOnce({ chargeId: 202, serviceId: 11, serviceName: "X-Ray", rate: 700, qty: 1 });

    mockCreateBill.mockResolvedValueOnce({
      billId: 888,
      billNo: "OPDBILL-888",
    });

    renderNewBillPage();

    // 1. Pick patient
    const searchInput = screen.getByPlaceholderText(/search by name, mobile, abha/i);
    fireEvent.change(searchInput, { target: { value: "Meera" } });
    await waitFor(() => {
      expect(screen.getByText(/Meera Patel/)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText(/Meera Patel/));
    await waitFor(() => {
      expect(screen.getByText(/Meera Patel \(ID: 77\)/)).toBeInTheDocument();
    });

    // 2. Add charge 1
    fireEvent.change(screen.getByPlaceholderText("Service ID"), { target: { value: "10" } });
    fireEvent.change(screen.getByPlaceholderText("Service Name"), { target: { value: "ECG" } });
    fireEvent.change(screen.getByPlaceholderText("Rate"), { target: { value: "300" } });
    fireEvent.change(screen.getByPlaceholderText("Qty"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add" }));

    await waitFor(() => {
      expect(screen.getByText("ECG (#10)")).toBeInTheDocument();
    });

    // 3. Add charge 2
    fireEvent.change(screen.getByPlaceholderText("Service ID"), { target: { value: "11" } });
    fireEvent.change(screen.getByPlaceholderText("Service Name"), { target: { value: "X-Ray" } });
    fireEvent.change(screen.getByPlaceholderText("Rate"), { target: { value: "700" } });
    fireEvent.change(screen.getByPlaceholderText("Qty"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add" }));

    await waitFor(() => {
      expect(screen.getByText("X-Ray (#11)")).toBeInTheDocument();
    });

    // 4. Click Create Bill
    const createBillBtn = screen.getByRole("button", { name: "Create Bill" });
    fireEvent.click(createBillBtn);

    // Verify charges were created first
    await waitFor(() => {
      expect(mockCreateCharge).toHaveBeenCalledTimes(2);
      expect(mockCreateCharge).toHaveBeenNthCalledWith(1, {
        serviceId: 10,
        serviceName: "ECG",
        rate: 300,
        qty: 1,
      });
      expect(mockCreateCharge).toHaveBeenNthCalledWith(2, {
        serviceId: 11,
        serviceName: "X-Ray",
        rate: 700,
        qty: 1,
      });
    });

    // Verify createBill was called with chargeIds attached
    await waitFor(() => {
      expect(mockCreateBill).toHaveBeenCalledTimes(1);
      expect(mockCreateBill).toHaveBeenCalledWith(
        expect.objectContaining({
          patientId: 77,
          chargeIds: [201, 202],
          isSelfCompany: true,
          cashCounterId: 1,
        }),
      );
    });

    // Navigates to bill detail
    await waitFor(() => {
      expect(screen.getByTestId("bill-detail")).toBeInTheDocument();
    });
  });

  it("shows backend error messages if charge creation fails", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [{ patientId: 77, name: "Meera Patel" }],
    });

    mockCreateCharge.mockRejectedValueOnce(new Error("Service tariff expired"));

    renderNewBillPage();

    // Select patient
    fireEvent.change(screen.getByPlaceholderText(/search by name, mobile, abha/i), { target: { value: "Meera" } });
    await waitFor(() => expect(screen.getByText(/Meera Patel/)).toBeInTheDocument());
    fireEvent.click(screen.getByText(/Meera Patel/));
    await waitFor(() => expect(screen.getByText(/Meera Patel \(ID: 77\)/)).toBeInTheDocument());

    // Add charge
    fireEvent.change(screen.getByPlaceholderText("Service ID"), { target: { value: "10" } });
    fireEvent.change(screen.getByPlaceholderText("Service Name"), { target: { value: "ECG" } });
    fireEvent.change(screen.getByPlaceholderText("Rate"), { target: { value: "300" } });
    fireEvent.change(screen.getByPlaceholderText("Qty"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add" }));

    await waitFor(() => expect(screen.getByText("ECG (#10)")).toBeInTheDocument());

    // Submit
    fireEvent.click(screen.getByRole("button", { name: "Create Bill" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Service tariff expired");
    });
  });

  it("shows backend error messages if bill creation fails", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [{ patientId: 77, name: "Meera Patel" }],
    });

    mockCreateCharge.mockResolvedValueOnce({ chargeId: 501, serviceId: 10, serviceName: "ECG", rate: 300, qty: 1 });
    mockCreateBill.mockRejectedValueOnce(new Error("Cash counter closed"));

    renderNewBillPage();

    // Select patient
    fireEvent.change(screen.getByPlaceholderText(/search by name, mobile, abha/i), { target: { value: "Meera" } });
    await waitFor(() => expect(screen.getByText(/Meera Patel/)).toBeInTheDocument());
    fireEvent.click(screen.getByText(/Meera Patel/));
    await waitFor(() => expect(screen.getByText(/Meera Patel \(ID: 77\)/)).toBeInTheDocument());

    // Add charge
    fireEvent.change(screen.getByPlaceholderText("Service ID"), { target: { value: "10" } });
    fireEvent.change(screen.getByPlaceholderText("Service Name"), { target: { value: "ECG" } });
    fireEvent.change(screen.getByPlaceholderText("Rate"), { target: { value: "300" } });
    fireEvent.change(screen.getByPlaceholderText("Qty"), { target: { value: "1" } });
    fireEvent.click(screen.getByRole("button", { name: "+ Add" }));

    await waitFor(() => expect(screen.getByText("ECG (#10)")).toBeInTheDocument());

    // Submit
    fireEvent.click(screen.getByRole("button", { name: "Create Bill" }));

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent("Cash counter closed");
    });
  });
});
