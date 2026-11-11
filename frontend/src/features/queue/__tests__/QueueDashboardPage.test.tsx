import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueueDashboardPage } from "../QueueDashboardPage";

function wrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={qc}>{children}</QueryClientProvider>
  );
}

const fetchMock = vi.fn();

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("QueueDashboardPage", () => {
  it("renders counters table with code + name and real waiting token count", async () => {
    fetchMock.mockImplementation(async (url: string) => {
      if (url.includes("/api/v1/queue/counters")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              id: 1,
              code: "C1",
              name: "OPD Counter 1",
              departmentId: 10,
              status: true,
              addedBy: 1,
              addedDatetime: "2026-11-11T10:00:00Z",
              synchronizedd: false,
            },
          ],
        };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              id: 101,
              tokenNo: 1,
              patientId: 77,
              counterId: 1,
              departmentId: 10,
              priority: 1,
              queueStatus: 1,
              status: true,
            },
            {
              id: 102,
              tokenNo: 2,
              patientId: 78,
              counterId: 1,
              departmentId: 10,
              priority: 2,
              queueStatus: 2,
              status: true,
            },
          ],
        };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    render(<QueueDashboardPage />, { wrapper: wrapper() });

    const counterNames = await screen.findAllByText("C1 - OPD Counter 1");
    expect(counterNames.length).toBeGreaterThan(0);
    expect(await screen.findByText("1 waiting")).toBeTruthy();
  });

  it("+ Counter posts code, name and departmentId to create-counter endpoint", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (options?.method === "POST" && url.includes("/api/v1/queue/counters")) {
        return {
          ok: true,
          status: 201,
          json: async () => ({
            id: 2,
            code: "C2",
            name: "Pediatrics",
            departmentId: 5,
            status: true,
          }),
        };
      }
      if (url.includes("/api/v1/queue/counters")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    const user = userEvent.setup();
    render(<QueueDashboardPage />, { wrapper: wrapper() });

    const codeInput = screen.getByPlaceholderText("Counter code");
    const nameInput = screen.getByPlaceholderText("Counter name");
    const deptInput = screen.getByPlaceholderText("Department ID");
    const addBtn = screen.getByRole("button", { name: /\+ Counter/i });

    await user.type(codeInput, "C2");
    await user.type(nameInput, "Pediatrics");
    await user.type(deptInput, "5");
    await user.click(addBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/queue/counters"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ code: "C2", name: "Pediatrics", departmentId: 5 }),
        })
      );
    });
  });

  it("+ Counter shows backend error message on failure", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (options?.method === "POST" && url.includes("/api/v1/queue/counters")) {
        return {
          ok: false,
          status: 400,
          json: async () => ({
            status: "ERROR",
            errorCode: "BEXA003",
            message: "code and name are required",
          }),
        };
      }
      if (url.includes("/api/v1/queue/counters")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    const user = userEvent.setup();
    render(<QueueDashboardPage />, { wrapper: wrapper() });

    const addBtn = screen.getByRole("button", { name: /\+ Counter/i });
    await user.click(addBtn);

    expect(await screen.findByText("code and name are required")).toBeTruthy();
    expect(screen.getByRole("alert")).toBeTruthy();
  });

  it("Issue token provides counter select from GET counters and maps priority to number", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (options?.method === "POST" && url.includes("/api/v1/queue/tokens")) {
        return {
          ok: true,
          status: 201,
          json: async () => ({
            id: 201,
            tokenNo: 5,
            patientId: 501,
            counterId: 1,
            departmentId: 10,
            priority: 2,
            queueStatus: 1,
          }),
        };
      }
      if (url.includes("/api/v1/queue/counters")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              id: 1,
              code: "C1",
              name: "OPD Counter 1",
              departmentId: 10,
              status: true,
            },
          ],
        };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    const user = userEvent.setup();
    render(<QueueDashboardPage />, { wrapper: wrapper() });

    // Counter select options should show the counter option
    const counterMatches = await screen.findAllByText("C1 - OPD Counter 1");
    expect(counterMatches.length).toBeGreaterThan(0);

    const counterSelect = screen.getByRole("combobox", { name: /counter/i });
    await user.selectOptions(counterSelect, "1");

    const patientInput = screen.getByPlaceholderText("Patient ID");
    await user.type(patientInput, "501");

    const prioritySelect = screen.getByRole("combobox", { name: /priority/i });
    await user.selectOptions(prioritySelect, "EMERGENCY");

    const issueBtn = screen.getByRole("button", { name: /^Issue$/i });
    await user.click(issueBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/queue/tokens"),
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({
            counterId: 1,
            patientId: 501,
            departmentId: 10,
            priority: 2,
          }),
        })
      );
    });
  });

  it("Issue token shows backend error message on failure", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (options?.method === "POST" && url.includes("/api/v1/queue/tokens")) {
        return {
          ok: false,
          status: 400,
          json: async () => ({
            status: "ERROR",
            errorCode: "BEXA003",
            message: "Invalid input data !",
          }),
        };
      }
      if (url.includes("/api/v1/queue/counters")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            { id: 1, code: "C1", name: "OPD Counter 1", departmentId: 10, status: true },
          ],
        };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return { ok: true, status: 200, json: async () => [] };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    const user = userEvent.setup();
    render(<QueueDashboardPage />, { wrapper: wrapper() });

    await screen.findAllByText("C1 - OPD Counter 1");

    const counterSelect = screen.getByRole("combobox", { name: /counter/i });
    await user.selectOptions(counterSelect, "1");
    await user.type(screen.getByPlaceholderText("Patient ID"), "501");

    const issueBtn = screen.getByRole("button", { name: /^Issue$/i });
    await user.click(issueBtn);

    expect(await screen.findByText("Invalid input data !")).toBeTruthy();
    expect(screen.getByRole("alert")).toBeTruthy();
  });

  it("Manage counter displays tokens and allows calling next", async () => {
    fetchMock.mockImplementation(async (url: string, options?: RequestInit) => {
      if (options?.method === "POST" && url.includes("/api/v1/queue/counters/1/call-next")) {
        return {
          ok: true,
          status: 200,
          json: async () => ({
            id: 101,
            tokenNo: 1,
            patientId: 77,
            counterId: 1,
            departmentId: 10,
            priority: 1,
            queueStatus: 2,
          }),
        };
      }
      if (url.includes("/api/v1/queue/counters")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            { id: 1, code: "C1", name: "OPD Counter 1", departmentId: 10, status: true },
          ],
        };
      }
      if (url.includes("/api/v1/queue/tokens")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              id: 101,
              tokenNo: 1,
              patientId: 77,
              counterId: 1,
              departmentId: 10,
              priority: 1,
              queueStatus: 1,
              status: true,
            },
          ],
        };
      }
      return { ok: true, status: 200, json: async () => ({}) };
    });

    const user = userEvent.setup();
    render(<QueueDashboardPage />, { wrapper: wrapper() });

    const manageBtn = await screen.findByRole("button", { name: /Manage/i });
    await user.click(manageBtn);

    expect(await screen.findByText("Counter #1 — Tokens")).toBeTruthy();

    const callNextBtn = screen.getByRole("button", { name: /Call Next/i });
    await user.click(callNextBtn);

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        expect.stringContaining("/api/v1/queue/counters/1/call-next"),
        expect.objectContaining({ method: "POST" })
      );
    });
  });
});
