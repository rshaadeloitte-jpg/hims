import { useMemo } from "react";
import type { QueueToken } from "../queries";

/**
 * Compute estimated wait time from token timestamps.
 * Uses average service duration × number of waiting tokens ahead.
 */
export function computeWaitTime(
  tokens: QueueToken[],
  _now: Date = new Date(),
): { avgServiceMinutes: number; waitingAhead: number; estimatedMinutes: number } {
  const completed = tokens.filter((t) => t.status === 3 && t.calledAt && t.completedAt);
  const waiting = tokens.filter((t) => t.status === 1);

  let avgServiceMinutes = 8; // default assumption: 8 min per patient

  if (completed.length > 0) {
    const totalMs = completed.reduce((sum, t) => {
      const start = new Date(t.calledAt!).getTime();
      const end = new Date(t.completedAt!).getTime();
      return sum + Math.max(0, end - start);
    }, 0);
    avgServiceMinutes = totalMs / completed.length / 60000;
    // Clamp to reasonable range
    avgServiceMinutes = Math.max(2, Math.min(45, avgServiceMinutes));
  }

  const waitingAhead = waiting.length;
  const estimatedMinutes = Math.round(waitingAhead * avgServiceMinutes);

  return { avgServiceMinutes: Math.round(avgServiceMinutes * 10) / 10, waitingAhead, estimatedMinutes };
}

/**
 * Hook wrapper around computeWaitTime for use inside components.
 */
export function useWaitTimeCalc(tokens: QueueToken[]) {
  return useMemo(() => computeWaitTime(tokens), [tokens]);
}

/**
 * Format minutes into a human-readable string.
 */
export function formatWaitTime(minutes: number): string {
  if (minutes <= 0) return "No wait";
  if (minutes < 60) return `~${minutes} min`;
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m > 0 ? `~${h}h ${m}m` : `~${h}h`;
}