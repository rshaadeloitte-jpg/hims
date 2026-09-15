import { useState } from "react";
import { useCallNext, useCallPatient } from "./queries";
import type { QueueToken } from "./queries";

interface CallPatientProps {
  /** Counter to call from. */
  counterId: number;
  /** Specific token to call (optional — if omitted, calls next in queue). */
  token?: QueueToken;
  /** Called after successful call action. */
  onCalled?: (token: QueueToken) => void;
}

/**
 * M27 CallPatient — call button with visual/audio feedback states.
 * Supports both "call next" (counter-level) and "call specific token".
 */
export function CallPatient({ counterId, token, onCalled }: CallPatientProps) {
  const callNext = useCallNext(counterId);
  const callSpecific = useCallPatient();
  const [callState, setCallState] = useState<"idle" | "calling" | "called">("idle");

  const handleCall = async () => {
    setCallState("calling");
    try {
      let result: QueueToken;
      if (token) {
        result = await callSpecific.mutateAsync(token.tokenId);
      } else {
        result = await callNext.mutateAsync();
      }
      setCallState("called");
      onCalled?.(result);
      // Reset after 3s
      setTimeout(() => setCallState("idle"), 3000);
    } catch {
      setCallState("idle");
    }
  };

  const isPending = callNext.isPending || callSpecific.isPending;
  const disabled = isPending || (callState === "called");

  const label =
    callState === "calling"
      ? "Calling…"
      : callState === "called"
        ? `Called #${token?.tokenNo ?? ""}`
        : token
          ? `Call #${token.tokenNo}`
          : "Call Next Patient";

  return (
    <button
      className={`q-call-btn ${callState === "calling" ? "q-call-btn--primary q-call-btn--calling" : callState === "called" ? "q-call-btn--outline" : "q-call-btn--primary"}`}
      onClick={handleCall}
      disabled={disabled}
      aria-label={token ? `Call patient token ${token.tokenNo}` : "Call next patient"}
    >
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <path
          d="M13.5 8.5c0-3-2.5-5.5-5.5-5.5S2.5 5.5 2.5 8.5c0 1.2.4 2.3 1 3.2L3 14l2.5-1c.8.3 1.6.5 2.5.5 3 0 5.5-2.5 5.5-5.5z"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
      {label}
    </button>
  );
}