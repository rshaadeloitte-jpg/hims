import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BcmaPage } from "./BcmaPage";

describe("BcmaPage", () => {
  it("shows scan history disabled message", () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={qc}><BcmaPage /></QueryClientProvider>);
    expect(screen.getByText(/not available/i)).toBeInTheDocument();
  });
});
