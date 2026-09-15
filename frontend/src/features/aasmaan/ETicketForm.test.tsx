import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ETicketForm } from "./ETicketForm";

const mockTickets = [
  { ticketId: 1, caseId: 1, subject: "Patient not reachable", priority: "HIGH", status: "OPEN", createdAt: "2024-01-15T10:00:00" },
  { ticketId: 2, caseId: 2, subject: "Device not syncing", priority: "MEDIUM", status: "IN_PROGRESS", createdAt: "2024-01-14T08:00:00" },
  { ticketId: 3, caseId: 3, subject: "Checklist incomplete", priority: "LOW", status: "RESOLVED", createdAt: "2024-01-13T12:00:00" },
];

let mockTicketsData = mockTickets;
let mockTicketsLoading = false;

const mockMutateAsync = vi.fn();

vi.mock("./queries", () => ({
  useETickets: () => ({ data: mockTicketsData, isLoading: mockTicketsLoading }),
  useCreateETicket: () => ({ mutateAsync: mockMutateAsync, isPending: false, isError: false }),
}));

function renderETicketForm() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={qc}>
      <ETicketForm />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockTicketsData = mockTickets;
  mockTicketsLoading = false;
  mockMutateAsync.mockResolvedValue({ ticketId: 10 });
});

describe("ETicketForm", () => {
  it("renders the e-ticket header", () => {
    renderETicketForm();
    expect(screen.getByText("E-Tickets")).toBeInTheDocument();
  });

  it("renders create ticket form", () => {
    renderETicketForm();
    expect(screen.getByText("Create E-Ticket")).toBeInTheDocument();
    expect(screen.getByLabelText("Subject *")).toBeInTheDocument();
    expect(screen.getByLabelText("Priority")).toBeInTheDocument();
  });

  it("renders ticket list", () => {
    renderETicketForm();
    expect(screen.getByText("Patient not reachable")).toBeInTheDocument();
    expect(screen.getByText("Device not syncing")).toBeInTheDocument();
  });

  it("shows priority labels in tickets", () => {
    renderETicketForm();
    expect(screen.getAllByText("HIGH")).toHaveLength(1);
    expect(screen.getAllByText("MEDIUM")).toHaveLength(1);
    expect(screen.getAllByText("LOW")).toHaveLength(1);
  });

  it("shows status labels in tickets", () => {
    renderETicketForm();
    expect(screen.getByText("OPEN")).toBeInTheDocument();
    expect(screen.getByText("IN_PROGRESS")).toBeInTheDocument();
    expect(screen.getByText("RESOLVED")).toBeInTheDocument();
  });

  it("shows ticket count in header", () => {
    renderETicketForm();
    expect(screen.getByText("Recent Tickets (3)")).toBeInTheDocument();
  });

  it("renders new ticket button", () => {
    renderETicketForm();
    expect(screen.getByRole("button", { name: /new ticket/i })).toBeInTheDocument();
  });

  it("shows empty state when no tickets", () => {
    mockTicketsData = [];
    renderETicketForm();
    expect(screen.getByText("No tickets yet")).toBeInTheDocument();
  });

  it("shows loading state", () => {
    mockTicketsLoading = true;
    renderETicketForm();
    expect(screen.getByText("Loading tickets...")).toBeInTheDocument();
  });

  it("renders case ID field", () => {
    renderETicketForm();
    expect(screen.getByLabelText("Case ID")).toBeInTheDocument();
  });

  it("renders status field in form", () => {
    renderETicketForm();
    expect(screen.getByLabelText("Status")).toBeInTheDocument();
  });

  it("shows create ticket button", () => {
    renderETicketForm();
    expect(screen.getByRole("button", { name: /create ticket/i })).toBeInTheDocument();
  });
});