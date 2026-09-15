import { describe, expect, it } from "vitest";
import { computeWaitTime, formatWaitTime } from "../hooks/useWaitTime";
import type { QueueToken } from "../queries";

function makeToken(overrides: Partial<QueueToken> = {}): QueueToken {
  return {
    tokenId: 1,
    counterId: 1,
    patientId: 1,
    tokenNo: 1,
    priority: "NORMAL",
    status: 1,
    createdAt: "2027-01-01T09:00:00Z",
    ...overrides,
  };
}

describe("computeWaitTime", () => {
  it("returns 0 wait for empty queue", () => {
    const result = computeWaitTime([]);
    expect(result.waitingAhead).toBe(0);
    expect(result.estimatedMinutes).toBe(0);
  });

  it("estimates based on default 8min when no completed tokens", () => {
    const tokens = [
      makeToken({ tokenId: 1, status: 1 }),
      makeToken({ tokenId: 2, status: 1 }),
      makeToken({ tokenId: 3, status: 1 }),
    ];
    const result = computeWaitTime(tokens);
    expect(result.waitingAhead).toBe(3);
    expect(result.estimatedMinutes).toBe(24); // 3 × 8
  });

  it("uses actual service duration from completed tokens", () => {
    const tokens = [
      makeToken({
        tokenId: 1, status: 3,
        calledAt: "2027-01-01T09:00:00Z",
        completedAt: "2027-01-01T09:10:00Z", // 10 min
      }),
      makeToken({ tokenId: 2, status: 1 }),
      makeToken({ tokenId: 3, status: 1 }),
    ];
    const result = computeWaitTime(tokens);
    expect(result.avgServiceMinutes).toBe(10);
    expect(result.waitingAhead).toBe(2);
    expect(result.estimatedMinutes).toBe(20);
  });

  it("clamps average to min 2 minutes", () => {
    const tokens = [
      makeToken({
        tokenId: 1, status: 3,
        calledAt: "2027-01-01T09:00:00Z",
        completedAt: "2027-01-01T09:00:30Z", // 0.5 min
      }),
      makeToken({ tokenId: 2, status: 1 }),
    ];
    const result = computeWaitTime(tokens);
    expect(result.avgServiceMinutes).toBe(2);
  });

  it("ignores non-completed tokens in average calculation", () => {
    const tokens = [
      makeToken({ tokenId: 1, status: 1 }), // waiting — not counted
      makeToken({ tokenId: 2, status: 2 }), // called — not counted
      makeToken({
        tokenId: 3, status: 3,
        calledAt: "2027-01-01T09:00:00Z",
        completedAt: "2027-01-01T09:15:00Z", // 15 min
      }),
    ];
    const result = computeWaitTime(tokens);
    expect(result.avgServiceMinutes).toBe(15);
    expect(result.waitingAhead).toBe(1);
    expect(result.estimatedMinutes).toBe(15);
  });
});

describe("formatWaitTime", () => {
  it("returns 'No wait' for 0 minutes", () => {
    expect(formatWaitTime(0)).toBe("No wait");
  });

  it("formats minutes under 60", () => {
    expect(formatWaitTime(25)).toBe("~25 min");
  });

  it("formats hours and minutes", () => {
    expect(formatWaitTime(90)).toBe("~1h 30m");
  });

  it("formats exact hours", () => {
    expect(formatWaitTime(120)).toBe("~2h");
  });
});