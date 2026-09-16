import { vi, describe, expect, it, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { WasteDashboard } from "./WasteDashboard";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

vi.mock("./queries", () => ({
  useWasteSummary: () => ({
    data: {
      totalCount: 50,
      pendingCount: 10,
      acceptedCount: 15,
      handOveredCount: 10,
      receivedCount: 10,
      completedCount: 5,
    },
    isLoading: false,
  }),
  useWasteList: () => ({
    data: {
      items: [
        {
          id: 1001,
          requisitionNo: "BMW/2026/001",
          fromStore: "Ward A",
          toStore: "BMW Store",
          storeId: 1,
          toStoreId: 2,
          date: "2026-09-10",
          requestStatus: 1,
          handedOverTo: "",
          status: true,
          unitId: 1,
        },
        {
          id: 1002,
          requisitionNo: "BMW/2026/002",
          fromStore: "Ward B",
          toStore: "BMW Store",
          storeId: 1,
          toStoreId: 2,
          date: "2026-09-09",
          requestStatus: 3,
          handedOverTo: "Ramesh",
          status: true,
          unitId: 1,
        },
      ],
      total: 2,
      page: 0,
      size: 25,
    },
    isLoading: false,
  }),
  useWasteDetails: () => ({
    data: [
      {
        id: 1,
        bagNo: "BAG-001",
        barcodeNo: "BC12345",
        weight: 2.5,
        status: 1,
        firmName: "Disposal Corp",
      },
    ],
    isLoading: false,
  }),
  useStores: () => ({
    data: [
      { id: 1, description: "Ward A", isBioMedicalStore: false },
      { id: 2, description: "BMW Store", isBioMedicalStore: true },
    ],
    isLoading: false,
  }),
  useWasteBags: () => ({
    data: [{ id: 1, description: "Yellow Bag" }],
    isLoading: false,
  }),
  useHandoverToFirm: () => ({ mutate: vi.fn(), isPending: false }),
  useReceiveBioRequest: () => ({ mutate: vi.fn(), isPending: false }),
  useDecomposeRequest: () => ({ mutate: vi.fn(), isPending: false }),
  useUpdateBagStatus: () => ({ mutate: vi.fn(), isPending: false }),
  useCreateWasteRequest: () => ({ mutate: vi.fn(), isPending: false }),
}));

describe("WasteDashboard", () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it("renders the title and summary strip", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByRole("heading", { name: /biomedical waste/i })).toBeInTheDocument();
    expect(screen.getAllByText("50").length).toBeGreaterThanOrEqual(1); // total
    expect(screen.getAllByText("10").length).toBeGreaterThanOrEqual(1); // pending
  });

  it("renders the paginated request table", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByText("BMW/2026/001")).toBeInTheDocument();
    expect(screen.getByText("BMW/2026/002")).toBeInTheDocument();
  });

  it("renders Stripe status badges", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getAllByText("Pending").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Handed Over").length).toBeGreaterThanOrEqual(1);
  });

  it("renders category filter strip", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    for (const label of ["All", "Pending", "Accepted", "Handed Over", "Received", "Completed"]) {
      expect(screen.getByRole("button", { name: label })).toBeInTheDocument();
    }
  });

  it("renders per-row action buttons based on status", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} isBioMedicalStore={true} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByRole("button", { name: "Accept" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Receive" })).toBeInTheDocument();
  });

  it("opens handover view when clicking a row", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    fireEvent.click(screen.getByText("BMW/2026/002"));
    expect(screen.getByText(/handover — request #1002/i)).toBeInTheDocument();
  });

  it("shows the create button and opens the form on click", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    const createBtn = screen.getByRole("button", { name: /create waste request/i });
    fireEvent.click(createBtn);
    expect(screen.getByText("Create Biomedical Waste Request")).toBeInTheDocument();
  });

  it("filters by category when a filter pill is clicked", () => {
    render(
      <WasteDashboard unitId={1} storeId={1} />,
      { wrapper: Wrapper },
    );
    fireEvent.click(screen.getByRole("button", { name: "Pending" }));
    expect(screen.getByText("BMW/2026/001")).toBeInTheDocument();
  });
});
