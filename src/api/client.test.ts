import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, legacyWirePage, mastersApi } from "./client";

/**
 * Client tests: error-envelope parsing + wire behaviour against a mocked fetch.
 * Uses plain fetch-like objects (no real network, no global Response needed).
 */

type FetchLike = (input: RequestInfo | URL, init?: RequestInit) => Promise<unknown>;

const fetchMock = vi.fn<FetchLike>();

const jsonResponse = (status: number, body: unknown) => ({
  ok: status >= 200 && status < 300,
  status,
  json: async () => body,
});

beforeEach(() => {
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  vi.unstubAllGlobals();
  fetchMock.mockClear();
});

describe("ApiError envelope parsing", () => {
  it("parses the reference-service envelope { status, errorCode, message }", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, { status: "BAD_REQUEST", errorCode: "BEXA003", message: "Invalid input data !" }),
    );
    await expect(mastersApi.listScoped("unitmaster", {})).rejects.toMatchObject({
      status: 400,
      code: "BEXA003",
      message: "Invalid input data !",
    });
  });

  it("parses the generic envelope { code, message, details }", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(404, { code: "NOT_FOUND", message: "Record not found", details: { id: 99 } }),
    );
    const err = await mastersApi.getById("unitmaster", 99).catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).code).toBe("NOT_FOUND");
    expect((err as ApiError).message).toBe("Record not found");
    expect((err as ApiError).details).toEqual({ id: 99 });
  });

  it("falls back for non-JSON error bodies (e.g. plain 401)", async () => {
    fetchMock.mockResolvedValue({ ok: false, status: 401, json: async () => { throw new Error("not json"); } });
    const err = await mastersApi.listEntities().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).status).toBe(401);
    expect((err as ApiError).code).toBe("HTTP_401");
  });

  it("wraps network failures as NETWORK_ERROR", async () => {
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));
    const err = await mastersApi.listEntities().catch((e: unknown) => e);
    expect(err).toBeInstanceOf(ApiError);
    expect((err as ApiError).code).toBe("NETWORK_ERROR");
    expect((err as ApiError).status).toBe(0);
  });
});

describe("request wiring", () => {
  it("sends the DEV basic-auth header and JSON accept on list calls", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, [{ id: 7 }]));
    await mastersApi.listScoped("unitmaster", { unitId: 1, search: "jai", searchFld: "name", page: 0, count: 10 });

    const [input, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(String(input)).toContain("/api/masters/unitmaster");
    expect(String(input)).toContain("unitId=1");
    expect(String(input)).toContain("search=jai");
    expect(String(input)).toContain("searchFld=name");
    expect(String(input)).toContain("page=0");
    expect(String(input)).toContain("count=10");
    expect((init.headers as Record<string, string>).Authorization).toBe(
      `Basic ${btoa("devops:devops-dev-only")}`,
    );
  });

  it("POSTs JSON on create and PUTs JSON on update (MIG-010 write surface)", async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 77 }));
    await mastersApi.create("unitmaster", { code: "U077", name: "New Unit" });
    const [createUrl, createInit] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(String(createUrl)).toBe("http://localhost:3000/api/masters/unitmaster");
    expect(createInit.method).toBe("POST");
    expect((createInit.headers as Record<string, string>)["Content-Type"]).toBe("application/json");
    expect(JSON.parse(String(createInit.body))).toEqual({ code: "U077", name: "New Unit" });

    fetchMock.mockResolvedValue(jsonResponse(200, { id: 77 }));
    await mastersApi.update("unitmaster", 77, { name: "Renamed" });
    const [updateUrl, updateInit] = fetchMock.mock.calls[1] as unknown as [string, RequestInit];
    expect(String(updateUrl)).toBe("http://localhost:3000/api/masters/unitmaster/77");
    expect(updateInit.method).toBe("PUT");
  });

  it("sends unitId on scoped GET-by-id, create and PUT (red-team F-6)", async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 5, unitId: 7, code: "D01" }));

    await mastersApi.getById("departmentmaster", 5, 7);
    expect(String(fetchMock.mock.calls[0][0])).toContain("unitId=7");
    expect(String(fetchMock.mock.calls[0][0])).toContain("/api/masters/departmentmaster/5");

    // create with unitId query param (server: scope = unitId param ?? payload.unitId)
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 6 }));
    await mastersApi.create("departmentmaster", { code: "D02" }, 7);
    expect(String(fetchMock.mock.calls[1][0])).toContain("unitId=7");

    // PUT with unitId (scoped update; without it the backend answers BEXA031)
    fetchMock.mockResolvedValue(jsonResponse(200, { id: 5 }));
    await mastersApi.update("departmentmaster", 5, { description: "Edited" }, 7);
    expect(String(fetchMock.mock.calls[2][0])).toContain("/api/masters/departmentmaster/5");
    expect(String(fetchMock.mock.calls[2][0])).toContain("unitId=7");
    expect((fetchMock.mock.calls[2][1] as RequestInit).method).toBe("PUT");
  });
});

describe("legacy pagination semantics (offset = (page / count) * count)", () => {
  it("sends page = pageIndex * pageSize so the server offset matches", () => {
    expect(legacyWirePage(0, 10)).toBe(0);
    expect(legacyWirePage(1, 10)).toBe(10);
    expect(legacyWirePage(3, 25)).toBe(75);
  });
});
