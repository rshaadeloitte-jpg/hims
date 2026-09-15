import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { userEvent } from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { KioskAppointment } from "./KioskAppointment";

const mockDepts = [
  { id: 1, name: "Cardiology" },
  { id: 2, name: "Neurology" },
];

const mockDoctors = [
  { id: 10, name: "Dr. Smith", departmentId: 1 },
  { id: 11, name: "Dr. Jones", departmentId: 1 },
];

const mockSlots = [
  { slotId: 100, startTime: "09:00", endTime: "09:30", available: true },
  { slotId: 101, startTime: "10:00", endTime: "10:30", available: false },
];

vi.mock("./queries", () => ({
  useDepartments: vi.fn(),
  useDoctors: vi.fn(),
  useSlots: vi.fn(),
  useBookAppointment: vi.fn(),
}));

const queries = await import("./queries");
const mockBookMutate = vi.fn();

function renderComponent() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <KioskAppointment />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  (queries.useDepartments as ReturnType<typeof vi.fn>).mockReturnValue({
    data: mockDepts,
    isLoading: false,
  });
  (queries.useDoctors as ReturnType<typeof vi.fn>).mockReturnValue({
    data: mockDoctors,
    isLoading: false,
  });
  (queries.useSlots as ReturnType<typeof vi.fn>).mockReturnValue({
    data: mockSlots,
    isLoading: false,
  });
  (queries.useBookAppointment as ReturnType<typeof vi.fn>).mockReturnValue({
    mutate: mockBookMutate,
    isPending: false,
  });
});

describe("KioskAppointment", () => {
  it("renders the appointment booking form", () => {
    renderComponent();
    expect(screen.getByText("Book Appointment")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Full name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("10-digit mobile")).toBeInTheDocument();
    expect(screen.getByText("Select Department")).toBeInTheDocument();
    expect(screen.getByText("Select Doctor")).toBeInTheDocument();
    expect(screen.getByText("Select Slot")).toBeInTheDocument();
    expect(screen.getByText("Book Appointment")).toBeInTheDocument();
  });

  it("shows validation errors for empty form", async () => {
    const user = userEvent.setup();
    renderComponent();

    await user.click(screen.getByText("Book Appointment"));

    await waitFor(() => {
      expect(screen.getByText("Patient name required")).toBeInTheDocument();
      expect(screen.getByText("Invalid 10-digit mobile")).toBeInTheDocument();
      expect(screen.getByText("Department required")).toBeInTheDocument();
    });
  });

  it("submits valid form and shows success with token", async () => {
    const user = userEvent.setup();
    mockBookMutate.mockImplementation((_data: unknown, opts: { onSuccess: (d: { tokenNo: string }) => void }) => {
      opts.onSuccess({ tokenNo: "APT-001" });
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("Full name"), "Jane Doe");
    await user.type(screen.getByPlaceholderText("10-digit mobile"), "9876543210");

    // Select department
    const deptSelect = screen.getByText("Select Department").closest("select") as HTMLSelectElement;
    await user.selectOptions(deptSelect, "1");

    // Select doctor
    const docSelect = screen.getByText("Select Doctor").closest("select") as HTMLSelectElement;
    await user.selectOptions(docSelect, "10");

    // Set date
    const dateInput = screen.getByText("Date *").closest(".field")?.querySelector("input[type=date]") as HTMLInputElement;
    await user.type(dateInput, "2026-09-15");

    // Select slot
    const slotSelect = screen.getByText("Select Slot").closest("select") as HTMLSelectElement;
    await user.selectOptions(slotSelect, "100");

    await user.click(screen.getByText("Book Appointment"));

    await waitFor(() => {
      expect(mockBookMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          patientName: "Jane Doe",
          mobileNo: "9876543210",
          departmentId: 1,
          doctorId: 10,
          slotId: 100,
        }),
        expect.any(Object),
      );
    });

    await waitFor(() => {
      expect(screen.getByText("Appointment Booked")).toBeInTheDocument();
      expect(screen.getByText("Token: APT-001")).toBeInTheDocument();
    });
  });

  it("resets form after successful booking", async () => {
    const user = userEvent.setup();
    mockBookMutate.mockImplementation((_data: unknown, opts: { onSuccess: (d: { tokenNo: string }) => void }) => {
      opts.onSuccess({ tokenNo: "APT-001" });
    });
    renderComponent();

    await user.type(screen.getByPlaceholderText("Full name"), "Jane Doe");
    await user.type(screen.getByPlaceholderText("10-digit mobile"), "9876543210");
    const deptSelect = screen.getByText("Select Department").closest("select") as HTMLSelectElement;
    await user.selectOptions(deptSelect, "1");
    const docSelect = screen.getByText("Select Doctor").closest("select") as HTMLSelectElement;
    await user.selectOptions(docSelect, "10");
    const dateInput = screen.getByText("Date *").closest(".field")?.querySelector("input[type=date]") as HTMLInputElement;
    await user.type(dateInput, "2026-09-15");
    const slotSelect = screen.getByText("Select Slot").closest("select") as HTMLSelectElement;
    await user.selectOptions(slotSelect, "100");
    await user.click(screen.getByText("Book Appointment"));

    await waitFor(() => {
      expect(screen.getByText("Appointment Booked")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Book Another"));

    await waitFor(() => {
      expect(screen.getByPlaceholderText("Full name")).toHaveValue("");
      expect(screen.getByPlaceholderText("10-digit mobile")).toHaveValue("");
    });
  });
});
