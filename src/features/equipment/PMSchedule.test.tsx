import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PMSchedule } from "./PMSchedule";

/**
 * Component test for PMSchedule — verifies PM schedule creation,
 * completion recording, history view, and checklist display.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    usePmSchedules: vi.fn(),
    useCreatePmSchedule: vi.fn(),
    usePmHistory: vi.fn(),
    useCompletePm: vi.fn(),
    usePmChecklists: vi.fn(),
  };
});

const queries = await import("./queries");
const usePmSchedules = queries.usePmSchedules as unknown as ReturnType<typeof vi.fn>;
const useCreatePmSchedule = queries.useCreatePmSchedule as unknown as ReturnType<typeof vi.fn>;
const usePmHistory = queries.usePmHistory as unknown as ReturnType<typeof vi.fn>;
const useCompletePm = queries.useCompletePm as unknown as ReturnType<typeof vi.fn>;
const usePmChecklists = queries.usePmChecklists as unknown as ReturnType<typeof vi.fn>;

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <PMSchedule />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  usePmSchedules.mockReturnValue({ data: [], isLoading: false, error: null });
  useCreatePmSchedule.mockReturnValue({ mutate: vi.fn(), isPending: false });
  usePmHistory.mockReturnValue({ data: [] });
  useCompletePm.mockReturnValue({ mutate: vi.fn(), isPending: false });
  usePmChecklists.mockReturnValue({ data: [] });
});

describe("PMSchedule", () => {
  it("renders empty state when no schedules", () => {
    renderPage();
    expect(screen.getByText(/No PM schedules/)).toBeInTheDocument();
  });

  it("renders schedule list with maintenance type badges", () => {
    usePmSchedules.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          scheduleCode: "IHMS/EQ/PM/2026/00001",
          frequency: 3,
          pmType: 1,
          nextDueDate: "2026-04-15",
          lastCompletedDate: "2026-01-15",
          jan: true,
          feb: false,
          mar: false,
          apr: true,
          may: false,
          jun: false,
          jul: false,
          aug: false,
          sep: false,
          oct: false,
          nov: false,
          dec: false,
        },
        {
          id: 2,
          equipmentId: 11,
          scheduleCode: "IHMS/EQ/PM/2026/00002",
          frequency: 6,
          pmType: 3,
          nextDueDate: "2026-07-01",
          lastCompletedDate: null,
          jan: false,
          feb: false,
          mar: false,
          apr: false,
          may: false,
          jun: true,
          jul: false,
          aug: false,
          sep: false,
          oct: false,
          nov: false,
          dec: false,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    expect(screen.getByText("IHMS/EQ/PM/2026/00001")).toBeInTheDocument();
    expect(screen.getByText("Preventive")).toBeInTheDocument();
    expect(screen.getByText("Calibration")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    usePmSchedules.mockReturnValue({ data: [], isLoading: true, error: null });
    renderPage();
    expect(screen.getByText(/Loading PM schedules/)).toBeInTheDocument();
  });

  it("opens create schedule form", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /New PM Schedule/ }));
    expect(screen.getByText(/Create PM Schedule/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Equipment ID/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Frequency/)).toBeInTheDocument();
  });

  it("validates required fields on create", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn();
    useCreatePmSchedule.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /New PM Schedule/ }));

    // Clear frequency to trigger validation error
    const freq = screen.getByLabelText(/Frequency/);
    await user.clear(freq);
    await user.click(screen.getByRole("button", { name: /Create Schedule/ }));

    expect(screen.getByText(/Equipment is required/)).toBeInTheDocument();
    expect(screen.getByText(/Frequency \(months\) required/)).toBeInTheDocument();
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("submits valid schedule", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useCreatePmSchedule.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /New PM Schedule/ }));

    await user.type(screen.getByLabelText(/Equipment ID/), "10");
    const freq = screen.getByLabelText(/Frequency/);
    await user.clear(freq);
    await user.type(freq, "3");
    await user.type(screen.getByLabelText(/PM Start Date/), "2026-01-15");

    await user.click(screen.getByRole("button", { name: /Create Schedule/ }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledTimes(1);
    });
    const [payload] = mockMutate.mock.calls[0];
    expect(payload).toMatchObject({
      equipmentId: 10,
      frequency: 3,
      pmDate: "2026-01-15",
    });
  });

  it("opens completion form", async () => {
    const user = userEvent.setup();
    usePmSchedules.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          scheduleCode: "SCHED-001",
          frequency: 3,
          pmType: 1,
          nextDueDate: "2026-04-15",
          lastCompletedDate: null,
          jan: true,
          feb: false,
          mar: false,
          apr: true,
          may: false,
          jun: false,
          jul: false,
          aug: false,
          sep: false,
          oct: false,
          nov: false,
          dec: false,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Complete/ }));

    expect(screen.getByText(/Record Completion — Schedule #1/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Maintenance No./)).toBeInTheDocument();
    expect(screen.getByLabelText(/Functional Check Remark/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Calibration/)).toBeInTheDocument();
  });

  it("submits completion", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useCompletePm.mockReturnValue({ mutate: mockMutate, isPending: false });

    usePmSchedules.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          scheduleCode: "SCHED-001",
          frequency: 3,
          pmType: 1,
          nextDueDate: "2026-04-15",
          lastCompletedDate: null,
          jan: true,
          feb: false,
          mar: false,
          apr: true,
          may: false,
          jun: false,
          jul: false,
          aug: false,
          sep: false,
          oct: false,
          nov: false,
          dec: false,
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Complete/ }));

    await user.type(screen.getByLabelText(/Maintenance No./), "PM-2026-00001");
    await user.type(screen.getByLabelText(/Maintenance Date/), "2026-03-15");
    await user.type(screen.getByLabelText(/Functional Check Remark/), "All OK");
    await user.type(screen.getByLabelText(/Calibration/), "Within spec");

    await user.click(screen.getByRole("button", { name: /Record Completion/ }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledTimes(1);
    });
    const [{ scheduleId, payload }] = mockMutate.mock.calls[0];
    expect(scheduleId).toBe(1);
    expect(payload).toMatchObject({
      maintenanceNo: "PM-2026-00001",
      functionalCheckRemark: "All OK",
      calibration: "Within spec",
    });
  });

  it("shows execution history", async () => {
    const user = userEvent.setup();
    usePmSchedules.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          scheduleCode: "SCHED-001",
          frequency: 3,
          pmType: 1,
          nextDueDate: "2026-04-15",
          lastCompletedDate: "2026-01-15",
          jan: true,
          feb: false,
          mar: false,
          apr: true,
          may: false,
          jun: false,
          jul: false,
          aug: false,
          sep: false,
          oct: false,
          nov: false,
          dec: false,
        },
      ],
      isLoading: false,
      error: null,
    });
    usePmHistory.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          pmScheduleId: 1,
          maintenanceNo: "PM-2026-00001",
          maintenanceDate: "2026-01-15",
          performedBy: 5,
          pmStatus: 3,
          amountIncurred: 2500,
          calibration: "OK",
        },
      ],
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /History/ }));

    expect(screen.getByText(/Execution History — Schedule #1/)).toBeInTheDocument();
    expect(screen.getByText("PM-2026-00001")).toBeInTheDocument();
    expect(screen.getByText("Completed")).toBeInTheDocument();
  });

  it("shows PM checklist templates", () => {
    usePmChecklists.mockReturnValue({
      data: [
        { id: 1, code: "CHK-001", description: "Check oil level" },
        { id: 2, code: "CHK-002", description: "Inspect cables" },
      ],
    });

    renderPage();
    expect(screen.getByText(/PM Checklist Templates/)).toBeInTheDocument();
    expect(screen.getByText("CHK-001")).toBeInTheDocument();
    expect(screen.getByText("Check oil level")).toBeInTheDocument();
  });
});
