import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MasterListPage } from "./MasterListPage";

/**
 * Component smoke test for MasterListPage.
 * The API module is mocked; TanStack Query runs for real on a test client so the
 * data flow (query → table) is exercised end to end within the component.
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

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/masters"]}>
        <MasterListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  api.listEntities.mockResolvedValue(["unitmaster", "departmentmaster", "companymaster"]);
});

describe("MasterListPage", () => {
  it("renders registry entities, schema-derived columns and rows", async () => {
    api.listScoped.mockResolvedValue([
      { id: 7, unitId: 1, code: "U001", name: "Jaipur Hospital", status: true, contactno: "0141-123" },
      { id: 8, unitId: 1, code: "U002", name: "Delhi Clinic", status: false, contactno: "011-456" },
    ]);

    renderPage();

    // Entity selector options come from GET /api/v1/masters.
    await screen.findByRole("option", { name: "Unit Master" });
    const entitySelect = screen.getByLabelText("Entity") as HTMLSelectElement;
    expect(entitySelect.options.length).toBeGreaterThanOrEqual(3);

    // Rows arrive from the mocked scoped-list query.
    expect(await screen.findByText("Jaipur Hospital")).toBeInTheDocument();
    expect(screen.getByText("Delhi Clinic")).toBeInTheDocument();

    // Columns are metadata-derived: unitmaster listable fields include
    // Code, Name, Description, Status, Contact No., Added.
    expect(screen.getByRole("columnheader", { name: "Code" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Status" })).toBeInTheDocument();
    expect(screen.getByRole("columnheader", { name: "Contact No." })).toBeInTheDocument();

    // Boolean cells render as badges.
    expect(screen.getByText("Active")).toBeInTheDocument();
    expect(screen.getByText("Inactive")).toBeInTheDocument();

    // Pagination controls honour the legacy offset semantics.
    expect(screen.getByText("Page 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Prev/ })).toBeDisabled();
  });

  it("shows the empty state when no records match", async () => {
    api.listScoped.mockResolvedValue([]);

    renderPage();

    expect(await screen.findByText(/No records found/)).toBeInTheDocument();
  });

  it("surfaces API errors from the query", async () => {
    api.listScoped.mockRejectedValue(
      Object.assign(new Error("Record not found"), { code: "NOT_FOUND", status: 404, name: "ApiError" }),
    );

    renderPage();

    expect(await screen.findByRole("alert")).toHaveTextContent(/Record not found/);
  });
});
