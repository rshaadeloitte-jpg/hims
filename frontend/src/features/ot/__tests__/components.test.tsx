import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SurgeryScheduler } from "../SurgeryScheduler";
import { OTDashboard } from "../OTDashboard";
import { AnesthesiaRecord } from "../AnesthesiaRecord";
import { SurgicalChecklist } from "../SurgicalChecklist";

// Mock the queries module
const mockSchedules = [
  {
    scheduleId: 1,
    patientId: 100,
    patientName: "John Doe",
    otTable: "OT-1",
    scheduledStart: "2026-09-11T08:00:00.000+00:00",
    scheduledEnd: "2026-09-11T10:00:00.000+00:00",
    status: 1,
    details: [{ detailId: 1, procedureCode: "APPEND", procedureName: "Appendectomy" }],
  },
];

const mockEmpty: never[] = [];

vi.mock("../queries", () => ({
  useOtSchedules: vi.fn(() => ({ data: mockSchedules, isLoading: false })),
  useCreateOtSchedule: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useStartSurgery: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCompleteSurgery: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useCancelSurgery: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useOtDashboard: vi.fn(() => ({ data: { rooms: [], schedules: mockSchedules }, isLoading: false })),
  useAnesthesiaRecords: vi.fn(() => ({ data: [], isLoading: false })),
  useCreatePreOp: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useAddVitalSign: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useVitalSigns: vi.fn(() => ({ data: [], isLoading: false })),
  useAddAnesthesiaDrug: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useAnesthesiaDrugs: vi.fn(() => ({ data: [], isLoading: false })),
  useCompletePostOp: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useChecklists: vi.fn(() => ({ data: [], isLoading: false })),
  useChecklistsBySchedule: vi.fn(() => ({ data: [], isLoading: false })),
  useCreateChecklist: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useAddChecklistItem: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useCompleteChecklistItem: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  useRecordChecklistCompletion: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useOtRooms: vi.fn(() => ({ data: [], isLoading: false })),
  useStaffSchedule: vi.fn(() => ({ data: [], isLoading: false })),
  useNurseSchedule: vi.fn(() => ({ data: [], isLoading: false })),
  useBillingEstimates: vi.fn(() => ({ data: [], isLoading: false })),
  useCreateBillingEstimate: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useFinalizeBillingEstimate: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useAssignStaff: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useCreateNurseDuty: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useUpdateOtScheduleStatus: vi.fn(() => ({ mutate: vi.fn(), isPending: false })),
  otQueryKeys: {
    rooms: () => ["ot", "rooms"],
    schedules: () => ["ot", "schedules"],
    schedule: (id: number) => ["ot", "schedules", id],
    anesthesia: () => ["ot", "anesthesia"],
    vitals: (id: number) => ["ot", "anesthesia", id, "vitals"],
    drugs: (id: number) => ["ot", "anesthesia", id, "drugs"],
    checklists: () => ["ot", "checklists"],
    checklist: (id: number) => ["ot", "checklists", id],
    billingEstimates: () => ["ot", "billing", "estimates"],
    staffSchedule: (id: number) => ["ot", "staff-schedule", id],
    nurseSchedule: (id: number) => ["ot", "nurse-schedule", id],
    dashboard: () => ["ot", "dashboard"],
  },
}));

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe("SurgeryScheduler", () => {
  it("renders the scheduler header", () => {
    renderWithQuery(<SurgeryScheduler />);
    expect(screen.getByText("Surgery Scheduler")).toBeInTheDocument();
    expect(screen.getByText("Linear calendar view — book OT slots by date")).toBeInTheDocument();
  });

  it("renders the calendar grid", () => {
    renderWithQuery(<SurgeryScheduler />);
    expect(screen.getByLabelText("surgery-calendar")).toBeInTheDocument();
  });

  it("toggles the new surgery form", async () => {
    renderWithQuery(<SurgeryScheduler />);
    const button = screen.getByText("+ New Surgery");
    fireEvent.click(button);
    expect(screen.getByText("Schedule New Surgery")).toBeInTheDocument();
    expect(screen.getByText("Cancel")).toBeInTheDocument();
  });

  it("renders slot grid with schedules", () => {
    renderWithQuery(<SurgeryScheduler />);
    expect(screen.getByText(/Slots for/)).toBeInTheDocument();
  });
});

describe("OTDashboard", () => {
  it("renders the dashboard header", () => {
    renderWithQuery(<OTDashboard />);
    expect(screen.getByText("OT Dashboard")).toBeInTheDocument();
    expect(screen.getByText("Real-time room status and surgery board")).toBeInTheDocument();
  });

  it("renders stat cards", () => {
    renderWithQuery(<OTDashboard />);
    expect(screen.getByText("Planned")).toBeInTheDocument();
    expect(screen.getByText("In Progress")).toBeInTheDocument();
    expect(screen.getByText("Completed")).toBeInTheDocument();
    expect(screen.getByText("Cancelled")).toBeInTheDocument();
  });

  it("renders the timeline section", () => {
    renderWithQuery(<OTDashboard />);
    expect(screen.getByText("Today's Timeline")).toBeInTheDocument();
  });
});

describe("AnesthesiaRecord", () => {
  it("renders the anesthesia header", () => {
    renderWithQuery(<AnesthesiaRecord />);
    expect(screen.getByText("Anesthesia Record")).toBeInTheDocument();
    expect(screen.getByText("Pre-op assessment, intra-op vitals & drugs, post-op PACU notes")).toBeInTheDocument();
  });

  it("renders the pre-op form", () => {
    renderWithQuery(<AnesthesiaRecord />);
    expect(screen.getByText("Pre-Operative Assessment")).toBeInTheDocument();
  });
});

describe("SurgicalChecklist", () => {
  it("renders the checklist header", () => {
    renderWithQuery(<SurgicalChecklist />);
    expect(screen.getByText("Surgical Safety Checklist")).toBeInTheDocument();
    expect(screen.getByText("WHO Surgical Safety Checklist — Sign In, Time Out, Sign Out")).toBeInTheDocument();
  });

  it("renders create checklist form", () => {
    renderWithQuery(<SurgicalChecklist />);
    expect(screen.getByText("Create WHO Surgical Checklist")).toBeInTheDocument();
  });

  it("renders the three WHO phases", () => {
    renderWithQuery(<SurgicalChecklist />);
    expect(screen.getByText(/SIGN IN/)).toBeInTheDocument();
    expect(screen.getByText(/TIME OUT/)).toBeInTheDocument();
    expect(screen.getByText(/SIGN OUT/)).toBeInTheDocument();
  });
});
