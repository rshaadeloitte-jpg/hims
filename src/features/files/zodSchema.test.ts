import { describe, expect, it } from "vitest";
import {
  fileUploadSchema,
  fileFilterSchema,
  fileCategorySchema,
  annotationSchema,
  FILE_CATEGORIES,
  OWNER_TYPES,
  type FileUploadValues,
} from "./zodSchema";

/**
 * M34 File/Document Management — Zod schema validation tests.
 * Stripe-style: real-time, field-level, progressive disclosure.
 */

describe("fileUploadSchema", () => {
  const validBase64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

  it("validates a complete upload payload", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "test.png",
      fileType: "image/png",
      fileData: validBase64.split(",")[1],
      fileCategoryId: 1,
      ownerType: "PATIENT",
      ownerId: 123,
    });
    expect(result.success).toBe(true);
  });

  it("accepts data URI with base64 prefix", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "scan.jpg",
      fileType: "image/jpeg",
      fileData: validBase64,
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty file name", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "",
      fileType: "image/png",
      fileData: validBase64.split(",")[1],
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("fileName");
    }
  });

  it("rejects invalid MIME type", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "test.png",
      fileType: "not-a-mime-type",
      fileData: validBase64.split(",")[1],
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid characters in file name", () => {
    const result = fileUploadSchema.safeParse({
      fileName: 'test<>file.png',
      fileType: "image/png",
      fileData: validBase64.split(",")[1],
    });
    expect(result.success).toBe(false);
  });

  it("allows optional fields to be undefined", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "doc.pdf",
      fileType: "application/pdf",
      fileData: validBase64.split(",")[1],
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.fileCategoryId).toBeUndefined();
      expect(result.data.ownerType).toBeUndefined();
      expect(result.data.ownerId).toBeUndefined();
    }
  });

  it("accepts all valid owner types", () => {
    for (const ownerType of OWNER_TYPES) {
      const result = fileUploadSchema.safeParse({
        fileName: "test.png",
        fileType: "image/png",
        fileData: validBase64.split(",")[1],
        ownerType,
      });
      expect(result.success).toBe(true);
    }
  });

  it("rejects invalid owner type", () => {
    const result = fileUploadSchema.safeParse({
      fileName: "test.png",
      fileType: "image/png",
      fileData: validBase64.split(",")[1],
      ownerType: "INVALID",
    });
    expect(result.success).toBe(false);
  });

  it("rejects base64 exceeding 10 MB limit", () => {
    // Generate a string larger than 10MB * 1.37 (base64 overhead factor)
    const largeData = "A".repeat(15 * 1024 * 1024);
    const result = fileUploadSchema.safeParse({
      fileName: "large.bin",
      fileType: "application/octet-stream",
      fileData: largeData,
    });
    expect(result.success).toBe(false);
  });
});

describe("fileFilterSchema", () => {
  it("applies defaults for pagination", () => {
    const result = fileFilterSchema.parse({});
    expect(result.page).toBe(0);
    expect(result.size).toBe(20);
  });

  it("coerces numeric strings", () => {
    const result = fileFilterSchema.parse({
      ownerId: "42",
      fileCategoryId: "5",
      page: "2",
      size: "50",
    });
    expect(result.ownerId).toBe(42);
    expect(result.fileCategoryId).toBe(5);
    expect(result.page).toBe(2);
    expect(result.size).toBe(50);
  });

  it("accepts all valid owner types", () => {
    for (const ownerType of OWNER_TYPES) {
      const result = fileFilterSchema.safeParse({ ownerType });
      expect(result.success).toBe(true);
    }
  });
});

describe("fileCategorySchema", () => {
  it("validates a valid category", () => {
    const result = fileCategorySchema.safeParse({
      code: "RADIOLOGY",
      name: "Radiology Reports",
      documentType: "IMAGE",
    });
    expect(result.success).toBe(true);
  });

  it("rejects non-UPPER_SNAKE_CASE codes", () => {
    const result = fileCategorySchema.safeParse({
      code: "radiology",
      name: "Radiology",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty name", () => {
    const result = fileCategorySchema.safeParse({
      code: "RADIOLOGY",
      name: "",
    });
    expect(result.success).toBe(false);
  });

  it("allows optional parentId", () => {
    const result = fileCategorySchema.safeParse({
      code: "XRAY",
      name: "X-Ray",
      parentId: 1,
    });
    expect(result.success).toBe(true);
  });
});

describe("annotationSchema", () => {
  it("validates a JSON annotation string", () => {
    const result = annotationSchema.safeParse({
      annotationJson: JSON.stringify([{ type: "rectangle", x: 10, y: 20 }]),
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid JSON", () => {
    const result = annotationSchema.safeParse({
      annotationJson: "not-json",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty annotation", () => {
    const result = annotationSchema.safeParse({
      annotationJson: "",
    });
    expect(result.success).toBe(false);
  });
});

describe("FILE_CATEGORIES", () => {
  it("contains expected categories", () => {
    expect(FILE_CATEGORIES).toContain("RADIOLOGY");
    expect(FILE_CATEGORIES).toContain("LAB_REPORTS");
    expect(FILE_CATEGORIES).toContain("PRESCRIPTION");
    expect(FILE_CATEGORIES).toContain("CONSENT_FORM");
  });
});

describe("OWNER_TYPES", () => {
  it("contains expected types", () => {
    expect(OWNER_TYPES).toContain("PATIENT");
    expect(OWNER_TYPES).toContain("ENCOUNTER");
    expect(OWNER_TYPES).toContain("ADMISSION");
    expect(OWNER_TYPES).toContain("EQUIPMENT");
  });
});