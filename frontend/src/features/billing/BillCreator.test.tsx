import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { BillCreator } from "./BillCreator";

const mockMutateAsync = vi.fn();
const mockUsePatientSearch = vi.fn((): { data: any[] } => ({ data: [] }));
const mockUseConcessionPlans = vi.fn((): { data: any[] } => ({ data: [] }));
const mockUseAvailablePackages = vi.fn((): { data: any[] } => ({ data: [] }));
const mockUseSponsorCompanies = vi.fn((): { data: any[] } => ({ data: [] }));

vi.mock("./queries", () => ({
  useCreateBill: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false, error: null }),
  usePatientSearch: () => mockUsePatientSearch(),
  useConcessionPlans: () => mockUseConcessionPlans(),
  useAvailablePackages: () => mockUseAvailablePackages(),
  useSponsorCompanies: () => mockUseSponsorCompanies(),
}));

function renderBillCreator() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <BillCreator />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ billId: 1 });
});

describe("BillCreator", () => {
  it("renders the bill creation form", () => {
    renderBillCreator();
    expect(screen.getByText("Create New Bill")).toBeInTheDocument();
    expect(screen.getByText("Patient")).toBeInTheDocument();
    expect(screen.getByText("Bill Options")).toBeInTheDocument();
    expect(screen.getByText("Charge Items")).toBeInTheDocument();
  });

  it("shows patient search input initially", () => {
    renderBillCreator();
    expect(screen.getByPlaceholderText(/name, mrn, mobile/i)).toBeInTheDocument();
  });

  it("disables submit when no patient selected", () => {
    renderBillCreator();
    const submitBtn = screen.getByRole("button", { name: /create bill/i });
    expect(submitBtn).toBeDisabled();
  });

  it("shows search results when patients are returned", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [
        { patientId: 1, name: "John Doe", mobileNo: "9999999999" },
        { patientId: 2, name: "Jane Smith", mobileNo: "8888888888" },
      ],
    });
    renderBillCreator();
    const searchInput = screen.getByPlaceholderText(/name, mrn, mobile/i);
    fireEvent.change(searchInput, { target: { value: "John" } });
    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText("Jane Smith")).toBeInTheDocument();
    });
  });

  it("shows selected patient after clicking a result", async () => {
    mockUsePatientSearch.mockReturnValue({
      data: [{ patientId: 1, name: "John Doe", mobileNo: "9999999999" }],
    });
    renderBillCreator();
    const searchInput = screen.getByPlaceholderText(/name, mrn, mobile/i);
    fireEvent.change(searchInput, { target: { value: "John" } });
    await waitFor(() => {
      expect(screen.getByText("John Doe")).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText("John Doe"));
    await waitFor(() => {
      expect(screen.getByText(/patient/i)).toBeInTheDocument();
    });
  });

  it("renders charge items section", () => {
    renderBillCreator();
    expect(screen.getByPlaceholderText("Svc ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Service name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Rate")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Qty")).toBeInTheDocument();
  });

  it("shows hint when no patient selected", () => {
    renderBillCreator();
    expect(screen.getByText(/select a patient to continue/i)).toBeInTheDocument();
  });
});
