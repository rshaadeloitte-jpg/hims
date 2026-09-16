import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RadOrderBooking } from "../RadOrderBooking";
import * as queries from "../queries";

/**
 * Component smoke test for RadOrderBooking.
 * Mock TanStack Query hooks so the data flow is exercised end to end.
 */

vi.mock("../queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../queries")>();
  return {
    ...actual,
    useRadStudies: vi.fn(),
    useRadOrders: vi.fn(),
    useCreateRadOrder: vi.fn(),
    useRescheduleLine: vi.fn(),
    useRequestRetest: vi.fn(),
  };
});

const mockedQueries = vi.mocked(queries);

function renderBooking() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <RadOrderBooking patientId={100} />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockedQueries.useRadStudies.mockReturnValue({
    data: [
      { itemId: 1, itemCode: "XR-CHEST", itemName: "Chest X-Ray", modality: "XRAY", department: "Radiology", tat: 2, rate: 300 },
      { itemId: 2, itemCode: "CT-HEAD", itemName: "CT Head", modality: "CT", department: "Radiology", tat: 4, rate: 2500 },
    ],
    isLoading: false,
  } as any);
  mockedQueries.useRadOrders.mockReturnValue({
    data: [
      {
        radOrderId: 1,
        patientId: 100,
        modality: "CT",
        priority: 1,
        status: 1,
        imageCaptured: false,
        details: [{ radOrderId: 1, lineNo: 1, itemId: 2, qty: 1, retestRequested: false }],
      },
    ],
  } as any);
  mockedQueries.useCreateRadOrder.mockReturnValue({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  } as any);
  mockedQueries.useRescheduleLine.mockReturnValue({
    mutateAsync: vi.fn(),
    isPending: false,
    isError: false,
  } as any);
  mockedQueries.useRequestRetest.mockReturnValue({
    mutateAsync: vi.fn(),
    isPending: false,
    isError: false,
  } as any);
});

describe("RadOrderBooking", () => {
  it("renders study list and existing orders", async () => {
    renderBooking();
    fireEvent.click(await screen.findByText("New Order"));

    // Study list renders
    expect(await screen.findByText("Chest X-Ray")).toBeInTheDocument();
    expect(screen.getByText("CT Head")).toBeInTheDocument();

    // Existing orders render
    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getAllByText("CT").length).toBeGreaterThan(0);
  });

  it("shows New Order button and opens form", async () => {
    renderBooking();

    const newOrderBtn = await screen.findByText("New Order");
    expect(newOrderBtn).toBeInTheDocument();

    fireEvent.click(newOrderBtn);
    expect(screen.getByText("New Radiology Order")).toBeInTheDocument();
  });

  it("filters studies by modality", async () => {
    renderBooking();

    // Click New Order to show form
    fireEvent.click(await screen.findByText("New Order"));

    // Select CT modality filter
    const modalitySelect = screen.getByLabelText("Modality");
    fireEvent.change(modalitySelect, { target: { value: "CT" } });

    // CT Head should still be visible, Chest X-Ray filtered out
    expect(await screen.findByText("CT Head")).toBeInTheDocument();
    expect(screen.queryByText("Chest X-Ray")).not.toBeInTheDocument();
  });

  it("renders existing orders with status badges", async () => {
    renderBooking();

    expect(await screen.findByText("SCHEDULED")).toBeInTheDocument();
  });

  it("renders empty state when no orders exist", async () => {
    mockedQueries.useRadOrders.mockReturnValue({
      data: [],
    } as any);

    renderBooking();

    expect(await screen.findByText("No orders found")).toBeInTheDocument();
  });
});
