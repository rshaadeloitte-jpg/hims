import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { TariffMaster } from "./TariffMaster";

const mockMutateAsync = vi.fn();
const mockUseServiceCategories = vi.fn(() => ({
  data: ["CONSULTATION", "DIAGNOSTIC", "PROCEDURE"],
}));
const mockUseTariffs = vi.fn(() => ({
  data: [
    {
      id: 1,
      serviceId: 101,
      serviceName: "General Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 500,
      copayPercent: 10,
      packageClass: "General",
      status: "ACTIVE" as const,
    },
    {
      id: 2,
      serviceId: 102,
      serviceName: "Specialist Consultation",
      serviceCategory: "CONSULTATION",
      baseRate: 1000,
      copayPercent: 0,
      status: "ACTIVE" as const,
    },
  ],
  isLoading: false,
}));

vi.mock("./queries", () => ({
  useServiceCategories: () => mockUseServiceCategories(),
  useTariffs: () => mockUseTariffs(),
  useCreateTariff: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
  useUpdateTariff: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}));

function renderTariffMaster() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <TariffMaster />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ id: 1 });
});

describe("TariffMaster", () => {
  it("renders the tariff master page", () => {
    renderTariffMaster();
    expect(screen.getByText("Tariff Masters")).toBeInTheDocument();
    expect(screen.getByText(/Manage service tariffs by category/i)).toBeInTheDocument();
  });

  it("shows category cards", () => {
    renderTariffMaster();
    expect(screen.getAllByText("CONSULTATION").length).toBeGreaterThan(0);
    expect(screen.getByText("DIAGNOSTIC")).toBeInTheDocument();
    expect(screen.getByText("PROCEDURE")).toBeInTheDocument();
  });

  it("renders tariff cards with rates", () => {
    renderTariffMaster();
    expect(screen.getByText("General Consultation")).toBeInTheDocument();
    expect(screen.getByText("Specialist Consultation")).toBeInTheDocument();
  });

  it("shows copay percentage when > 0", () => {
    renderTariffMaster();
    expect(screen.getByText("Copay: 10%")).toBeInTheDocument();
  });

  it("shows new tariff button", () => {
    renderTariffMaster();
    expect(screen.getByRole("button", { name: /new tariff/i })).toBeInTheDocument();
  });

  it("opens form when new tariff clicked", async () => {
    renderTariffMaster();
    fireEvent.click(screen.getByRole("button", { name: /new tariff/i }));
    await waitFor(() => {
      expect(screen.getByText("New Tariff")).toBeInTheDocument();
    });
  });

  it("opens form when tariff card clicked", async () => {
    renderTariffMaster();
    fireEvent.click(screen.getByText("General Consultation"));
    await waitFor(() => {
      expect(screen.getByText(/Edit Tariff/i)).toBeInTheDocument();
    });
  });

  it("shows search input", () => {
    renderTariffMaster();
    expect(screen.getByPlaceholderText(/service name or id/i)).toBeInTheDocument();
  });

  it("shows status filter", () => {
    renderTariffMaster();
    expect(screen.getByLabelText("Status")).toBeInTheDocument();
  });
});
