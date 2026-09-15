import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { usePatientSearch, useRegisterPatient, useUploadPatientDocument, usePatientDetail } from "./queries";
import { patientRegistrationSchema, mlcSchema, motherChildLinkSchema, covidTrackingSchema } from "./zodSchema";

/**
 * M01 Patient Registration — wire-level + schema validation tests.
 * Pins the wire URLs and validates Zod schemas.
 */

function testWrapper() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
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

// ---- Query hooks ------------------------------------------------------------

describe("usePatientSearch", () => {
  it("is disabled when query is shorter than 2 chars", async () => {
    fetchMock.mockResolvedValue({ ok: true, status: 200, json: async () => [] });
    const { result } = renderHook(() => usePatientSearch("a"), { wrapper: testWrapper() });
    expect(fetchMock).not.toHaveBeenCalled();
    expect(result.current.fetchStatus).toBe("idle");
  });

  it("encodes the search query and type in the URL", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [{ patientId: 1, mrNo: "M1", firstName: "Ravi", mobileNo: "9999999999" }],
    });
    const { result } = renderHook(() => usePatientSearch("Ravi", "NAME"), { wrapper: testWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/patients/search?q=Ravi&type=NAME",
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.patientId).toBe(1);
  });
});

describe("useRegisterPatient", () => {
  it("POSTs the payload and returns the created patient", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ patientId: 100, mrNo: "M100", firstName: "A" }),
    });
    const { result } = renderHook(() => useRegisterPatient(), { wrapper: testWrapper() });
    result.current.mutate({ firstName: "A", mobileNo: "9999999999" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/patients",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ firstName: "A", mobileNo: "9999999999" }),
      }),
    );
    expect(result.current.data?.patientId).toBe(100);
  });
});

describe("usePatientDetail", () => {
  it("fetches patient detail with all M01 fields", async () => {
    const mockPatient = {
      patientId: 42,
      mrNo: "MRN-42",
      firstName: "Priya",
      lastName: "Sharma",
      mobileNo: "9876543210",
      abhaAddress: "priya123@abdm",
      genderId: 2,
      dateOfBirth: "1990-05-15",
      bloodGroup: "O+",
      isMlc: true,
      mlcNumber: "MLC-2024-001",
      mlcFirNumber: "FIR-123",
      mlcPoliceStation: "Central PS",
      mlcNotes: "Accident case",
      isChildPatient: false,
      covidVaccinationStatus: "FULL",
      covidInfectionHistory: false,
    };
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => mockPatient,
    });
    const { result } = renderHook(() => usePatientDetail(42), { wrapper: testWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/api/v1/patients/42",
      expect.any(Object),
    );
    expect(result.current.data?.abhaAddress).toBe("priya123@abdm");
    expect(result.current.data?.isMlc).toBe(true);
    expect(result.current.data?.covidVaccinationStatus).toBe("FULL");
  });
});

describe("useUploadPatientDocument", () => {
  it("posts multipart with the file + documentType", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({
        id: 5,
        patientId: 1,
        documentType: "ID_PROOF",
        fileName: "aadhar.pdf",
        contentType: "application/pdf",
        sizeBytes: 100,
        uploadedBy: 1,
        uploadedAt: "2027-04-01T00:00:00Z",
        storageKey: "key-5",
      }),
    });
    const { result } = renderHook(() => useUploadPatientDocument(1), { wrapper: testWrapper() });
    const file = new File(["abc"], "aadhar.pdf", { type: "application/pdf" });
    result.current.mutate({ file, documentType: "ID_PROOF" });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    const call = fetchMock.mock.calls[0];
    const [url, init] = call as [string, RequestInit];
    expect(url).toBe("/api/api/v1/patients/1/documents");
    expect(init.method).toBe("POST");
    expect((init.headers as Record<string, string>)["Content-Type"]).toBeUndefined();
    expect((init.body as FormData).get("documentType")).toBe("ID_PROOF");
    expect((init.body as FormData).get("file")).toBeInstanceOf(File);
  });
});

