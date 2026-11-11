import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { OrdersPage } from "../OrdersPage";
import * as queries from "../queries";

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("OrdersPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders orders list with real ClinicalOrder fields", () => {
    vi.spyOn(queries, "useClinicalOrders").mockReturnValue({
      data: [
        {
          orderId: 42,
          patientId: 501,
          prescriberId: 11,
          encounterId: 7001,
          admissionId: null,
          statusCode: 1,
          grossTotal: 700,
          concessionPercent: 10,
          concessionAmount: 70,
          netTotal: 630,
          items: [
            {
              itemType: "LAB",
              itemId: 10,
              serviceName: "CBC",
              rate: 500,
              qty: 1,
            },
          ],
        },
        {
          orderId: 43,
          patientId: 502,
          prescriberId: 12,
          encounterId: null,
          admissionId: 9002,
          statusCode: 2,
          netTotal: 1500,
          items: [
            {
              itemType: "RADIOLOGY",
              itemId: 15,
              serviceName: "MRI Brain",
              rate: 1500,
              qty: 1,
            },
          ],
        },
      ],
      isLoading: false,
    } as any);

    renderWithProviders(<OrdersPage />);

    expect(screen.getByText("Clinical Orders")).toBeInTheDocument();
    expect(screen.getByText("New Order")).toBeInTheDocument();

    // Table headers
    expect(screen.getByText("Patient")).toBeInTheDocument();
    expect(screen.getByText("Prescriber")).toBeInTheDocument();
    expect(screen.getByText("Context")).toBeInTheDocument();
    expect(screen.getByText("Net Total")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();

    // Row 1
    expect(screen.getByText("42")).toBeInTheDocument();
    expect(screen.getByText("Patient #501")).toBeInTheDocument();
    expect(screen.getByText("Doctor #11")).toBeInTheDocument();
    expect(screen.getByText("Encounter #7001")).toBeInTheDocument();
    expect(screen.getByText("CBC (LAB)")).toBeInTheDocument();
    expect(screen.getByText("₹630.00")).toBeInTheDocument();
    expect(screen.getByText("INITIATED")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();

    // Row 2
    expect(screen.getByText("43")).toBeInTheDocument();
    expect(screen.getByText("Patient #502")).toBeInTheDocument();
    expect(screen.getByText("Doctor #12")).toBeInTheDocument();
    expect(screen.getByText("Admission #9002")).toBeInTheDocument();
    expect(screen.getByText("MRI Brain (RADIOLOGY)")).toBeInTheDocument();
    expect(screen.getByText("₹1500.00")).toBeInTheDocument();
    expect(screen.getByText("CONFIRMED")).toBeInTheDocument();
  });

  it("toggles between Encounter and Admission in New Clinical Order form", () => {
    vi.spyOn(queries, "useClinicalOrders").mockReturnValue({ data: [], isLoading: false } as any);
    renderWithProviders(<OrdersPage />);

    fireEvent.click(screen.getByText("New Order"));
    expect(screen.getByText("New Clinical Order")).toBeInTheDocument();

    // Initially Encounter is active
    expect(screen.getByText(/Encounter ID/)).toBeInTheDocument();

    // Click Admission
    fireEvent.click(screen.getByRole("button", { name: "Admission" }));
    expect(screen.getByText(/Admission ID/)).toBeInTheDocument();

    // Click Encounter back
    fireEvent.click(screen.getByRole("button", { name: "Encounter" }));
    expect(screen.getByText(/Encounter ID/)).toBeInTheDocument();
  });

  it("allows adding and removing item rows", () => {
    vi.spyOn(queries, "useClinicalOrders").mockReturnValue({ data: [], isLoading: false } as any);
    renderWithProviders(<OrdersPage />);

    fireEvent.click(screen.getByText("New Order"));
    expect(screen.getAllByPlaceholderText("e.g. Complete Blood Count")).toHaveLength(1);

    // Add item
    fireEvent.click(screen.getByText("+ Add Item"));
    expect(screen.getAllByPlaceholderText("e.g. Complete Blood Count")).toHaveLength(2);

    // Remove item
    const removeButtons = screen.getAllByRole("button", { name: "Remove" });
    expect(removeButtons).toHaveLength(2);
    fireEvent.click(removeButtons[0]);
    expect(screen.getAllByPlaceholderText("e.g. Complete Blood Count")).toHaveLength(1);
  });

  it("submits valid order with backend field names", async () => {
    const mockMutate = vi.fn();
    vi.spyOn(queries, "useClinicalOrders").mockReturnValue({ data: [], isLoading: false } as any);
    vi.spyOn(queries, "useCreateClinicalOrder").mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as any);

    renderWithProviders(<OrdersPage />);

    fireEvent.click(screen.getByText("New Order"));

    // Fill form
    fireEvent.change(screen.getByPlaceholderText("e.g. 501"), { target: { value: "501" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. 11"), { target: { value: "11" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. 7001"), { target: { value: "7001" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. Complete Blood Count"), {
      target: { value: "Complete Blood Count" },
    });
    fireEvent.change(screen.getByDisplayValue("0"), { target: { value: "450" } });

    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));

    expect(mockMutate).toHaveBeenCalledTimes(1);
    const sentPayload = mockMutate.mock.calls[0][0];
    expect(sentPayload).toEqual({
      patientId: 501,
      prescriberId: 11,
      encounterId: 7001,
      admissionId: undefined,
      wardId: undefined,
      concessionPercent: undefined,
      pharmacyPrescriptionId: undefined,
      items: [
        {
          itemType: "LAB",
          itemId: 1,
          serviceName: "Complete Blood Count",
          rate: 450,
          qty: 1,
          doctorSharePercent: undefined,
        },
      ],
    });
  });

  it("renders backend error message when submission fails", async () => {
    let capturedOnError: ((err: any) => void) | undefined;
    const mockMutate = vi.fn((_payload, options) => {
      capturedOnError = options?.onError;
    });

    vi.spyOn(queries, "useClinicalOrders").mockReturnValue({ data: [], isLoading: false } as any);
    vi.spyOn(queries, "useCreateClinicalOrder").mockReturnValue({
      mutate: mockMutate,
      isPending: false,
    } as any);

    renderWithProviders(<OrdersPage />);

    fireEvent.click(screen.getByText("New Order"));

    fireEvent.change(screen.getByPlaceholderText("e.g. 501"), { target: { value: "501" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. 11"), { target: { value: "11" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. 7001"), { target: { value: "7001" } });
    fireEvent.change(screen.getByPlaceholderText("e.g. Complete Blood Count"), {
      target: { value: "Complete Blood Count" },
    });

    fireEvent.click(screen.getByRole("button", { name: "Place Order" }));

    expect(mockMutate).toHaveBeenCalled();

    // Trigger backend 400 error
    capturedOnError?.({ message: "encounterId and admissionId are mutually exclusive — supply at most one" });

    await waitFor(() => {
      expect(
        screen.getByText("encounterId and admissionId are mutually exclusive — supply at most one"),
      ).toBeInTheDocument();
    });
  });
});
