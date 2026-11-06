import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { HomePage } from "./HomePage";

const fetchMock = vi.fn();

function renderHomePage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/"]}>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

function setupSuccessfulMetrics() {
  fetchMock.mockImplementation(async (url: RequestInfo | URL) => {
    const urlStr = String(url);
    if (urlStr.includes("/queue/tokens")) {
      return {
        ok: true,
        status: 200,
        json: async () => [
          {
            tokenId: 1,
            counterId: 1,
            patientId: 10,
            tokenNo: 101,
            priority: "NORMAL",
            status: 1,
            createdAt: "2026-11-06T10:00:00Z",
          },
          {
            tokenId: 2,
            counterId: 1,
            patientId: 11,
            tokenNo: 102,
            priority: "NORMAL",
            status: 1,
            createdAt: "2026-11-06T10:05:00Z",
          },
        ],
      };
    }
    if (urlStr.includes("/queue/counters")) {
      return {
        ok: true,
        status: 200,
        json: async () => [
          { counterId: 1, counterName: "Counter 1", isActive: true },
          { counterId: 2, counterName: "Counter 2", isActive: false },
        ],
      };
    }
    if (urlStr.includes("/beds/available")) {
      return {
        ok: true,
        status: 200,
        json: async () => [
          { bedId: 1, bedNo: "B-101", wardId: 1, status: 1, isActive: true },
          { bedId: 2, bedNo: "B-102", wardId: 1, status: 1, isActive: true },
          { bedId: 3, bedNo: "B-103", wardId: 1, status: 1, isActive: true },
        ],
      };
    }
    if (urlStr.includes("/lab/orders")) {
      return {
        ok: true,
        status: 200,
        json: async () => [
          {
            orderId: 1,
            patientId: 10,
            isOutSourced: false,
            status: "ORDERED",
            orderDate: "2026-11-06",
          },
        ],
      };
    }
    return {
      ok: true,
      status: 200,
      json: async () => [],
    };
  });
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal("fetch", fetchMock);
  localStorage.clear();
  sessionStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("HomePage", () => {
  it("renders a meaningful heading and does not display generic 'Home Page'", () => {
    setupSuccessfulMetrics();
    renderHomePage();

    const main = screen.getByRole("main");
    expect(
      within(main).getByRole("heading", { name: "Staff Operations Dashboard" }),
    ).toBeInTheDocument();
    expect(within(main).queryByText(/^Home Page$/i)).not.toBeInTheDocument();
  });

  it("renders quick action tiles as real links with exact router destinations", () => {
    setupSuccessfulMetrics();
    renderHomePage();

    const main = screen.getByRole("main");

    const patientReg = within(main).getByRole("link", {
      name: /Patient Registration/i,
    });
    expect(patientReg).toHaveAttribute("href", "/patients/register");
    expect(patientReg.tagName.toLowerCase()).toBe("a");
    expect(patientReg).not.toHaveAttribute("role", "button");

    const newAppt = within(main).getByRole("link", {
      name: /New Appointment/i,
    });
    expect(newAppt).toHaveAttribute("href", "/m/op/addAppointment");
    expect(newAppt.tagName.toLowerCase()).toBe("a");
    expect(newAppt).not.toHaveAttribute("role", "button");

    const lab = within(main).getByRole("link", {
      name: /Laboratory/i,
    });
    expect(lab).toHaveAttribute("href", "/lab");
    expect(lab.tagName.toLowerCase()).toBe("a");
    expect(lab).not.toHaveAttribute("role", "button");

    const queue = within(main).getByRole("link", {
      name: /Queue Dashboard/i,
    });
    expect(queue).toHaveAttribute("href", "/queue");
    expect(queue.tagName.toLowerCase()).toBe("a");
    expect(queue).not.toHaveAttribute("role", "button");
  });

  it("removes legacy Quick Access and Status placeholder cards", () => {
    setupSuccessfulMetrics();
    renderHomePage();

    const main = screen.getByRole("main");
    expect(within(main).queryByText("Quick Access")).not.toBeInTheDocument();
    expect(within(main).queryByText(/You are signed in\./i)).not.toBeInTheDocument();
  });

  it("displays real metric counter values when queries succeed", async () => {
    setupSuccessfulMetrics();
    renderHomePage();

    const main = screen.getByRole("main");

    // 2 patients waiting
    const queueCard = within(main).getByTestId("metric-patients-waiting");
    expect(await within(queueCard).findByText("2")).toBeInTheDocument();

    // 1 active counter
    const counterCard = within(main).getByTestId("metric-active-counters");
    expect(within(counterCard).getByText("1")).toBeInTheDocument();

    // 3 available beds
    const bedsCard = within(main).getByTestId("metric-available-beds");
    expect(within(bedsCard).getByText("3")).toBeInTheDocument();

    // 1 lab order
    const labCard = within(main).getByTestId("metric-lab-orders");
    expect(within(labCard).getByText("1")).toBeInTheDocument();
  });

  it("counts only tokens with status WAITING (1) when mocked tokens have statuses 1, 1, 2, 3, 4", async () => {
    fetchMock.mockImplementation(async (url: RequestInfo | URL) => {
      const urlStr = String(url);
      if (urlStr.includes("/queue/tokens")) {
        return {
          ok: true,
          status: 200,
          json: async () => [
            {
              tokenId: 1,
              counterId: 1,
              patientId: 10,
              tokenNo: 101,
              priority: "NORMAL",
              status: 1,
              createdAt: "2026-11-06T10:00:00Z",
            },
            {
              tokenId: 2,
              counterId: 1,
              patientId: 11,
              tokenNo: 102,
              priority: "NORMAL",
              status: 1,
              createdAt: "2026-11-06T10:05:00Z",
            },
            {
              tokenId: 3,
              counterId: 1,
              patientId: 12,
              tokenNo: 103,
              priority: "NORMAL",
              status: 2,
              createdAt: "2026-11-06T10:10:00Z",
            },
            {
              tokenId: 4,
              counterId: 1,
              patientId: 13,
              tokenNo: 104,
              priority: "NORMAL",
              status: 3,
              createdAt: "2026-11-06T10:15:00Z",
            },
            {
              tokenId: 5,
              counterId: 1,
              patientId: 14,
              tokenNo: 105,
              priority: "NORMAL",
              status: 4,
              createdAt: "2026-11-06T10:20:00Z",
            },
          ],
        };
      }
      return {
        ok: true,
        status: 200,
        json: async () => [],
      };
    });

    renderHomePage();

    const main = screen.getByRole("main");
    const queueCard = within(main).getByTestId("metric-patients-waiting");
    expect(await within(queueCard).findByText("2")).toBeInTheDocument();
    expect(
      within(queueCard).getByRole("heading", { name: "Patients Waiting" }),
    ).toBeInTheDocument();
    expect(
      within(queueCard).getByText("Tokens waiting to be called today"),
    ).toBeInTheDocument();
  });

  it("shows an em dash and 'Unavailable' without crashing when a metric query fails", async () => {
    fetchMock.mockImplementation(async (url: RequestInfo | URL) => {
      const urlStr = String(url);
      if (urlStr.includes("/queue/tokens")) {
        return {
          ok: false,
          status: 503,
          statusText: "Service Unavailable",
          json: async () => ({ message: "Queue microservice unreachable" }),
        };
      }
      if (urlStr.includes("/queue/counters")) {
        return {
          ok: true,
          status: 200,
          json: async () => [{ counterId: 1, counterName: "C1", isActive: true }],
        };
      }
      return {
        ok: true,
        status: 200,
        json: async () => [],
      };
    });

    renderHomePage();

    const main = screen.getByRole("main");

    // Page must still render heading and quick actions
    expect(
      within(main).getByRole("heading", { name: "Staff Operations Dashboard" }),
    ).toBeInTheDocument();
    expect(
      within(main).getByRole("link", { name: /Patient Registration/i }),
    ).toBeInTheDocument();

    // Failed metric displays 'Unavailable' and em dash '—'
    const queueCard = within(main).getByTestId("metric-patients-waiting");
    expect(await within(queueCard).findByText("Unavailable")).toBeInTheDocument();
    expect(within(queueCard).getByText("—")).toBeInTheDocument();
  });

  it("shows signed-in user information only if auth hook provides it", () => {
    // 1. Without auth token -> no signed-in user banner on HomePage
    setupSuccessfulMetrics();
    const { unmount } = renderHomePage();
    expect(screen.queryByTestId("signed-in-user-banner")).not.toBeInTheDocument();
    unmount();

    // 2. With valid JWT auth token in localStorage -> signed-in user info displayed
    const jwtPayload = btoa(
      JSON.stringify({
        name: "Dr. Sarah Connor",
        role: "Chief Medical Officer",
        email: "sconnor@hospital.org",
        exp: Math.floor(Date.now() / 1000) + 3600,
      }),
    );
    localStorage.setItem("access_token", `fakeheader.${jwtPayload}.fakesig`);

    renderHomePage();
    const banner = screen.getByTestId("signed-in-user-banner");
    expect(banner).toBeInTheDocument();
    expect(within(banner).getByText("Dr. Sarah Connor")).toBeInTheDocument();
    expect(within(banner).getByText(/Chief Medical Officer/i)).toBeInTheDocument();
    expect(within(banner).getByText(/sconnor@hospital.org/i)).toBeInTheDocument();
  });
});
