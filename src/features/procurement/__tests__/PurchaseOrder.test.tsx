import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PurchaseOrderPage } from "../PurchaseOrder";

vi.mock("../queries", () => ({
  useSuppliers: vi.fn(),
  useStores: vi.fn(),
  useItems: vi.fn(),
  useCreatePurchaseOrder: vi.fn(),
}));

import {
  useSuppliers,
  useStores,
  useItems,
  useCreatePurchaseOrder,
} from "../queries";

function renderPO() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <PurchaseOrderPage />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useSuppliers as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, code: "SUP1", description: "Supplier 1" }] });
  (useStores as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, code: "STR1", description: "Main Store" }] });
  (useItems as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, itemName: "Paracetamol" }] });
  (useCreatePurchaseOrder as ReturnType<typeof vi.fn>).mockReturnValue({ mutate: vi.fn(), isPending: false });
});

describe("PurchaseOrderPage", () => {
  it("renders the heading and New PO button", () => {
    renderPO();
    expect(screen.getByText("Purchase Orders")).toBeInTheDocument();
    expect(screen.getByText("New PO")).toBeInTheDocument();
  });

  it("shows the form when New PO is clicked", async () => {
    const user = userEvent.setup();
    renderPO();
    await user.click(screen.getByText("New PO"));
    expect(screen.getByText("New Purchase Order")).toBeInTheDocument();
    expect(screen.getByText("Line Items")).toBeInTheDocument();
  });

  it("shows store, supplier, and item selects", async () => {
    const user = userEvent.setup();
    renderPO();
    await user.click(screen.getByText("New PO"));
    expect(screen.getByLabelText(/Store/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Supplier/i)).toBeInTheDocument();
  });
});
