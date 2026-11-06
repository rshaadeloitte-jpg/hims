import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { PORequest } from "../PORequest";

describe("PORequest", () => {
  it("renders the PO request form", () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={qc}><PORequest /></QueryClientProvider>);
    expect(screen.getByText(/Purchase Order|PO Requisition|Requisition/i)).toBeInTheDocument();
  });
});
