import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { QuotationComparison } from "../QuotationComparison";

vi.mock("../queries", () => ({
  useSuppliers: vi.fn(),
  useItems: vi.fn(),
  useCreateQuotation: vi.fn(),
  useQuotationCompare: vi.fn(),
}));

import {
  useSuppliers,
  useItems,
  useCreateQuotation,
  useQuotationCompare,
} from "../queries";

function renderQC() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <QuotationComparison />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (useSuppliers as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, code: "SUP1", description: "Supplier 1" }] });
  (useItems as ReturnType<typeof vi.fn>).mockReturnValue({ data: [{ id: 1, itemName: "Paracetamol" }] });
  (useCreateQuotation as ReturnType<typeof vi.fn>).mockReturnValue({ mutate: vi.fn(), isPending: false });
  (useQuotationCompare as ReturnType<typeof vi.fn>).mockReturnValue({ data: [], isLoading: false, isError: false });
});

describe("QuotationComparison", () => {
  it("renders the heading and New Quotation button", () => {
    renderQC();
    expect(screen.getByText("Quotation Comparison")).toBeInTheDocument();
    expect(screen.getByText("New Quotation")).toBeInTheDocument();
  });

  it("shows the form when New Quotation is clicked", async () => {
    const user = userEvent.setup();
    renderQC();
    await user.click(screen.getByText("New Quotation"));
    expect(screen.getByText("New Quotation")).toBeInTheDocument();
    expect(screen.getByText("Line Items")).toBeInTheDocument();
  });

  it("shows supplier and item selects", async () => {
    const user = userEvent.setup();
    renderQC();
    await user.click(screen.getByText("New Quotation"));
    expect(screen.getByLabelText(/Supplier/i)).toBeInTheDocument();
  });

  it("shows the compare section", () => {
    renderQC();
    expect(screen.getByText("Compare Quotations")).toBeInTheDocument();
  });
});
