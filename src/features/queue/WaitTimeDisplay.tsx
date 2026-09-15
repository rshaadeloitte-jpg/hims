import { useWaitTime } from "./queries";
import { useWaitTimeCalc, formatWaitTime } from "./hooks";
import type { QueueToken } from "./queries";

interface WaitTimeDisplayProps {
  /** Counter to show wait time for (uses API). */
  counterId?: number;
  /** Or pass tokens directly for client-side calculation. */
  tokens?: QueueToken[];
  /** Label override. */
  label?: string;
}

function waitClass(minutes: number): string {
  if (minutes <= 10) return "q-wait--short";
  if (minutes <= 30) return "q-wait--medium";
  return "q-wait--long";
}

/**
 * M27 WaitTimeDisplay — estimated wait time with color-coded severity.
 * Can pull from the API (counterId) or compute client-side from tokens.
 */
export function WaitTimeDisplay({ counterId, tokens, label = "Estimated Wait" }: WaitTimeDisplayProps) {
  // API-driven mode
  const { data: apiEstimate } = useWaitTime(counterId ?? 0);

  // Client-side mode
  const clientEstimate = useWaitTimeCalc(tokens ?? []);

  const minutes = apiEstimate?.estimatedWaitMinutes ?? (tokens ? clientEstimate.estimatedMinutes : null);
  const waiting = apiEstimate?.currentWaiting ?? (tokens ? clientEstimate.waitingAhead : null);

  if (minutes === null) {
    return (
      <div className="q-wait">
        <span className="q-wait__value">—</span>
        <span className="q-wait__label">{label}</span>
      </div>
    );
  }

  return (
    <div className={`q-wait ${waitClass(minutes)}`}>
      <span className="q-wait__value">{formatWaitTime(minutes)}</span>
      <span className="q-wait__label">{label}</span>
      {waiting !== null && (
        <span style={{ fontSize: "var(--font-size-xs)", color: "var(--color-text-muted)" }}>
          {waiting} patient{waiting !== 1 ? "s" : ""} ahead
        </span>
      )}
    </div>
  );
}