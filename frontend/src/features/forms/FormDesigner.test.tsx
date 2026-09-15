import { describe, expect, it, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FormDesigner } from "./FormDesigner";
import { SubmissionList } from "./SubmissionList";

vi.mock("./queries", () => ({
  useFormSpecialties: () => ({ data: ["Cardiology", "General Medicine"] }),
  usePatientSubmissions: () => ({ data: [], isLoading: false, isError: false, error: null }),
  useEncounterSubmissions: () => ({ data: [], isLoading: false, isError: false, error: null }),
  useDeleteSubmission: () => ({ mutate: vi.fn(), isPending: false }),
}));

function renderWithQueryClient(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe("FormDesigner", () => {
  const mockSubmit = vi.fn();
  const mockCancel = vi.fn();

  it("renders the form designer with metadata fields", () => {
    renderWithQueryClient(<FormDesigner onSubmit={mockSubmit} onCancel={mockCancel} />);
    expect(screen.getByLabelText(/code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/specialty/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/version/i)).toBeInTheDocument();
  });

  it("renders visual mode by default", () => {
    renderWithQueryClient(<FormDesigner onSubmit={mockSubmit} onCancel={mockCancel} />);
    expect(screen.getByRole("button", { name: /visual/i })).toHaveClass("form-designer__mode-btn--active");
  });

  it("switches to JSON mode", () => {
    renderWithQueryClient(<FormDesigner onSubmit={mockSubmit} onCancel={mockCancel} />);
    fireEvent.click(screen.getByRole("button", { name: /json/i }));
    expect(screen.getByRole("button", { name: /json/i })).toHaveClass("form-designer__mode-btn--active");
  });

  it("shows empty state in visual mode", () => {
    renderWithQueryClient(<FormDesigner onSubmit={mockSubmit} onCancel={mockCancel} />);
    expect(screen.getByText(/no fields yet/i)).toBeInTheDocument();
  });

  it("adds a field when clicking Add Field", () => {
    renderWithQueryClient(<FormDesigner onSubmit={mockSubmit} onCancel={mockCancel} />);
    fireEvent.click(screen.getByRole("button", { name: /add field/i }));
    expect(screen.queryByText(/no fields yet/i)).not.toBeInTheDocument();
  });
});

describe("SubmissionList", () => {
  it("renders lookup controls", () => {
    renderWithQueryClient(<SubmissionList />);
    expect(screen.getByRole("button", { name: /by patient/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /by encounter/i })).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/patient id/i)).toBeInTheDocument();
  });

  it("shows empty state initially", () => {
    renderWithQueryClient(<SubmissionList />);
    expect(screen.getByText(/enter an id to look up/i)).toBeInTheDocument();
  });
});