// ---- Zod schema validation -------------------------------------------------

describe("patientRegistrationSchema", () => {
  it("validates a minimal valid registration", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "Ravi",
      mobileNo: "9876543210",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty first name", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "",
      mobileNo: "9876543210",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid mobile number", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "Ravi",
      mobileNo: "123",
    });
    expect(result.success).toBe(false);
  });

  it("validates ABHA address format", () => {
    const valid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      abhaAddress: "ravi1234@abdm",
    });
    expect(valid.success).toBe(true);

    const invalid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      abhaAddress: "ab",
    });
    expect(invalid.success).toBe(false);
  });

  it("validates Aadhaar format", () => {
    const valid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      aadhaarNumber: "1234 5678 9012",
    });
    expect(valid.success).toBe(true);

    const invalid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      aadhaarNumber: "12345",
    });
    expect(invalid.success).toBe(false);
  });

  it("validates pincode format", () => {
    const valid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      pincode: "302001",
    });
    expect(valid.success).toBe(true);

    const invalid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      pincode: "ABC",
    });
    expect(invalid.success).toBe(false);
  });

  it("validates email format", () => {
    const valid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      email: "test@example.com",
    });
    expect(valid.success).toBe(true);

    const invalid = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      email: "not-an-email",
    });
    expect(invalid.success).toBe(false);
  });

  it("accepts nested MLC data", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      mlc: {
        isMlc: true,
        mlcNumber: "MLC-001",
        mlcFirNumber: "FIR-123",
        mlcPoliceStation: "PS-1",
        mlcNotes: "Notes",
      },
    });
    expect(result.success).toBe(true);
  });

  it("accepts mother-child link data", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "Baby",
      mobileNo: "9876543210",
      motherChildLink: {
        isChildPatient: true,
        motherPatientId: 42,
        motherMrn: "MRN-42",
        birthWeightGrams: 3200,
        gestationalAgeWeeks: 38,
      },
    });
    expect(result.success).toBe(true);
  });

  it("accepts COVID tracking data", () => {
    const result = patientRegistrationSchema.safeParse({
      firstName: "A",
      mobileNo: "9876543210",
      covidTracking: {
        covidVaccinationStatus: "FULL",
        lastCovidVaccineDate: "2023-01-15",
        covidInfectionHistory: true,
        lastCovidInfectionDate: "2022-06-10",
      },
    });
    expect(result.success).toBe(true);
  });
});

describe("mlcSchema", () => {
  it("defaults isMlc to false", () => {
    const result = mlcSchema.parse({});
    expect(result.isMlc).toBe(false);
  });
});

describe("motherChildLinkSchema", () => {
  it("defaults isChildPatient to false", () => {
    const result = motherChildLinkSchema.parse({});
    expect(result.isChildPatient).toBe(false);
  });
});

describe("covidTrackingSchema", () => {
  it("defaults covidInfectionHistory to false", () => {
    const result = covidTrackingSchema.parse({});
    expect(result.covidInfectionHistory).toBe(false);
  });

  it("validates vaccination status enum", () => {
    const valid = covidTrackingSchema.safeParse({ covidVaccinationStatus: "BOOSTER" });
    expect(valid.success).toBe(true);

    const invalid = covidTrackingSchema.safeParse({ covidVaccinationStatus: "INVALID" });
    expect(invalid.success).toBe(false);
  });
});

// ---- renderHook helper (local) ---------------------------------------------

function renderHook<T>(
  hook: () => T,
  options: { wrapper: React.FC<{ children: React.ReactNode }> },
) {
  const { wrapper: Wrapper } = options;
  const result: { current: T } = { current: undefined as unknown as T };
  function TestComponent() {
    result.current = hook();
    return null;
  }
  render(
    <MemoryRouter>
      <Wrapper>
        <TestComponent />
      </Wrapper>
    </MemoryRouter>,
  );
  return { result };
}
