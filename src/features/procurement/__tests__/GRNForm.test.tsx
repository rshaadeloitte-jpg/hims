import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GRNForm } from "../GRNForm";

vi.mock("../queries", () => ({
  useSuppliers: vi.fn(),
  useStores: vi.fn(),
  useItems: vi.fn(),
  usePostGrn: vi.fn(),
}));

import {
  useSuppliers,
  useStores,
  useItems,
  usePostGrn,
} from "../queries";

function renderGRN() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <GRNForm />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useSuppliers as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, code: "SUP1", description: "Supplier 1" }] });
  (useStores as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, code: "STR1", description: "Main Store" }] });
  (useItems as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, itemName: "Paracetamol" }] });
  (usePostGrn as ReturnType<typeof vi.fn>).mockReturnValue({ mutate: vi.fn(), isPending: false });
});

describe("GRNForm", () => {
  it("renders the heading and New GRN button", () => {
    renderGRN();
    expect(screen.getByText("Goods Receipt Notes")).toBeInTheDocument();
    expect(screen.getByText("New GRN")).toBeInTheDocument();
  });

  it("shows the form when New GRN is clicked", async () => {
    const user = userEvent.setup();
    renderGRN();
    await user.click(screen.getByText("New GRN"));
    expect(screen.getByText("New GRN")).toBeInTheDocument();
    expect(screen.getByText("Line Items")).toBeInTheDocument();
  });

  it("shows store, supplier, and item selects", async () => {
    const user = userEvent.setup();
    renderGRN();
    await user.click(screen.getByText("New GRN"));
    expect(screen.getByLabelText(/Store/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Supplier/i)).toBeInTheDocument();
  });

  it("shows provisional and consignment checkboxes", async () => {
    const user = userEvent.setup();
    renderGRN();
    await user.click(screen.getByText("New GRN"));
    expect(screen.getByLabelText(/Provisional/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Consignment/i)).toBeInTheDocument();
  });
});
