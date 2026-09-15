import { z } from "zod";

describe("bloodbank zod schemas", () => {
  it("donorSchema rejects empty name", () => {
    const s = z.object({
      name: z.string().min(1, "Name required"),
      age: z.coerce.number().int().positive("Age required"),
      gender: z.enum(["MALE", "FEMALE", "OTHER"]),
      bloodGroup: z.string().min(1, "Blood group required"),
    });
    const r = s.safeParse({ name: "", age: "25", gender: "FEMALE", bloodGroup: "A+" });
    expect(r.success).toBe(false);
  });

  it("donorSchema coerces age string to number", () => {
    const s = z.object({
      name: z.string().min(1),
      age: z.coerce.number().int().positive(),
      gender: z.enum(["MALE", "FEMALE", "OTHER"]),
      bloodGroup: z.string().min(1),
    });
    const r = s.safeParse({ name: "Riya", age: "25", gender: "FEMALE", bloodGroup: "A+" });
    expect(r.success).toBe(true);
    if (r.success) expect(r.data.age).toBe(25);
  });

  it("bloodBagSchema requires collection and expiry dates", () => {
    const s = z.object({
      bloodGroup: z.string().min(1),
      component: z.string().min(1),
      donorId: z.coerce.number().int().positive(),
      volume: z.coerce.number().positive(),
      collectionDate: z.string().min(1),
      expiryDate: z.string().min(1),
    });
    const r = s.safeParse({ bloodGroup: "A+", component: "WHOLE_BLOOD", donorId: 1, volume: 450, collectionDate: "", expiryDate: "2027-01-08" });
    expect(r.success).toBe(false);
  });

  it("crossMatchSchema validates method enum", () => {
    const s = z.object({
      bagId: z.coerce.number().int().positive(),
      patientId: z.coerce.number().int().positive(),
      method: z.enum(["MAJOR", "MINOR", "BOTH"]),
    });
    const ok = s.safeParse({ bagId: 1, patientId: 2, method: "MAJOR" });
    expect(ok.success).toBe(true);
    const bad = s.safeParse({ bagId: 1, patientId: 2, method: "INVALID" });
    expect(bad.success).toBe(false);
  });

  it("indentSchema requires positive units", () => {
    const s = z.object({
      sourceStore: z.string().min(1),
      targetStore: z.string().min(1),
      bloodGroup: z.string().min(1),
      component: z.string().min(1),
      units: z.coerce.number().int().positive(),
    });
    const ok = s.safeParse({ sourceStore: "BB-MAIN", targetStore: "BB-OT", bloodGroup: "O+", component: "WHOLE_BLOOD", units: 2 });
    expect(ok.success).toBe(true);
    const bad = s.safeParse({ sourceStore: "", targetStore: "", bloodGroup: "", component: "", units: 0 });
    expect(bad.success).toBe(false);
  });
});
