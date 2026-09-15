import { describe, it, expect } from "vitest";
import {
  kioskRegistrationSchema,
  appointmentBookingSchema,
  labResultUploadSchema,
} from "./zodSchema";

describe("kioskRegistrationSchema", () => {
  it("validates a correct registration form", () => {
    const result = kioskRegistrationSchema.safeParse({
      mobileNo: "9876543210",
      firstName: "John",
      lastName: "Doe",
      gender: "M",
      dob: "1990-01-15",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid mobile number", () => {
    const result = kioskRegistrationSchema.safeParse({
      mobileNo: "123",
      firstName: "John",
      lastName: "Doe",
      gender: "M",
      dob: "1990-01-15",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe("Invalid 10-digit mobile number");
    }
  });

  it("rejects empty first name", () => {
    const result = kioskRegistrationSchema.safeParse({
      mobileNo: "9876543210",
      firstName: "",
      lastName: "Doe",
      gender: "M",
      dob: "1990-01-15",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].message).toBe("First name required");
    }
  });

  it("rejects invalid gender", () => {
    const result = kioskRegistrationSchema.safeParse({
      mobileNo: "9876543210",
      firstName: "John",
      lastName: "Doe",
      gender: "X",
      dob: "1990-01-15",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid date format", () => {
    const result = kioskRegistrationSchema.safeParse({
      mobileNo: "9876543210",
      firstName: "John",
      lastName: "Doe",
      gender: "M",
      dob: "01-15-1990",
    });
    expect(result.success).toBe(false);
  });
});

describe("appointmentBookingSchema", () => {
  it("validates a correct booking form", () => {
    const result = appointmentBookingSchema.safeParse({
      patientName: "Jane Doe",
      mobileNo: "9876543210",
      departmentId: 1,
      doctorId: 10,
      date: "2026-09-15",
      slotId: 100,
      reason: "Follow-up",
    });
    expect(result.success).toBe(true);
  });

  it("validates without optional reason", () => {
    const result = appointmentBookingSchema.safeParse({
      patientName: "Jane Doe",
      mobileNo: "9876543210",
      departmentId: 1,
      doctorId: 10,
      date: "2026-09-15",
      slotId: 100,
    });
    expect(result.success).toBe(true);
  });

  it("rejects zero departmentId", () => {
    const result = appointmentBookingSchema.safeParse({
      patientName: "Jane Doe",
      mobileNo: "9876543210",
      departmentId: 0,
      doctorId: 10,
      date: "2026-09-15",
      slotId: 100,
    });
    expect(result.success).toBe(false);
  });

  it("rejects zero slotId", () => {
    const result = appointmentBookingSchema.safeParse({
      patientName: "Jane Doe",
      mobileNo: "9876543210",
      departmentId: 1,
      doctorId: 10,
      date: "2026-09-15",
      slotId: 0,
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid mobile", () => {
    const result = appointmentBookingSchema.safeParse({
      patientName: "Jane Doe",
      mobileNo: "abc",
      departmentId: 1,
      doctorId: 10,
      date: "2026-09-15",
      slotId: 100,
    });
    expect(result.success).toBe(false);
  });
});

describe("labResultUploadSchema", () => {
  it("validates a correct upload form", () => {
    const file = new File(["test"], "result.pdf", { type: "application/pdf" });
    const result = labResultUploadSchema.safeParse({
      tokenNo: "TKN-001",
      visitId: "VIS-001",
      testName: "CBC",
      resultValue: "135",
      resultDate: "2026-09-15",
      file,
    });
    expect(result.success).toBe(true);
  });

  it("validates without optional file", () => {
    const result = labResultUploadSchema.safeParse({
      tokenNo: "TKN-001",
      visitId: "VIS-001",
      testName: "CBC",
      resultValue: "135",
      resultDate: "2026-09-15",
    });
    expect(result.success).toBe(true);
  });

  it("rejects empty token number", () => {
    const result = labResultUploadSchema.safeParse({
      tokenNo: "",
      visitId: "VIS-001",
      testName: "CBC",
      resultValue: "135",
      resultDate: "2026-09-15",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty test name", () => {
    const result = labResultUploadSchema.safeParse({
      tokenNo: "TKN-001",
      visitId: "VIS-001",
      testName: "",
      resultValue: "135",
      resultDate: "2026-09-15",
    });
    expect(result.success).toBe(false);
  });

  it("rejects empty result value", () => {
    const result = labResultUploadSchema.safeParse({
      tokenNo: "TKN-001",
      visitId: "VIS-001",
      testName: "CBC",
      resultValue: "",
      resultDate: "2026-09-15",
    });
    expect(result.success).toBe(false);
  });

  it("rejects invalid date format", () => {
    const result = labResultUploadSchema.safeParse({
      tokenNo: "TKN-001",
      visitId: "VIS-001",
      testName: "CBC",
      resultValue: "135",
      resultDate: "15-09-2026",
    });
    expect(result.success).toBe(false);
  });
});
