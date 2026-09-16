import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FormRenderer } from "./FormRenderer";

vi.mock("./queries", () => ({
  useSubmitForm: () => ({
    mutate: vi.fn((_data, opts) => opts?.onSuccess?.()),
    isPending: false,
    isError: false,
    error: null,
  }),
}));

function renderWithQueryClient(ui: React.ReactElement) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe("FormRenderer", () => {
  const mockSchema = {
    id: 1,
    code: "CONSULT_V1",
    name: "Consultation Note",
    specialty: "General Medicine",
    description: "Standard OPD consultation",
    version: 1,
    schema: {
      type: "object",
      properties: {
        chief_complaint: { type: "string", title: "Chief Complaint" },
        severity: { type: "string", title: "Severity", enum: ["Mild", "Moderate", "Severe"] },
        temperature: { type: "number", title: "Temperature (°F)" },
        followup_needed: { type: "boolean", title: "Follow-up Needed" },
      },
      required: ["chief_complaint", "severity"],
    },
    legacyTiuxmlRef: null,
  };

  it("renders the form header with name, specialty, and version", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getByText("Consultation Note")).toBeInTheDocument();
    expect(screen.getByText("General Medicine")).toBeInTheDocument();
    expect(screen.getByText("v1")).toBeInTheDocument();
  });

  it("renders string input fields", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getByLabelText(/chief complaint/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/chief complaint/i)).toHaveAttribute("type", "text");
  });

  it("renders enum fields as select dropdown", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    const select = screen.getByLabelText(/severity/i);
    expect(select.tagName).toBe("SELECT");
    expect(screen.getByText("Mild")).toBeInTheDocument();
    expect(screen.getByText("Moderate")).toBeInTheDocument();
    expect(screen.getByText("Severe")).toBeInTheDocument();
  });

  it("renders number input fields", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getByLabelText(/temperature/i)).toHaveAttribute("type", "number");
  });

  it("renders boolean fields as checkbox", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getByLabelText(/follow-up needed/i)).toHaveAttribute("type", "checkbox");
  });

  it("marks required fields with asterisk", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getAllByText(/\*/).length).toBeGreaterThan(0);
  });

  it("displays the description text", () => {
    renderWithQueryClient(<FormRenderer schemaResponse={mockSchema} />);
    expect(screen.getByText("Standard OPD consultation")).toBeInTheDocument();
  });
});
