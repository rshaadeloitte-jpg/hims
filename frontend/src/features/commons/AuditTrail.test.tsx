import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AuditTrailPage } from "./AuditTrail";

/**
 * AuditTrailPage smoke test — exercises the entity-scoped audit feed
 * with JSONB diff rendering (Linear audit log patterns).
 */

const mockFetch = vi.fn();
global.fetch = mockFetch;

function renderAuditTrail() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AuditTrailPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockFetch.mockReset();
});

afterEach(() => {
  mockFetch.mockReset();
});

describe("AuditTrailPage", () => {
  it("renders the audit feed with entity-scoped entries and diff", async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          entityType: "Patient",
          entityId: 42,
          action: "UPDATE",
          actorId: 7,
          actorUsername: "alice",
          diff: JSON.stringify({ name: { old: "Bob", new: "Alice" } }),
          occurredAt: "2024-09-11T14:30:00Z",
        },
        {
          id: 2,
          entityType: "Patient",
          entityId: 42,
          action: "CREATE",
          actorId: 7,
          actorUsername: "alice",
          diff: null,
          occurredAt: "2024-09-11T14:25:00Z",
        },
      ],
    });

    const user = userEvent.setup();
    renderAuditTrail();

    expect(screen.getByText("Audit Trail")).toBeInTheDocument();

    // Scope by entity
    await user.type(screen.getByLabelText("Entity Type"), "Patient");
    await user.type(screen.getByLabelText("Entity ID"), "42");

    // Wait for query
    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/commons/audit/by-entity"),
        expect.anything(),
      );
    });

    // Entries rendered
    await screen.findAllByText("Patient #42");
    expect(screen.getByText("UPDATE")).toBeInTheDocument();
    expect(screen.getByText("CREATE")).toBeInTheDocument();
    expect(screen.getAllByText("by alice").length).toBeGreaterThanOrEqual(1);
  });

  it("expands a diff row to show old/new values", async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [
        {
          id: 1,
          entityType: "Patient",
          entityId: 42,
          action: "UPDATE",
          actorId: 7,
          actorUsername: "alice",
          diff: JSON.stringify({
            name: { old: "Bob", new: "Alice" },
            age: { old: "30", new: "31" },
          }),
          occurredAt: "2024-09-11T14:30:00Z",
        },
      ],
    });

    renderAuditTrail();

    await user.type(screen.getByLabelText("Entity Type"), "Patient");
    await user.type(screen.getByLabelText("Entity ID"), "42");

    await screen.findByText("Patient #42");

    // Click "Show 2 changes"
    const showBtn = await screen.findByRole("button", { name: /Show 2 changes/ });
    await user.click(showBtn);

    // Diff table shows old/new values
    await screen.findByText("Bob");
    expect(screen.getByText("Alice")).toBeInTheDocument();
    expect(screen.getByText("30")).toBeInTheDocument();
    expect(screen.getByText("31")).toBeInTheDocument();
  });

  it("switches to actor scope and filters by actorId", async () => {
    const user = userEvent.setup();
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    renderAuditTrail();

    // Switch scope to "By Actor"
    const scopeSelect = screen.getByRole("combobox", { name: "Scope" });
    await user.selectOptions(scopeSelect, "actor");
    await user.type(screen.getByLabelText("Actor ID"), "7");

    await waitFor(() => {
      expect(mockFetch).toHaveBeenCalledWith(
        expect.stringContaining("/commons/audit/by-actor/7"),
        expect.anything(),
      );
    });

    // Prompt shows
    expect(screen.getByText(/Enter an actor ID/)).toBeInTheDocument();
  });
});
