import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { InteropNewPage } from "./InteropNewPage";

describe("InteropNewPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [{ id: 1, drugCode: "D1", drugName: "Drug One", syncStatus: 0 }],
    }));
  });
  afterEach(() => vi.unstubAllGlobals());

  it("renders sync status from backend fields", async () => {
    const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    render(<QueryClientProvider client={qc}><InteropNewPage /></QueryClientProvider>);
    await waitFor(() => expect(screen.getByText("D1")).toBeInTheDocument());
    expect(screen.getByText("Drug One")).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
  });
});
