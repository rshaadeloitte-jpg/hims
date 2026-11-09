import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { RcmNewPage } from "./RcmNewPage";

describe("RcmNewPage", () => {
  it("shows disabled claim controls message", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue({ ok: true, json: async () => [] } as Response);
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={qc}><RcmNewPage /></QueryClientProvider>);
    await waitFor(() => expect(screen.getByText(/not available/i)).toBeInTheDocument());
  });
});
