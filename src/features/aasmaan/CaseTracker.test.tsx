import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CaseTracker } from "./CaseTracker";

const mockCases = [
  { caseId: 1, patientId: 100, caseType: "ANC", status: 1, openedOn: "2024-01-15", closedOn: undefined },
  { caseId: 2, patientId: 200, caseType: "PNC", status: 2, openedOn: "2024-01-10", closedOn: undefined },
];

let mockCasesData = mockCases;
let mockLoading = false;

const mockMutateAsync = vi.fn();

vi.mock("./queries", () => ({
  useCases: () => ({ data: mockCasesData, isLoading: mockLoading }),
  useCreateCase: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false, error: null }),
  useUpdateCase: () => ({ mutateAsync: vi.fn(), isPending: false, isError: false, error: null }),
  useDeleteCase: () => ({ mutateAsync: vi.fn(), isPending: false, isError: false, error: null }),
}));

function renderCaseTracker() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <CaseTracker />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockCasesData = mockCases;
  mockLoading = false;
  mockMutateAsync.mockResolvedValue({ caseId: 3 });
});

describe("CaseTracker", () => {
  it("renders the case tracker header", () => {
    renderCaseTracker();
    expect(screen.getByText("Field Cases")).toBeInTheDocument();
  });

  it("renders case list with data", () => {
    renderCaseTracker();
    expect(screen.getByText("ANC")).toBeInTheDocument();
    expect(screen.getByText("PNC")).toBeInTheDocument();
  });

  it("shows status badges", () => {
    renderCaseTracker();
    expect(screen.getByText("OPEN")).toBeInTheDocument();
    expect(screen.getByText("IN PROGRESS")).toBeInTheDocument();
  });

  it("shows new case button", () => {
    renderCaseTracker();
    expect(screen.getByRole("button", { name: /new case/i })).toBeInTheDocument();
  });

  it("opens create form on New Case click", async () => {
    renderCaseTracker();
    fireEvent.click(screen.getByRole("button", { name: /new case/i }));
    await waitFor(() => {
      expect(screen.getByLabelText("Patient ID *")).toBeInTheDocument();
    });
  });

  it("renders filter controls", () => {
    renderCaseTracker();
    expect(screen.getByLabelText("Status")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Filter by type...")).toBeInTheDocument();
  });

  it("shows edit and delete buttons for each case", () => {
    renderCaseTracker();
    const editButtons = screen.getAllByText("Edit");
    expect(editButtons).toHaveLength(2);
    const deleteButtons = screen.getAllByText("Delete");
    expect(deleteButtons).toHaveLength(2);
  });

  it("opens edit form with case data", async () => {
    renderCaseTracker();
    const editButtons = screen.getAllByText("Edit");
    fireEvent.click(editButtons[0]);
    await waitFor(() => {
      expect(screen.getByText(/Edit Case #1/)).toBeInTheDocument();
    });
  });

  it("shows empty state when no cases", () => {
    mockCasesData = [];
    renderCaseTracker();
    expect(screen.getByText("No cases found")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    mockLoading = true;
    renderCaseTracker();
    expect(screen.getByText("Loading cases...")).toBeInTheDocument();
  });
});
