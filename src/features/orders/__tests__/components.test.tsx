import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { OrderSetBuilder } from "../OrderSetBuilder";
import { ClinicalWorkflow } from "../ClinicalWorkflow";
import { OrderDetail } from "../OrderDetail";
import type { OrderSet, CwfDefinition, ScwfDefinition, WorkflowStepStatus, ClinicalOrder, Investigation, ExternalOrder } from "../queries";

// Mock the queries module
const mockUseOrderSets = vi.fn((): { data: OrderSet[]; isLoading: boolean } => ({ data: [], isLoading: false }));
const mockUseCreateOrderSet = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseUpdateOrderSet = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseApplyOrderSet = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseCwfDefinitions = vi.fn((): { data: CwfDefinition[]; isLoading: boolean } => ({ data: [], isLoading: false }));
const mockUseScwfDefinitions = vi.fn((): { data: ScwfDefinition[]; isLoading: boolean } => ({ data: [], isLoading: false }));
const mockUseCreateCwfDefinition = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseUpdateCwfDefinition = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseCreateScwfDefinition = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseUpdateScwfDefinition = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseWorkflowSteps = vi.fn((): { data: WorkflowStepStatus[] } => ({ data: [] }));
const mockUseAdvanceWorkflowStep = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));
const mockUseClinicalOrder = vi.fn((): { data: ClinicalOrder | null; isLoading: boolean } => ({ data: null, isLoading: true }));
const mockUseInvestigations = vi.fn((): { data: Investigation[] } => ({ data: [] }));
const mockUseExternalOrders = vi.fn((): { data: ExternalOrder[] } => ({ data: [] }));
const mockUseUpdateOrderStatus = vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false }));

