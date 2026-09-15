import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ComplaintTracker } from "./ComplaintTracker";

/**
 * Component test for ComplaintTracker — verifies complaint raising,
 * kanban view, resolution recording, and detail expansion.
 */

vi.mock("./queries", async (importOriginal) => {
  const actual = await importOriginal<typeof import("./queries")>();
  return {
    ...actual,
    useComplaints: vi.fn(),
    useRaiseComplaint: vi.fn(),
    useComplaint: vi.fn(),
    useAddResolution: vi.fn(),
  };
});

const queries = await import("./queries");
const useComplaints = queries.useComplaints as unknown as ReturnType<typeof vi.fn>;
const useRaiseComplaint = queries.useRaiseComplaint as unknown as ReturnType<typeof vi.fn>;
const useComplaint = queries.useComplaint as unknown as ReturnType<typeof vi.fn>;
const useAddResolution = queries.useAddResolution as unknown as ReturnType<typeof vi.fn>;

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ComplaintTracker />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  useComplaints.mockReturnValue({ data: [], isLoading: false, error: null });
  useRaiseComplaint.mockReturnValue({ mutate: vi.fn(), isPending: false });
  useComplaint.mockReturnValue({ data: null });
  useAddResolution.mockReturnValue({ mutate: vi.fn(), isPending: false });
});

describe("ComplaintTracker", () => {
  it("renders empty state when no complaints", () => {
    renderPage();
    expect(screen.getByText(/No complaints/)).toBeInTheDocument();
  });

  it("renders complaint list with status badges", () => {
    useComplaints.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          complaintNo: "IHMS/EQ/2026/00001",
          description: "Machine making noise",
          complaintStatus: 1,
          reportedDate: "2026-01-15",
        },
        {
          id: 2,
          equipmentId: 11,
          complaintNo: "IHMS/EQ/2026/00002",
          description: "Display flickering",
          complaintStatus: 2,
          reportedDate: "2026-01-16",
        },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    expect(screen.getByText("IHMS/EQ/2026/00001")).toBeInTheDocument();
    expect(screen.getByText("Machine making noise")).toBeInTheDocument();
    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("In Progress")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    useComplaints.mockReturnValue({ data: [], isLoading: true, error: null });
    renderPage();
    expect(screen.getByText(/Loading complaints/)).toBeInTheDocument();
  });

  it("opens raise complaint form", async () => {
    const user = userEvent.setup();
    renderPage();

    await user.click(screen.getByRole("button", { name: /Raise Complaint/ }));
    expect(screen.getByText(/Raise New Complaint/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Equipment ID/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Description/)).toBeInTheDocument();
  });

  it("validates required fields on raise", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn();
    useRaiseComplaint.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Raise Complaint/ }));

    await user.click(screen.getByRole("button", { name: /Submit Complaint/ }));

    expect(screen.getByText(/Equipment is required/)).toBeInTheDocument();
    expect(screen.getByText(/Description required/)).toBeInTheDocument();
    expect(mockMutate).not.toHaveBeenCalled();
  });

  it("submits valid complaint", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useRaiseComplaint.mockReturnValue({ mutate: mockMutate, isPending: false });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Raise Complaint/ }));

    await user.type(screen.getByLabelText(/Equipment ID/), "10");
    await user.type(screen.getByLabelText(/Description/), "Broken display");

    await user.click(screen.getByRole("button", { name: /Submit Complaint/ }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledTimes(1);
    });
    const [payload] = mockMutate.mock.calls[0];
    expect(payload).toMatchObject({
      equipmentId: 10,
      description: "Broken display",
    });
  });

  it("toggles kanban view", async () => {
    const user = userEvent.setup();
    useComplaints.mockReturnValue({
      data: [
        { id: 1, equipmentId: 10, description: "Issue A", complaintStatus: 1 },
        { id: 2, equipmentId: 11, description: "Issue B", complaintStatus: 2 },
        { id: 3, equipmentId: 12, description: "Issue C", complaintStatus: 3 },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Kanban View/ }));

    // Kanban columns should be visible
    expect(screen.getByText("Open")).toBeInTheDocument();
    expect(screen.getByText("In Progress")).toBeInTheDocument();
    expect(screen.getByText("Resolved")).toBeInTheDocument();
    expect(screen.getByText("Closed")).toBeInTheDocument();

    // Cards should be in correct columns
    expect(screen.getByText("Issue A")).toBeInTheDocument();
    expect(screen.getByText("Issue B")).toBeInTheDocument();
    expect(screen.getByText("Issue C")).toBeInTheDocument();
  });

  it("opens resolution form for a complaint", async () => {
    const user = userEvent.setup();
    useComplaints.mockReturnValue({
      data: [
        { id: 1, equipmentId: 10, description: "Broken", complaintStatus: 1 },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Resolve/ }));

    expect(screen.getByText(/Resolve Complaint #1/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Resolution Notes/)).toBeInTheDocument();
  });

  it("submits resolution", async () => {
    const user = userEvent.setup();
    const mockMutate = vi.fn((_payload, opts) => opts?.onSuccess?.());
    useAddResolution.mockReturnValue({ mutate: mockMutate, isPending: false });

    useComplaints.mockReturnValue({
      data: [
        { id: 1, equipmentId: 10, description: "Broken", complaintStatus: 1 },
      ],
      isLoading: false,
      error: null,
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Resolve/ }));

    await user.type(screen.getByLabelText(/Resolution Notes/), "Replaced part");
    await user.type(screen.getByLabelText(/Parts Replaced/), "Bearing SKF-6205");

    await user.click(screen.getByRole("button", { name: /Record Resolution/ }));

    await waitFor(() => {
      expect(mockMutate).toHaveBeenCalledTimes(1);
    });
    const [{ complaintId, payload }] = mockMutate.mock.calls[0];
    expect(complaintId).toBe(1);
    expect(payload).toMatchObject({
      resolutionNotes: "Replaced part",
      partsReplaced: "Bearing SKF-6205",
    });
  });

  it("expands complaint detail", async () => {
    const user = userEvent.setup();
    useComplaints.mockReturnValue({
      data: [
        {
          id: 1,
          equipmentId: 10,
          complaintNo: "IHMS/EQ/2026/00001",
          description: "Broken",
          complaintStatus: 1,
          reportedDate: "2026-01-15",
        },
      ],
      isLoading: false,
      error: null,
    });
    useComplaint.mockReturnValue({
      data: {
        complaint: {
          id: 1,
          equipmentId: 10,
          description: "Broken",
          accessionNo: "ACC-001",
        },
        details: [],
        resolutions: [
          {
            id: 1,
            complaintId: 1,
            resolutionNotes: "Fixed bearing",
            resolutionDate: "2026-01-16",
            resolvedStatus: 3,
          },
        ],
      },
    });

    renderPage();
    await user.click(screen.getByRole("button", { name: /Detail/ }));

    expect(screen.getByText(/Complaint #1 Details/)).toBeInTheDocument();
    expect(screen.getByText(/Fixed bearing/)).toBeInTheDocument();
  });
});
