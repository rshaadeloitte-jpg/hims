import { describe, expect, it } from "vitest";
import {
  appConfigSchema,
  auditSchema,
  hospitalSetupSchema,
  parseDiff,
  auditActionSchema,
  AUDIT_ACTIONS,
} from "./zodSchema";

describe("appConfigSchema", () => {
  it("accepts a valid config entry", () => {
    const result = appConfigSchema.safeParse({
      key: "app.feature.enabled",
      value: "true",
      description: "Feature flag",
    });
    expect(result.success).toBe(true);
  });

  it("rejects blank key", () => {
    const result = appConfigSchema.safeParse({ key: "", value: "x" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain("key");
    }
  });

  it("allows empty value (nullable config)", () => {
    const result = appConfigSchema.safeParse({ key: "x", value: "" });
    expect(result.success).toBe(true);
  });

  it("treats blank description as undefined", () => {
    const parsed = appConfigSchema.parse({ key: "x", description: "" });
    expect(parsed.description).toBeUndefined();
  });

  it("enforces max key length (120)", () => {
    const result = appConfigSchema.safeParse({ key: "a".repeat(121) });
    expect(result.success).toBe(false);
  });
});

describe("auditSchema", () => {
  it("accepts a valid audit entry", () => {
    const result = auditSchema.safeParse({
      entityType: "Patient",
      entityId: 42,
      action: "UPDATE",
      actorId: 7,
      actorUsername: "alice",
      diff: { name: { old: "Bob", new: "Alice" } },
    });
    expect(result.success).toBe(true);
  });

  it("requires entityType", () => {
    const result = auditSchema.safeParse({ entityId: 1, action: "CREATE" });
    expect(result.success).toBe(false);
  });

  it("requires action", () => {
    const result = auditSchema.safeParse({ entityType: "X", entityId: 1 });
    expect(result.success).toBe(false);
  });

  it("coerces entityId string to number", () => {
    const parsed = auditSchema.parse({ entityType: "X", entityId: "42", action: "CREATE" });
    expect(parsed.entityId).toBe(42);
    expect(typeof parsed.entityId).toBe("number");
  });

  it("rejects non-positive entityId", () => {
    const result = auditSchema.safeParse({ entityType: "X", entityId: 0, action: "CREATE" });
    expect(result.success).toBe(false);
  });

  it("allows null diff (append-only entry without change detail)", () => {
    const result = auditSchema.safeParse({
      entityType: "Patient",
      entityId: 1,
      action: "VIEW",
    });
    expect(result.success).toBe(true);
  });
});

describe("hospitalSetupSchema", () => {
  it("accepts a valid hospital setup", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "HOSP-001",
      name: "Jaipur Hospital",
      city: "Jaipur",
      state: "Rajasthan",
      pincode: "302001",
      longitude: "75.7873",
      latitude: "26.9124",
    });
    expect(result.success).toBe(true);
  });

  it("requires code and name", () => {
    const result = hospitalSetupSchema.safeParse({ code: "", name: "" });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("code");
      expect(paths).toContain("name");
    }
  });

  it("validates 6-digit PIN code", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "H1",
      name: "Hospital",
      pincode: "1234",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.path).toContain("pincode");
    }
  });

  it("validates email format", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "H1",
      name: "Hospital",
      email: "not-an-email",
    });
    expect(result.success).toBe(false);
  });

  it("validates longitude format", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "H1",
      name: "Hospital",
      longitude: "abc",
    });
    expect(result.success).toBe(false);
  });

  it("validates latitude format", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "H1",
      name: "Hospital",
      latitude: "abc",
    });
    expect(result.success).toBe(false);
  });

  it("accepts negative coordinates", () => {
    const result = hospitalSetupSchema.safeParse({
      code: "H1",
      name: "Hospital",
      longitude: "-75.7873",
      latitude: "-26.9124",
    });
    expect(result.success).toBe(true);
  });

  it("treats blank optional fields as undefined", () => {
    const parsed = hospitalSetupSchema.parse({
      code: "H1",
      name: "Hospital",
      description: "",
      city: "",
      pincode: "",
      email: "",
      longitude: "",
      latitude: "",
    });
    expect(parsed.description).toBeUndefined();
    expect(parsed.city).toBeUndefined();
    expect(parsed.pincode).toBeUndefined();
    expect(parsed.email).toBeUndefined();
    expect(parsed.longitude).toBeUndefined();
    expect(parsed.latitude).toBeUndefined();
  });
});

describe("parseDiff", () => {
  it("parses valid JSONB diff", () => {
    const result = parseDiff('{"name":{"old":"Bob","new":"Alice"}}');
    expect(result).toEqual({ name: { old: "Bob", new: "Alice" } });
  });

  it("returns null for null input", () => {
    expect(parseDiff(null)).toBeNull();
    expect(parseDiff(undefined)).toBeNull();
  });

  it("returns null for empty string", () => {
    expect(parseDiff("")).toBeNull();
  });

  it("returns null for invalid JSON", () => {
    expect(parseDiff("{invalid")).toBeNull();
  });

  it("returns null for non-object JSON", () => {
    expect(parseDiff('"string"')).toBeNull();
    expect(parseDiff("42")).toBeNull();
  });
});

describe("auditActionSchema", () => {
  it("accepts all Linear-style actions", () => {
    for (const action of AUDIT_ACTIONS) {
      expect(auditActionSchema.safeParse(action).success).toBe(true);
    }
  });

  it("rejects unknown actions", () => {
    expect(auditActionSchema.safeParse("FLIGHT").success).toBe(false);
  });
});
