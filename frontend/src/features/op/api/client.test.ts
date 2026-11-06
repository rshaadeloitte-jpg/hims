import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { appointmentApi } from "./client";

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  fetchMock.mockClear();
});

describe("op client departments", () => {
  it("calls the real URL with the unitId", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.departments(42);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/masters/departmentmaster/all?unitId=42",
    );
  });

  it("calls /all with no param when there is no unit", async () => {
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    });
    await appointmentApi.departments(null);
    expect(String(fetchMock.mock.calls[0][0])).toBe(
      "http://localhost:3000/api/v1/masters/departmentmaster/all",
    );
  });
});