vi.mock("../queries", () => ({
  useOrderSets: () => mockUseOrderSets(),
  useCreateOrderSet: () => mockUseCreateOrderSet(),
  useUpdateOrderSet: () => mockUseUpdateOrderSet(),
  useApplyOrderSet: () => mockUseApplyOrderSet(),
  useCwfDefinitions: () => mockUseCwfDefinitions(),
  useScwfDefinitions: () => mockUseScwfDefinitions(),
  useCreateCwfDefinition: () => mockUseCreateCwfDefinition(),
  useUpdateCwfDefinition: () => mockUseUpdateCwfDefinition(),
  useCreateScwfDefinition: () => mockUseCreateScwfDefinition(),
  useUpdateScwfDefinition: () => mockUseUpdateScwfDefinition(),
  useWorkflowSteps: () => mockUseWorkflowSteps(),
  useAdvanceWorkflowStep: () => mockUseAdvanceWorkflowStep(),
  useClinicalOrder: () => mockUseClinicalOrder(),
  useInvestigations: () => mockUseInvestigations(),
  useExternalOrders: () => mockUseExternalOrders(),
  useUpdateOrderStatus: () => mockUseUpdateOrderStatus(),
}));

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("OrderSetBuilder", () => {
  it("renders the order set builder page", () => {
    renderWithProviders(<OrderSetBuilder />);
    expect(screen.getByText("Order Sets")).toBeInTheDocument();
    expect(screen.getByText("+ New Order Set")).toBeInTheDocument();
  });

  it("shows empty state when no order sets", () => {
    mockUseOrderSets.mockReturnValue({ data: [], isLoading: false });
    renderWithProviders(<OrderSetBuilder />);
    expect(screen.getByText(/no order sets configured/i)).toBeInTheDocument();
  });

  it("shows loading state", () => {
    mockUseOrderSets.mockReturnValue({ data: [], isLoading: true });
    renderWithProviders(<OrderSetBuilder />);
    expect(screen.getByText(/loading order sets/i)).toBeInTheDocument();
  });

  it("opens form when clicking new order set", () => {
    mockUseOrderSets.mockReturnValue({ data: [], isLoading: false });
    renderWithProviders(<OrderSetBuilder />);
    fireEvent.click(screen.getByText("+ New Order Set"));
    expect(screen.getByText("New Order Set")).toBeInTheDocument();
  });

  it("renders existing order set cards", () => {
    mockUseOrderSets.mockReturnValue({
      data: [
        {
          id: 1,
          name: "Diabetes Panel",
          code: "DM-PANEL",
          category: "LAB",
          description: "Standard diabetes workup",
          items: [{ id: "i1", serviceType: "LAB", serviceId: 100, serviceName: "FBS", qty: 1, priority: "ROUTINE" }],
          status: "ACTIVE",
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      ],
      isLoading: false,
    });
    renderWithProviders(<OrderSetBuilder />);
    expect(screen.getByText("Diabetes Panel")).toBeInTheDocument();
    expect(screen.getByText("DM-PANEL")).toBeInTheDocument();
  });
});

describe("ClinicalWorkflow", () => {
  it("renders the clinical workflow page", () => {
    renderWithProviders(<ClinicalWorkflow />);
    expect(screen.getByText("Clinical Workflows")).toBeInTheDocument();
    expect(screen.getByText("+ New Workflow")).toBeInTheDocument();
  });

  it("shows CWF and SCWF tabs", () => {
    renderWithProviders(<ClinicalWorkflow />);
    // Use getAllByRole to find tabs specifically
    const tabs = screen.getAllByRole("tab");
    expect(tabs).toHaveLength(2);
    expect(tabs[0]).toHaveTextContent("CWF");
    expect(tabs[1]).toHaveTextContent("SCWF");
  });

  it("shows empty state for CWF", () => {
    mockUseCwfDefinitions.mockReturnValue({ data: [], isLoading: false });
    renderWithProviders(<ClinicalWorkflow />);
    expect(screen.getByText(/no CWF definitions/i)).toBeInTheDocument();
  });

  it("opens form when clicking new workflow", () => {
    renderWithProviders(<ClinicalWorkflow />);
    fireEvent.click(screen.getByText("+ New Workflow"));
    expect(screen.getByText("New CWF")).toBeInTheDocument();
  });

  it("renders workflow cards with steps", () => {
    mockUseCwfDefinitions.mockReturnValue({
      data: [
        {
          cwfId: 1,
          cwfName: "Lab Approval",
          serviceType: "LAB",
          steps: [
            { stepId: 1, stepName: "Collect", stepOrder: 0, role: "NURSE", isMandatory: true },
            { stepId: 2, stepName: "Review", stepOrder: 1, role: "DOCTOR", isMandatory: true },
          ],
          status: "ACTIVE",
        },
      ],
      isLoading: false,
    });
    renderWithProviders(<ClinicalWorkflow />);
    expect(screen.getByText("Lab Approval")).toBeInTheDocument();
    expect(screen.getByText("Collect")).toBeInTheDocument();
    expect(screen.getByText("Review")).toBeInTheDocument();
  });
});

describe("OrderDetail", () => {
  it("shows loading state", () => {
    mockUseClinicalOrder.mockReturnValue({ data: null, isLoading: true });
    renderWithProviders(<OrderDetail />);
    expect(screen.getByText(/loading order/i)).toBeInTheDocument();
  });

  it("shows not found state", () => {
    mockUseClinicalOrder.mockReturnValue({ data: null, isLoading: false });
    renderWithProviders(<OrderDetail />);
    expect(screen.getByText(/order not found/i)).toBeInTheDocument();
  });

  it("renders order details when loaded", () => {
    mockUseClinicalOrder.mockReturnValue({
      data: {
        id: 42,
        patientId: 100,
        patientName: "John Doe",
        orderType: "LAB",
        description: "Fasting glucose test",
        status: "PENDING",
        priority: "ROUTINE",
        orderedBy: "Dr. Smith",
        orderedAt: "2024-01-01T10:00:00Z",
      },
      isLoading: false,
    });
    mockUseWorkflowSteps.mockReturnValue({ data: [] });
    mockUseInvestigations.mockReturnValue({ data: [] });
    mockUseExternalOrders.mockReturnValue({ data: [] });

    renderWithProviders(<OrderDetail />);
    expect(screen.getByText("Order #42")).toBeInTheDocument();
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("Fasting glucose test")).toBeInTheDocument();
  });

  it("renders workflow tab by default", () => {
    mockUseClinicalOrder.mockReturnValue({
      data: {
        id: 1,
        patientId: 100,
        patientName: "Jane",
        orderType: "LAB",
        description: "Test",
        status: "PENDING",
        priority: "URGENT",
        orderedBy: "Dr. X",
        orderedAt: "2024-01-01T00:00:00Z",
      },
      isLoading: false,
    });
    mockUseWorkflowSteps.mockReturnValue({
      data: [{ stepId: 1, stepName: "Review", stepOrder: 0, role: "DOCTOR", status: "PENDING" }],
    });
    mockUseInvestigations.mockReturnValue({ data: [] });
    mockUseExternalOrders.mockReturnValue({ data: [] });

    renderWithProviders(<OrderDetail />);
    expect(screen.getByText("Review")).toBeInTheDocument();
  });
});
