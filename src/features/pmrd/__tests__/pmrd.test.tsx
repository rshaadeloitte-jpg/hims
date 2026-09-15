import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import {
  usePatientEncounters,
  useJsyForms,
  useSaveJsyForm,
  usePmrdDocuments,
  useUploadPmrdDocument,
} from "../queries";
import {
  jsyJ1Schema,
  jsyJ2Schema,
  jsyJ3Schema,
  hFormSchema,
  pmrdDocumentUploadSchema,
  JSY_FORM_TYPES,
  PMRD_DOC_CATEGORIES,
} from "../zodSchema";

/**
 * M24 PMRD — wire-level + schema validation tests.
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

describe("usePatientEncounters", () => {
  it("hits the patient-encounters endpoint with patientId filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, patientId: 5, patientName: "Rani", encounterType: "OPD", encounterDate: "2026-09-01", status: "ACTIVE" },
      ],
    });

    const { result } = renderHook(() => usePatientEncounters({ patientId: 5 }), { wrapper: testWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/patient-encounters?patientId=5",
      expect.objectContaining({ headers: expect.any(Object) }),
    );
    expect(result.current.data?.[0]?.patientName).toBe("Rani");
  });

  it("encodes all filter params", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [],
    });

    const { result } = renderHook(
      () => usePatientEncounters({ patientId: 10, encounterType: "IPD", fromDate: "2026-01-01", toDate: "2026-12-31" }),
      { wrapper: testWrapper() },
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/patient-encounters?patientId=10&encounterType=IPD&fromDate=2026-01-01&toDate=2026-12-31",
      expect.any(Object),
    );
  });
});

describe("useJsyForms", () => {
  it("hits the jsy-forms endpoint with formType filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, formType: "J1", patientId: 5, beneficiaryName: "Rani", status: "SUBMITTED", submittedDate: "2026-09-01" },
      ],
    });

    const { result } = renderHook(() => useJsyForms({ formType: "J1" }), { wrapper: testWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/jsy-forms?formType=J1",
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.formType).toBe("J1");
  });
});

describe("useSaveJsyForm", () => {
  it("POSTs the form payload and returns the created record", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 42, formType: "J1", status: "SUBMITTED" }),
    });

    const { result } = renderHook(() => useSaveJsyForm(), { wrapper: testWrapper() });
    const payload = {
      formType: "J1",
      patientId: 5,
      beneficiaryName: "Rani Devi",
      husbandName: "Ram",
      address: "Village X",
      category: "BPL",
      ancRegistrationDate: "2026-01-15",
      ancVisits: 4,
      deliveryDate: "2026-09-01",
      deliveryType: "NORMAL",
      placeOfDelivery: "PHC",
      childGender: "FEMALE",
      childWeight: 3.2,
      anmName: "Sita",
      anmMobile: "9876543210",
      submittedDate: "2026-09-02",
    };

    const created = await result.current.mutateAsync(payload);
    expect(created.id).toBe(42);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/jsy-forms",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  });
});

describe("usePmrdDocuments", () => {
  it("hits the documents endpoint with patientId filter", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: async () => [
        { id: 1, fileName: "j1_form.pdf", fileType: "application/pdf", fileSize: 102400, documentCategory: "JSY_FORM", patientId: 5 },
      ],
    });

    const { result } = renderHook(() => usePmrdDocuments({ patientId: 5 }), { wrapper: testWrapper() });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/documents?patientId=5",
      expect.any(Object),
    );
    expect(result.current.data?.[0]?.documentCategory).toBe("JSY_FORM");
  });
});

describe("useUploadPmrdDocument", () => {
  it("POSTs the document payload", async () => {
    fetchMock.mockResolvedValueOnce({
      ok: true,
      status: 201,
      json: async () => ({ id: 10, fileName: "scan.pdf", documentCategory: "ANC_RECORD" }),
    });

    const { result } = renderHook(() => useUploadPmrdDocument(), { wrapper: testWrapper() });
    const payload = {
      fileName: "scan.pdf",
      fileType: "application/pdf",
      fileData: "base64data",
      documentCategory: "ANC_RECORD",
      patientId: 5,
    };

    const created = await result.current.mutateAsync(payload);
    expect(created.id).toBe(10);
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/pmrd/documents",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify(payload),
      }),
    );
  });
});

// ---- Schema validation ------------------------------------------------------

describe("jsyJ1Schema", () => {
  it("validates a complete J1 form", () => {
    const valid = {
      formType: "J1" as const,
      patientId: 5,
      beneficiaryName: "Rani Devi",
      husbandName: "Ram",
      address: "Village X, Block Y",
      category: "BPL" as const,
      bplCardNo: "BPL123",
      mobileNo: "9876543210",
      ancRegistrationDate: "2026-01-15",
      ancVisits: 4,
      deliveryDate: "2026-09-01",
      deliveryType: "NORMAL" as const,
      placeOfDelivery: "PHC",
      childGender: "FEMALE" as const,
      childWeight: 3.2,
      anmName: "Sita",
      anmMobile: "9876543210",
      submittedDate: "2026-09-02",
    };
    expect(jsyJ1Schema.safeParse(valid).success).toBe(true);
  });

  it("rejects missing required fields", () => {
    const invalid = {
      formType: "J1" as const,
      patientId: 5,
      beneficiaryName: "",
    };
    const result = jsyJ1Schema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it("rejects invalid mobile number", () => {
    const invalid = {
      formType: "J1" as const,
      patientId: 5,
      beneficiaryName: "Rani",
      husbandName: "Ram",
      address: "Village X",
      category: "BPL" as const,
      anmName: "Sita",
      anmMobile: "123", // invalid
      submittedDate: "2026-09-02",
    };
    const result = jsyJ1Schema.safeParse(invalid);
    expect(result.success).toBe(false);
  });
});

describe("jsyJ2Schema", () => {
  it("validates a complete J2 form", () => {
    const valid = {
      formType: "J2" as const,
      patientId: 5,
      beneficiaryName: "Rani Devi",
      facilityName: "District Hospital",
      facilityCode: "DH001",
      moName: "Dr. Sharma",
      moRegistrationNo: "MC12345",
      deliveryDate: "2026-09-01",
      deliveryType: "CESAREAN" as const,
      dischargeDate: "2026-09-05",
      amountClaimed: 5000,
      submittedDate: "2026-09-06",
    };
    expect(jsyJ2Schema.safeParse(valid).success).toBe(true);
  });
});

describe("jsyJ3Schema", () => {
  it("validates a complete J3 form", () => {
    const valid = {
      formType: "J3" as const,
      districtName: "Jaipur",
      blockName: "Sanganer",
      month: "September",
      year: 2026,
      totalJ1Received: 150,
      totalJ2Received: 140,
      totalAmountDisbursed: 700000,
      submittedBy: "Block Officer",
      submittedDate: "2026-10-01",
    };
    expect(jsyJ3Schema.safeParse(valid).success).toBe(true);
  });
});

describe("hFormSchema", () => {
  it("validates a complete H-form", () => {
    const valid = {
      formType: "H_FORM" as const,
      patientId: 5,
      beneficiaryName: "Rani Devi",
      highRiskReason: "Previous C-section",
      lmp: "2026-01-01",
      edd: "2026-10-08",
      riskFactors: ["Previous C-section", "Anemia"],
      actionTaken: "Referred to district hospital",
      anmName: "Sita",
      submittedDate: "2026-09-02",
    };
    expect(hFormSchema.safeParse(valid).success).toBe(true);
  });
});

describe("pmrdDocumentUploadSchema", () => {
  it("validates a complete document upload", () => {
    const valid = {
      fileName: "j1_form.pdf",
      fileType: "application/pdf",
      fileData: "base64encodeddata",
      documentCategory: "JSY_FORM" as const,
      patientId: 5,
      encounterId: 10,
      description: "J1 form scan",
    };
    expect(pmrdDocumentUploadSchema.safeParse(valid).success).toBe(true);
  });

  it("rejects missing patientId", () => {
    const invalid = {
      fileName: "test.pdf",
      fileType: "application/pdf",
      fileData: "base64data",
      documentCategory: "JSY_FORM" as const,
    };
    const result = pmrdDocumentUploadSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });

  it("rejects invalid document category", () => {
    const invalid = {
      fileName: "test.pdf",
      fileType: "application/pdf",
      fileData: "base64data",
      documentCategory: "INVALID_CATEGORY",
      patientId: 5,
    };
    const result = pmrdDocumentUploadSchema.safeParse(invalid);
    expect(result.success).toBe(false);
  });
});

// ---- Constants --------------------------------------------------------------

describe("JSY_FORM_TYPES", () => {
  it("contains all four form types", () => {
    expect(JSY_FORM_TYPES).toEqual(["J1", "J2", "J3", "H_FORM"]);
  });
});

describe("PMRD_DOC_CATEGORIES", () => {
  it("contains PMRD-specific categories", () => {
    expect(PMRD_DOC_CATEGORIES).toContain("JSY_FORM");
    expect(PMRD_DOC_CATEGORIES).toContain("ANC_RECORD");
    expect(PMRD_DOC_CATEGORIES).toContain("DELIVERY_RECORD");
    expect(PMRD_DOC_CATEGORIES).toContain("BPL_CARD");
  });
});
