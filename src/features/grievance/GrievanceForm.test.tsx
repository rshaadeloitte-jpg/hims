import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { GrievanceForm } from "./GrievanceForm";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const mockCategories = [
  { id: 1, code: "CLEANLINESS", description: "Cleanliness" },
  { id: 2, code: "BILLING", description: "Billing" },
];

const mockSeverities = [
  { id: 1, code: "LOW", description: "Low" },
  { id: 2, code: "MEDIUM", description: "Medium" },
  { id: 3, code: "HIGH", description: "High" },
  { id: 4, code: "CRITICAL", description: "Critical" },
];

const mockSubmit = vi.fn();
const mockCancel = vi.fn();

beforeEach(() => {
  vi.clearAllMocks();
});

describe("GrievanceForm", () => {
  it("renders all input fields", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    expect(screen.getByLabelText(/Name/)).toBeTruthy();
    expect(screen.getByLabelText(/Mobile Number/)).toBeTruthy();
    expect(screen.getByLabelText(/Category/)).toBeTruthy();
    expect(screen.getByLabelText(/Severity/)).toBeTruthy();
    expect(screen.getByLabelText(/MRN/)).toBeTruthy();
    expect(screen.getByLabelText(/Address/)).toBeTruthy();
    expect(screen.getByLabelText(/Issue Description/)).toBeTruthy();
  });

  it("renders category options from props", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    const categorySelect = screen.getByLabelText(/Category/);
    fireEvent.click(categorySelect);

    expect(screen.getByText("Cleanliness")).toBeTruthy();
    expect(screen.getByText("Billing")).toBeTruthy();
  });

  it("renders severity options from props", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    const severitySelect = screen.getByLabelText(/Severity/);
    fireEvent.click(severitySelect);

    expect(screen.getByText(/LOW/)).toBeTruthy();
    expect(screen.getByText(/MEDIUM/)).toBeTruthy();
    expect(screen.getByText(/HIGH/)).toBeTruthy();
    expect(screen.getByText(/CRITICAL/)).toBeTruthy();
  });

  it("shows required asterisk for mandatory fields", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    const reqLabels = screen.getAllByText("*");
    expect(reqLabels.length).toBeGreaterThanOrEqual(3);
  });

  it("does not submit when required fields are missing", async () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    fireEvent.click(screen.getByRole("button", { name: "Submit Grievance" }));

    await waitFor(() => {
      expect(mockSubmit).not.toHaveBeenCalled();
    });
  });

  it("calls onCancel when Cancel is clicked", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(mockCancel).toHaveBeenCalledOnce();
  });

  it("shows severity guide section", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    expect(screen.getByText("Severity Guide")).toBeTruthy();
    expect(screen.getByText(/minor inconvenience/)).toBeTruthy();
    expect(screen.getByText(/significant disruption/)).toBeTruthy();
    expect(screen.getByText(/system down/)).toBeTruthy();
  });

  it("submits valid form data", async () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={false}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    fireEvent.change(screen.getByLabelText(/Name/), { target: { value: "Jane Doe" } });
    fireEvent.change(screen.getByLabelText(/Mobile Number/), { target: { value: "9876543210" } });
    fireEvent.change(screen.getByLabelText(/Address/), { target: { value: "Test Address" } });
    fireEvent.change(screen.getByLabelText(/Issue Description/), {
      target: { value: "This is a test issue description" },
    });

    const categorySelect = screen.getByLabelText(/Category/);
    fireEvent.change(categorySelect, { target: { value: "1" } });

    const severitySelect = screen.getByLabelText(/Severity/);
    fireEvent.change(severitySelect, { target: { value: "2" } });

    fireEvent.click(screen.getByRole("button", { name: /submit grievance/i }));

    await waitFor(() => {
      expect(mockSubmit).toHaveBeenCalledOnce();
      const data = mockSubmit.mock.calls[0][0];
      expect(data.name).toBe("Jane Doe");
      expect(data.mobileNo).toBe("9876543210");
      expect(data.address).toBe("Test Address");
      expect(data.issue).toBe("This is a test issue description");
      expect(data.categoryId).toBe(1);
      expect(data.severityId).toBe(2);
    });
  });

  it("shows submit button disabled while submitting", () => {
    render(
      <GrievanceForm
        categories={mockCategories}
        severities={mockSeverities}
        submitting={true}
        onSubmit={mockSubmit}
        onCancel={mockCancel}
      />,
      { wrapper: wrapper() },
    );

    expect(screen.getByText("Submitting…")).toBeTruthy();
  });
});
