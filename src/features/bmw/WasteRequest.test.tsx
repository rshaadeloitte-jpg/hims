import { vi, describe, expect, it, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { WasteRequest } from "./WasteRequest";

const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
});

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

vi.mock("./queries", () => ({
  useStores: () => ({
    data: [
      { id: 1, description: "Ward A", isBioMedicalStore: false },
      { id: 2, description: "BMW Store", isBioMedicalStore: true },
    ],
    isLoading: false,
  }),
  useWasteBags: () => ({
    data: [
      { id: 1, description: "Yellow Bag" },
      { id: 2, description: "Red Bag" },
    ],
    isLoading: false,
  }),
  useCreateWasteRequest: () => ({
    mutate: vi.fn(),
    isPending: false,
    isError: false,
    error: null,
  }),
}));

describe("WasteRequest", () => {
  beforeEach(() => {
    queryClient.clear();
  });

  it("renders the create request form", () => {
    render(
      <WasteRequest unitId={1} defaultStoreId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByText("Create Biomedical Waste Request")).toBeInTheDocument();
  });

  it("renders store and date fields", () => {
    render(
      <WasteRequest unitId={1} defaultStoreId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByLabelText(/from store/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date/i)).toBeInTheDocument();
  });

  it("renders the add bag button", () => {
    render(
      <WasteRequest unitId={1} defaultStoreId={1} />,
      { wrapper: Wrapper },
    );
    expect(screen.getByRole("button", { name: /add bag/i })).toBeInTheDocument();
  });

  it("shows error when submitting without bags", async () => {
    render(
      <WasteRequest unitId={1} defaultStoreId={1} />,
      { wrapper: Wrapper },
    );
    // Try to submit without adding bags
    const submitBtn = screen.getByRole("button", { name: /create request/i });
    fireEvent.click(submitBtn);
    expect(screen.getByText(/add at least one waste bag/i)).toBeInTheDocument();
  });

  it("renders cancel button that calls onCancel", () => {
    const onCancel = vi.fn();
    render(
      <WasteRequest unitId={1} defaultStoreId={1} onCancel={onCancel} />,
      { wrapper: Wrapper },
    );
    fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalled();
  });
});
