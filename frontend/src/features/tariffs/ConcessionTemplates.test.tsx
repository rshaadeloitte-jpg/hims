import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { ConcessionTemplates } from "./ConcessionTemplates";

const mockMutateAsync = vi.fn();
const mockDeactivateMutateAsync = vi.fn();
const mockUseConcessionTemplates = vi.fn(() => ({
  data: [
    {
      id: 1,
      code: "STAFF",
      name: "Staff Discount",
      scheme: "Self",
      description: "For hospital staff",
      maxPercentOff: 20,
      requiresApproval: true,
      docStatus: 1,
    },
    {
      id: 2,
      code: "RGHS",
      name: "RGHS Concession",
      scheme: "RGHS",
      maxPercentOff: 100,
      requiresApproval: false,
      docStatus: 1,
    },
  ],
}));

vi.mock("./queries", () => ({
  useConcessionTemplates: () => mockUseConcessionTemplates(),
  useCreateConcessionTemplate: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
  useDeactivateConcessionTemplate: () => ({
    mutateAsync: mockDeactivateMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}));

function renderConcessionTemplates() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <ConcessionTemplates />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ id: 1 });
  mockDeactivateMutateAsync.mockResolvedValue({ id: 1, docStatus: 2 });
});

describe("ConcessionTemplates", () => {
  it("renders the concession templates page", () => {
    renderConcessionTemplates();
    expect(screen.getByText("Concession Templates")).toBeInTheDocument();
    expect(screen.getByText(/Configure discount templates/i)).toBeInTheDocument();
  });

  it("renders template cards", () => {
    renderConcessionTemplates();
    expect(screen.getByText("Staff Discount")).toBeInTheDocument();
    expect(screen.getByText("RGHS Concession")).toBeInTheDocument();
  });

  it("shows template codes", () => {
    renderConcessionTemplates();
    expect(screen.getByText("STAFF")).toBeInTheDocument();
    expect(screen.getAllByText("RGHS").length).toBeGreaterThan(0);
  });

  it("shows max percent off", () => {
    renderConcessionTemplates();
    expect(screen.getByText("20% off")).toBeInTheDocument();
    expect(screen.getByText("100% off")).toBeInTheDocument();
  });

  it("shows approval badge when requiresApproval is true", () => {
    renderConcessionTemplates();
    expect(screen.getByText("Approval Required")).toBeInTheDocument();
  });

  it("shows new template button", () => {
    renderConcessionTemplates();
    expect(screen.getByRole("button", { name: /new template/i })).toBeInTheDocument();
  });

  it("opens form when new template clicked", async () => {
    renderConcessionTemplates();
    fireEvent.click(screen.getByRole("button", { name: /new template/i }));
    await waitFor(() => {
      expect(screen.getByText("New Concession Template")).toBeInTheDocument();
    });
  });

  it("shows deactivate button for active templates", () => {
    renderConcessionTemplates();
    const deactivateButtons = screen.getAllByRole("button", { name: /deactivate/i });
    expect(deactivateButtons.length).toBeGreaterThan(0);
  });

  it("shows scheme on cards", () => {
    renderConcessionTemplates();
    expect(screen.getByText("Self")).toBeInTheDocument();
    expect(screen.getAllByText("RGHS").length).toBeGreaterThan(0);
  });
});
