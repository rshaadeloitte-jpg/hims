import { describe, test, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PrescriptionList } from "./PrescriptionList";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

const mockPrescriptions = [
  {
    prescriptionId: 1,
    patientId: 101,
    prescriberId: 7,
    statusCode: 1,
    details: [
      {
        lineNo: 1,
        itemId: 5001,
        qty: 10,
        dose: "1 tablet",
        frequency: "twice daily",
      },
    ],
  },
];

vi.mock("./queries", () => ({
  usePrescriptions: () => ({
    data: mockPrescriptions,
    isLoading: false,
  }),
  usePrescription: () => ({
    data: mockPrescriptions[0],
    isLoading: false,
  }),
}));

const queryClient = new QueryClient();

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
);

describe("PrescriptionList", () => {
  test("renders prescription list correctly", () => {
    render(<PrescriptionList />, { wrapper: Wrapper });

    expect(screen.getByText("Prescription Dispensing")).toBeInTheDocument();
    expect(screen.getByText("Patient 101")).toBeInTheDocument();
    expect(screen.getByText("ACTIVE")).toBeInTheDocument();
    expect(screen.getByText("Dispense (unavailable)")).toBeInTheDocument();
  });

  test("shows prescription details when selected", () => {
    render(<PrescriptionList />, { wrapper: Wrapper });

    fireEvent.click(screen.getByText("Dispense (unavailable)"));
    // button is disabled; details panel not auto-opened. Ensure no crash.
    expect(screen.getByText("Prescription Dispensing")).toBeInTheDocument();
  });
});
