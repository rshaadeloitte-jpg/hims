import { render, screen, fireEvent } from "@testing-library/react";
import { PrescriptionList } from "./PrescriptionList";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

// Mock API response
const mockPrescriptions = [
  {
    id: 1,
    patientId: 101,
    patientName: "John Doe",
    encounterId: 2001,
    status: "PENDING" as const,
    createdAt: "2026-09-10T10:00:00Z",
    items: [
      {
        id: 1001,
        drugId: 5001,
        drugName: "Amoxicillin",
        strength: "500mg",
        dosage: "1 tablet twice daily",
        quantity: 10,
        instructions: "Take with food",
      },
    ],
  },
];

jest.mock("./queries", () => ({
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
    expect(screen.getByText("John Doe")).toBeInTheDocument();
    expect(screen.getByText("PENDING")).toBeInTheDocument();
    expect(screen.getByText("Dispense")).toBeInTheDocument();
  });

  test("shows dispense form when prescription selected", () => {
    render(<PrescriptionList />, { wrapper: Wrapper });

    fireEvent.click(screen.getByText("Dispense"));
    expect(screen.getByText("Dispense Prescription #1")).toBeInTheDocument();
  });

  test("disables dispense button for non-pending prescriptions", () => {
    const mockCompleted = [
      {
        ...mockPrescriptions[0],
        id: 2,
        status: "DISPENSED" as const,
      },
    ];

    jest.spyOn(require("./queries"), "usePrescriptions").mockReturnValue({
      data: mockCompleted,
      isLoading: false,
    });

    render(<PrescriptionList />, { wrapper: Wrapper });

    expect(screen.getByText("View")).toBeInTheDocument();
    expect(screen.queryByText("Dispense")).not.toBeInTheDocument();
  });
});