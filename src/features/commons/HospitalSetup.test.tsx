import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { HospitalSetupPage } from "./HospitalSetup";

/**
 * HospitalSetupPage smoke test — exercises geo-tagging + unit creation.
 * Mocks the mastersApi.create for unitmaster and browser geolocation.
 */

vi.mock("../../api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/client")>();
  return {
    ...actual,
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

// Mock navigator.geolocation
const mockGetCurrentPosition = vi.fn();
Object.defineProperty(globalThis, "navigator", {
  value: {
    geolocation: {
      getCurrentPosition: mockGetCurrentPosition,
    },
  },
  writable: true,
  configurable: true,
});

function renderHospitalSetup() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <HospitalSetupPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  api.create.mockResolvedValue({ id: 42, code: "HOSP-001", name: "Jaipur Hospital" });
});

afterEach(() => {
  vi.clearAllMocks();
});

describe("HospitalSetupPage", () => {
  it("renders the hospital setup form", () => {
    renderHospitalSetup();

    expect(screen.getByText("Hospital Setup")).toBeInTheDocument();
    expect(screen.getByLabelText(/Unit Code/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Unit Name/)).toBeInTheDocument();
    expect(screen.getByLabelText("Longitude")).toBeInTheDocument();
    expect(screen.getByLabelText("Latitude")).toBeInTheDocument();
  });

  it("captures geo-coordinates via browser API", async () => {
    const user = userEvent.setup();
    mockGetCurrentPosition.mockImplementationOnce((success) => {
      success({
        coords: { longitude: 75.7873, latitude: 26.9124 },
      });
    });

    renderHospitalSetup();

    await user.click(screen.getByRole("button", { name: /Auto-capture/ }));

    await waitFor(() => {
      expect(screen.getByText(/Coordinates captured/)).toBeInTheDocument();
    });

    expect(screen.getByLabelText("Longitude")).toHaveValue("75.7873");
    expect(screen.getByLabelText("Latitude")).toHaveValue("26.9124");
  });

  it("validates required fields and PIN format", async () => {
    const user = userEvent.setup();
    renderHospitalSetup();

    // Submit empty form
    await user.click(screen.getByRole("button", { name: "Create Unit" }));

    expect(await screen.findByText(/Unit code is required/i)).toBeInTheDocument();
    expect(screen.getByText(/Unit name is required/i)).toBeInTheDocument();

    // Fill invalid PIN
    await user.type(screen.getByLabelText(/Unit Code/), "H1");
    await user.type(screen.getByLabelText(/Unit Name/), "Hospital");
    await user.type(screen.getByLabelText("PIN Code"), "1234");
    await user.click(screen.getByRole("button", { name: "Create Unit" }));

    expect(await screen.findByText(/PIN code must be 6 digits/i)).toBeInTheDocument();
  });

  it("creates the unit with geo-coordinates (legacy LATIUTDE column)", async () => {
    const user = userEvent.setup();
    renderHospitalSetup();

    await user.type(screen.getByLabelText(/Unit Code/), "HOSP-001");
    await user.type(screen.getByLabelText(/Unit Name/), "Jaipur Hospital");
    await user.type(screen.getByLabelText("City"), "Jaipur");
    await user.type(screen.getByLabelText("State"), "Rajasthan");
    await user.type(screen.getByLabelText("PIN Code"), "302001");
    await user.type(screen.getByLabelText("Longitude"), "75.7873");
    await user.type(screen.getByLabelText("Latitude"), "26.9124");

    await user.click(screen.getByRole("button", { name: "Create Unit" }));

    await waitFor(() => expect(api.create).toHaveBeenCalledTimes(1));

    const [entity, payload] = api.create.mock.calls[0];
    expect(entity).toBe("unitmaster");
    expect(payload).toMatchObject({
      code: "HOSP-001",
      name: "Jaipur Hospital",
      city: "Jaipur",
      longitude: "75.7873",
      latiutde: "26.9124",
    });

    // Success alert
    expect(await screen.getByText(/Unit created/i)).toBeInTheDocument();
  });

  it("shows error on API failure", async () => {
    const user = userEvent.setup();
    api.create.mockRejectedValue(
      Object.assign(new Error("Code already exists"), { code: "BEXA003", status: 409, name: "ApiError" }),
    );

    renderHospitalSetup();

    await user.type(screen.getByLabelText(/Unit Code/), "DUP");
    await user.type(screen.getByLabelText(/Unit Name/), "Duplicate");
    await user.click(screen.getByRole("button", { name: "Create Unit" }));

    expect(await screen.findByText(/Code already exists/i)).toBeInTheDocument();
  });
});
