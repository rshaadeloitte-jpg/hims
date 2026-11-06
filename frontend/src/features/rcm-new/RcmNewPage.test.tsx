import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RcmNewPage } from "./RcmNewPage";

describe("RcmNewPage", () => {
  it("shows disabled claim controls message", () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={qc}><RcmNewPage /></QueryClientProvider>);
    expect(screen.getByText(/not available/i)).toBeInTheDocument();
  });
});
