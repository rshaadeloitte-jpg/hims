import { describe, expect, it } from "vitest";
import {
  reportMasterSchema,
  executeReportSchema,
  reportFilterSchema,
  buildTabularDefinition,
  buildSummaryDefinition,
} from "./zodSchema";

describe("reportMasterSchema", () => {
  it("validates a correct report definition", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "opd_summary",
      title: "OPD Summary",
      kind: "1",
      definition: '{"table":"encounters","columns":["id","date"]}',
    });
    expect(result.success).toBe(true);
  });

  it("rejects blank reportCode", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "",
      title: "OPD Summary",
      kind: "1",
      definition: "{}",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("reportCode");
    }
  });

  it("rejects reportCode longer than 40 chars", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "a".repeat(41),
      title: "OPD Summary",
      kind: "1",
      definition: "{}",
    });
    expect(result.success).toBe(false);
  });

  it("rejects blank title", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "test",
      title: "",
      kind: "1",
      definition: "{}",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("title");
    }
  });

  it("rejects invalid kind (must be 1 or 2)", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "test",
      title: "Test",
      kind: "3",
      definition: "{}",
    });
    expect(result.success).toBe(false);
  });

  it("defaults kind to '1' when omitted", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "test",
      title: "Test",
      definition: "{}",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.kind).toBe("1");
    }
  });

  it("rejects empty definition JSON", () => {
    const result = reportMasterSchema.safeParse({
      reportCode: "test",
      title: "Test",
      kind: "1",
      definition: "",
    });
    expect(result.success).toBe(false);
  });
});

describe("executeReportSchema", () => {
  it("validates a correct date range", () => {
    const result = executeReportSchema.safeParse({
      dateFrom: "2025-01-01",
      dateTo: "2025-01-31",
    });
    expect(result.success).toBe(true);
  });

  it("accepts empty strings (optional)", () => {
    const result = executeReportSchema.safeParse({
      dateFrom: "",
      dateTo: "",
    });
    expect(result.success).toBe(true);
  });

  it("accepts undefined (optional)", () => {
    const result = executeReportSchema.safeParse({});
    expect(result.success).toBe(true);
  });

  it("rejects invalid date format", () => {
    const result = executeReportSchema.safeParse({
      dateFrom: "01-01-2025",
      dateTo: "2025-01-31",
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      const paths = result.error.issues.map((i) => i.path.join("."));
      expect(paths).toContain("dateFrom");
    }
  });

  it("rejects another invalid date format", () => {
    const result = executeReportSchema.safeParse({
      dateFrom: "2025-01-01",
      dateTo: "Jan 31, 2025",
    });
    expect(result.success).toBe(false);
  });
});

describe("reportFilterSchema", () => {
  it("applies defaults correctly", () => {
    const result = reportFilterSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.activeOnly).toBe(true);
    }
  });

  it("validates all fields", () => {
    const result = reportFilterSchema.safeParse({
      search: "opd",
      activeOnly: false,
      kind: "1",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.search).toBe("opd");
      expect(result.data.activeOnly).toBe(false);
      expect(result.data.kind).toBe("1");
    }
  });

  it("rejects invalid kind value", () => {
    const result = reportFilterSchema.safeParse({
      kind: "3",
    });
    expect(result.success).toBe(false);
  });
});

describe("definition builder helpers", () => {
  it("buildTabularDefinition produces correct JSON", () => {
    const def = buildTabularDefinition("rm_encounter_stats", ["stat_date", "encounter_type", "cnt"], "stat_date");
    const parsed = JSON.parse(def);
    expect(parsed.table).toBe("rm_encounter_stats");
    expect(parsed.columns).toEqual(["stat_date", "encounter_type", "cnt"]);
    expect(parsed.dateField).toBe("stat_date");
  });

  it("buildTabularDefinition omits dateField when not provided", () => {
    const def = buildTabularDefinition("my_table", ["col1", "col2"]);
    const parsed = JSON.parse(def);
    expect(parsed.table).toBe("my_table");
    expect(parsed.columns).toEqual(["col1", "col2"]);
    expect(parsed.dateField).toBeUndefined();
  });

  it("buildSummaryDefinition produces correct JSON for cnt", () => {
    const def = buildSummaryDefinition("my_table", "cnt");
    const parsed = JSON.parse(def);
    expect(parsed.table).toBe("my_table");
    expect(parsed.aggregate).toBe("cnt");
  });

  it("buildSummaryDefinition produces correct JSON for total", () => {
    const def = buildSummaryDefinition("my_table", "total");
    const parsed = JSON.parse(def);
    expect(parsed.table).toBe("my_table");
    expect(parsed.aggregate).toBe("total");
  });
});
