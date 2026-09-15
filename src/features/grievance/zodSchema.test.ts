import { describe, expect, it } from "vitest";
import {
  grievanceSchema,
  feedbackSchema,
  statusChangeSchema,
  ISSUE_STATUS,
  CLOSE_STATUS,
  SEVERITY_CODE,
  FEEDBACK_QUESTIONS,
} from "./zodSchema";

describe("grievanceSchema", () => {
  it("accepts a valid grievance", () => {
    const result = grievanceSchema.safeParse({
      name: "Jane Doe",
      categoryId: 1,
      issue: "Test issue",
      severityId: 2,
      isCitizen: true,
    });
    expect(result.success).toBe(true);
  });

  it("rejects missing name", () => {
    const result = grievanceSchema.safeParse({
      categoryId: 1,
      issue: "Test",
      severityId: 2,
    });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.errors[0].path).toContain("name");
    }
  });

  it("rejects empty issue", () => {
    const result = grievanceSchema.safeParse({
      name: "Jane Doe",
      categoryId: 1,
      issue: "",
      severityId: 2,
    });
    expect(result.success).toBe(false);
  });

  it("coerces string numbers for ids", () => {
    const result = grievanceSchema.safeParse({
      name: "Jane",
      categoryId: "3",
      issue: "Problem",
      severityId: "2",
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.categoryId).toBe(3);
      expect(result.data.severityId).toBe(2);
    }
  });

  it("defaults isCitizen to true", () => {
    const result = grievanceSchema.safeParse({
      name: "Jane",
      categoryId: 1,
      issue: "Test",
      severityId: 2,
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.isCitizen).toBe(true);
    }
  });

  it("rejects negative severity", () => {
    const result = grievanceSchema.safeParse({
      name: "Jane",
      categoryId: 1,
      issue: "Test",
      severityId: -1,
    });
    expect(result.success).toBe(false);
  });
});

describe("feedbackSchema", () => {
  it("accepts valid feedback with all 5 questions", () => {
    const result = feedbackSchema.safeParse({
      grievanceId: 1,
      question1: 4,
      question2: 5,
      question3: 3,
      question4: 4,
      question5: 5,
    });
    expect(result.success).toBe(true);
  });

  it("rejects rating above 5", () => {
    const result = feedbackSchema.safeParse({
      grievanceId: 1,
      question1: 6,
      question2: 5,
      question3: 5,
      question4: 5,
      question5: 5,
    });
    expect(result.success).toBe(false);
  });

  it("rejects rating below 1", () => {
    const result = feedbackSchema.safeParse({
      grievanceId: 1,
      question1: 0,
      question2: 5,
      question3: 5,
      question4: 5,
      question5: 5,
    });
    expect(result.success).toBe(false);
  });

  it("allows optional free text and email", () => {
    const result = feedbackSchema.safeParse({
      grievanceId: 1,
      question1: 4,
      question2: 4,
      question3: 4,
      question4: 4,
      question5: 4,
      freeTextFeedback: "Great service!",
      email: "test@example.com",
    });
    expect(result.success).toBe(true);
  });

  it("rejects invalid email", () => {
    const result = feedbackSchema.safeParse({
      grievanceId: 1,
      question1: 4,
      question2: 4,
      question3: 4,
      question4: 4,
      question5: 4,
      email: "not-an-email",
    });
    expect(result.success).toBe(false);
  });
});

describe("statusChangeSchema", () => {
  it("accepts valid status values", () => {
    for (const s of [1, 2, 3, 4]) {
      const result = statusChangeSchema.safeParse({ issueStatus: s, assignToCreator: false });
      expect(result.success).toBe(true);
    }
  });

  it("rejects status outside 1-4", () => {
    const result = statusChangeSchema.safeParse({ issueStatus: 5 });
    expect(result.success).toBe(false);
  });

  it("defaults assignToCreator to false", () => {
    const result = statusChangeSchema.safeParse({ issueStatus: 2 });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.assignToCreator).toBe(false);
    }
  });
});

describe("constants", () => {
  it("ISSUE_STATUS maps all 4 codes", () => {
    expect(ISSUE_STATUS[1]).toBe("NEW");
    expect(ISSUE_STATUS[2]).toBe("ASSIGNED");
    expect(ISSUE_STATUS[3]).toBe("RESOLVED");
    expect(ISSUE_STATUS[4]).toBe("ASSIGN_TO_CREATOR");
  });

  it("CLOSE_STATUS maps all 3 codes", () => {
    expect(CLOSE_STATUS[1]).toBe("PENDING");
    expect(CLOSE_STATUS[2]).toBe("COMPLETED");
    expect(CLOSE_STATUS[3]).toBe("CLOSED");
  });

  it("SEVERITY_CODE has 4 levels", () => {
    expect(Object.keys(SEVERITY_CODE)).toEqual(["LOW", "MEDIUM", "HIGH", "CRITICAL"]);
  });

  it("FEEDBACK_QUESTIONS has 5 questions", () => {
    expect(FEEDBACK_QUESTIONS).toHaveLength(5);
  });
});
