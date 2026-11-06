import { vi, describe, it, expect, beforeEach, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ChemotherapyPage } from "./ChemotherapyPage";

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe("ChemotherapyPage", () => {
  beforeEach(() => {
    queryClient.clear();
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 401, error: "Unauthorized" }), {
          status: 401,
          headers: { "Content-Type": "application/json" },
        }),
      ),
    );
  });
  afterEach(() => vi.unstubAllGlobals());

  it("does not crash when the API returns a 401 object", async () => {
    render(<ChemotherapyPage />, { wrapper: Wrapper });
    expect(await screen.findByRole("heading", { name: /chemotherapy/i })).toBeInTheDocument();
  });
});
