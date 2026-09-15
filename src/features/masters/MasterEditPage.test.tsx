import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MasterEditPage } from "./MasterEditPage";

/**
 * Red-team F-6: unit-scoped entities (departmentmaster/companymaster) require
 * the owning unitId on GET /{entity}/{id} and PUT /{entity}/{id} — otherwise
 * the backend scoped lookup 404s / BEXA031s. This test proves the edit page
 * passes the unitId carried from the list row (?unitId=...) on both calls.
 */

vi.mock("../../api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/client")>();
  return {
    ...actual, // keep ApiError (used by the page's error UI)
    mastersApi: {
      listEntities: vi.fn(),
      listScoped: vi.fn(),
      listAll: vi.fn(),
      getById: vi.fn(),
      create: vi.fn(),
      update: vi.fn(),
    },
  };
});

const api = (await import("../../api/client")).mastersApi as unknown as Record<
  string,
  ReturnType<typeof vi.fn>
>;

function renderEditPage(entry: string) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[entry]}>
        <Routes>
          <Route path="/masters/:entity/:id" element={<MasterEditPage />} />
          {/* post-save navigation target (list) — keeps router warnings out */}
          <Route path="/masters/:entity" element={<div>list fallback</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  api.getById.mockResolvedValue({
    id: 5,
    unitId: 7,
    code: "D01",
    description: "Cardiology",
    status: true,
    isclinical: false,
  });
  api.update.mockResolvedValue({ id: 5, unitId: 7, code: "D01", description: "Cardiology edited" });
});

describe("MasterEditPage — unit-scoped entity edit (F-6)", () => {
  it("sends the row's unitId on GET-by-id and PUT", async () => {
    const user = userEvent.setup();
    renderEditPage("/masters/departmentmaster/5?unitId=7");

    // The detail query must carry unitId=7 (scoped lookup would 404 without it).
    await waitFor(() => expect(api.getById).toHaveBeenCalledWith("departmentmaster", "5", 7));

    // Record rendered into the schema-generated form.
    expect(await screen.findByDisplayValue("Cardiology")).toBeInTheDocument();
    expect(screen.getByDisplayValue("D01")).toBeInTheDocument();

    // Edit the description, then save. (Required labels carry a " *" marker,
    // so match the label text loosely.)
    const description = screen.getByLabelText(/^Description/);
    await user.type(description, " (updated)");
    await user.click(screen.getByRole("button", { name: /Save/ }));

    // PUT must carry unitId=7 as well (scoped update, else BEXA031).
    await waitFor(() => expect(api.update).toHaveBeenCalledTimes(1));
    const [entity, id, payload, unitId] = api.update.mock.calls[0];
    expect(entity).toBe("departmentmaster");
    expect(id).toBe("5");
    expect(unitId).toBe(7);
    expect(payload).toMatchObject({ description: "Cardiology (updated)" });
  });

  it("does not PUT untouched defaults (F-18: undefined = untouched)", async () => {
    const user = userEvent.setup();
    renderEditPage("/masters/departmentmaster/5?unitId=7");

    await screen.findByDisplayValue("Cardiology");

    // Only toggle the status checkbox (record had status true -> now false).
    await user.click(screen.getByRole("checkbox", { name: "Status" }));
    await user.click(screen.getByRole("button", { name: /Save/ }));

    await waitFor(() => expect(api.update).toHaveBeenCalledTimes(1));
    const payload = api.update.mock.calls[0][2] as Record<string, unknown>;
    expect(payload.status).toBe(false);
    // Untouched optional fields must NOT be sent as "" / false defaults.
    expect(payload).not.toHaveProperty("email");
    expect(payload).not.toHaveProperty("isclinical");
  });

  it("falls back to unscoped GET when no unitId is carried", async () => {
    renderEditPage("/masters/unitmaster/3");
    await waitFor(() => expect(api.getById).toHaveBeenCalledWith("unitmaster", "3", undefined));
  });
});
