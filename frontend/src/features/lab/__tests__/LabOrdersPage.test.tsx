import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter } from "react-router-dom";
import { LabOrdersPage } from "../LabOrdersPage";
import * as queries from "../queries";

function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}

describe("LabOrdersPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders worklist table with PathOrderBooking columns (Patient, Status, Date not blank)", () => {
    vi.spyOn(queries, "useLabOrders").mockReturnValue({
      data: [
        {
          bookingId: 10,
          bookingNo: "LAB-10",
          patientId: 505,
          encounterId: 7777,
          bookingStatus: 1, // ORDERED
          isOutSourced: false,
          addedDatetime: "2024-01-01T10:30:00Z",
        },
        {
          bookingId: 11,
          bookingNo: "LAB-11",
          patientId: 506,
          encounterId: 7778,
          bookingStatus: 2, // SAMPLE_COLLECTED
          isOutSourced: true,
          addedDatetime: "2024-01-02T14:15:00Z",
        },
        {
          bookingId: 12,
          bookingNo: "LAB-12",
          patientId: 507,
          encounterId: null,
          bookingStatus: 3, // IN_PROGRESS
          isOutSourced: false,
          addedDatetime: "2024-01-03T09:00:00Z",
        },
        {
          bookingId: 13,
          bookingNo: "LAB-13",
          patientId: 508,
          bookingStatus: 4, // FINALIZED
          isOutSourced: false,
          addedDatetime: "2024-01-04T12:00:00Z",
        },
        {
          bookingId: 14,
          bookingNo: "LAB-14",
          patientId: 509,
          bookingStatus: 5, // CANCELLED
          isOutSourced: false,
          addedDatetime: "2024-01-05T16:45:00Z",
        },
      ],
      isLoading: false,
    } as any);

    vi.spyOn(queries, "useLabOrder").mockReturnValue({ data: null } as any);

    renderWithProviders(<LabOrdersPage />);

    expect(screen.getByText("Lab Orders")).toBeInTheDocument();

    // Column headers
    expect(screen.getByText("#")).toBeInTheDocument();
    expect(screen.getByText("Patient")).toBeInTheDocument();
    expect(screen.getByText("Status")).toBeInTheDocument();
    expect(screen.getByText("Outsourced")).toBeInTheDocument();
    expect(screen.getByText("Date")).toBeInTheDocument();

    // Check Row 1
    expect(screen.getByText("LAB-10")).toBeInTheDocument();
    expect(screen.getByText("Patient #505")).toBeInTheDocument();
    expect(screen.getByText("ORDERED")).toBeInTheDocument();
    expect(screen.getByText("2024-01-01")).toBeInTheDocument();

    // Check Row 2
    expect(screen.getByText("LAB-11")).toBeInTheDocument();
    expect(screen.getByText("Patient #506")).toBeInTheDocument();
    expect(screen.getByText("SAMPLE_COLLECTED")).toBeInTheDocument();
    expect(screen.getByText("Yes")).toBeInTheDocument();
    expect(screen.getByText("2024-01-02")).toBeInTheDocument();

    // Check Row 3
    expect(screen.getByText("LAB-12")).toBeInTheDocument();
    expect(screen.getByText("Patient #507")).toBeInTheDocument();
    expect(screen.getByText("IN_PROGRESS")).toBeInTheDocument();
    expect(screen.getByText("2024-01-03")).toBeInTheDocument();

    // Check Row 4
    expect(screen.getByText("LAB-13")).toBeInTheDocument();
    expect(screen.getByText("Patient #508")).toBeInTheDocument();
    expect(screen.getByText("FINALIZED")).toBeInTheDocument();
    expect(screen.getByText("2024-01-04")).toBeInTheDocument();

    // Check Row 5
    expect(screen.getByText("LAB-14")).toBeInTheDocument();
    expect(screen.getByText("Patient #509")).toBeInTheDocument();
    expect(screen.getByText("CANCELLED")).toBeInTheDocument();
    expect(screen.getByText("2024-01-05")).toBeInTheDocument();
  });

  it("selects order on row click and shows workflow details", () => {
    vi.spyOn(queries, "useLabOrders").mockReturnValue({
      data: [
        {
          bookingId: 10,
          bookingNo: "LAB-10",
          patientId: 505,
          bookingStatus: 1,
          isOutSourced: false,
          addedDatetime: "2024-01-01T10:30:00Z",
        },
      ],
      isLoading: false,
    } as any);

    vi.spyOn(queries, "useLabOrder").mockImplementation(((id: number) => {
      if (id === 10) {
        return {
          data: {
            bookingId: 10,
            bookingNo: "LAB-10",
            patientId: 505,
            bookingStatus: 1,
            isOutSourced: false,
            details: [
              {
                detailId: 21,
                bookingId: 10,
                testCode: "CBC",
                testName: "Complete Blood Count",
                detailStatus: 1,
                barcode: "2024010000001",
              },
            ],
          },
        };
      }
      return { data: null };
    }) as any);

    renderWithProviders(<LabOrdersPage />);

    // Click row
    fireEvent.click(screen.getByText("LAB-10"));

    expect(screen.getByText("Order #LAB-10 — Workflow")).toBeInTheDocument();
    expect(screen.getByText("Complete Blood Count")).toBeInTheDocument();
    expect(screen.getByText("2024010000001")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Collect" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Start" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });
});
