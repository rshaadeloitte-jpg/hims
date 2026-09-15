import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { PackageBuilder } from "./PackageBuilder";

const mockMutateAsync = vi.fn();
const mockUsePackages = vi.fn(() => ({
  data: [
    {
      id: 1,
      packageName: "Health Checkup Basic",
      packageCode: "HCB001",
      description: "Basic health screening",
      totalValue: 5000,
      sellPrice: 4500,
      services: [
        { serviceId: 101, serviceName: "Consultation", qty: 1 },
        { serviceId: 102, serviceName: "Blood Test", qty: 1 },
      ],
      plans: [
        { planId: 1, planName: "Basic", coveragePercent: 80 },
      ],
      status: "ACTIVE" as const,
    },
  ],
  isLoading: false,
}));

vi.mock("./queries", () => ({
  usePackages: () => mockUsePackages(),
  useCreatePackage: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
  useUpdatePackage: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
    isError: false,
    error: null,
  }),
}));

function renderPackageBuilder() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <PackageBuilder />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockMutateAsync.mockResolvedValue({ id: 1 });
});

describe("PackageBuilder", () => {
  it("renders the package builder page", () => {
    renderPackageBuilder();
    expect(screen.getByText("Package Builder")).toBeInTheDocument();
    expect(screen.getByText(/Compose service packages/i)).toBeInTheDocument();
  });

  it("renders package cards", () => {
    renderPackageBuilder();
    expect(screen.getByText("Health Checkup Basic")).toBeInTheDocument();
    expect(screen.getByText("HCB001")).toBeInTheDocument();
  });

  it("shows service count", () => {
    renderPackageBuilder();
    expect(screen.getByText("2 services")).toBeInTheDocument();
  });

  it("shows plan count when plans exist", () => {
    renderPackageBuilder();
    expect(screen.getByText("1 plan(s)")).toBeInTheDocument();
  });

  it("shows new package button", () => {
    renderPackageBuilder();
    expect(screen.getByRole("button", { name: /new package/i })).toBeInTheDocument();
  });

  it("opens form when new package clicked", async () => {
    renderPackageBuilder();
    fireEvent.click(screen.getByRole("button", { name: /new package/i }));
    await waitFor(() => {
      expect(screen.getByText("New Package")).toBeInTheDocument();
    });
  });

  it("opens form when package card clicked", async () => {
    renderPackageBuilder();
    fireEvent.click(screen.getByText("Health Checkup Basic"));
    await waitFor(() => {
      expect(screen.getByText(/Edit Package/i)).toBeInTheDocument();
    });
  });

  it("shows sell price on cards", () => {
    renderPackageBuilder();
    expect(screen.getByText("₹ 4500.00")).toBeInTheDocument();
  });

  it("shows value price on cards", () => {
    renderPackageBuilder();
    expect(screen.getByText("Value: ₹ 5000.00")).toBeInTheDocument();
  });
});
