import { useCallback, useEffect, useRef, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { QueueEvent } from "../queries";

export type ConnectionStatus = "connecting" | "connected" | "disconnected" | "error";

interface UseQueueRealtimeOptions {
  /** SSE endpoint; falls back to polling when unavailable. */
  sseUrl?: string;
  /** Polling interval in ms when SSE is not used. Default 5000. */
  pollInterval?: number;
  /** Whether to enable SSE. Default false (polling mode). */
  useSSE?: boolean;
}

/**
 * Real-time queue updates — Linear-style live board.
 *
 * Two modes:
 * 1. SSE (Server-Sent Events) when the backend supports /api/v1/queue/events/stream.
 * 2. Polling via TanStack Query refetchInterval (configured per-hook in queries.ts).
 *
 * Returns connection status for the Vercel-style indicator dot.
 */
export function useQueueRealtime(options: UseQueueRealtimeOptions = {}) {
  const { sseUrl, useSSE = false } = options;
  const qc = useQueryClient();
  const [status, setStatus] = useState<ConnectionStatus>("connecting");
  const [lastEvent, setLastEvent] = useState<QueueEvent | null>(null);
  const esRef = useRef<EventSource | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout>>();

  const invalidateAll = useCallback(() => {
    void qc.invalidateQueries({ queryKey: ["queue"] });
  }, [qc]);

  const handleEvent = useCallback(
    (raw: MessageEvent) => {
      try {
        const evt = JSON.parse(raw.data) as QueueEvent;
        setLastEvent(evt);
        invalidateAll();
      } catch {
        // malformed SSE payload — ignore
      }
    },
    [invalidateAll],
  );

  const connect = useCallback(() => {
    if (!useSSE || !sseUrl) {
      setStatus("connected"); // polling mode — always "connected"
      return;
    }

    setStatus("connecting");
    const es = new EventSource(sseUrl);
    esRef.current = es;

    es.onopen = () => setStatus("connected");

    es.onmessage = handleEvent;

    es.onerror = () => {
      setStatus("error");
      es.close();
      esRef.current = null;
      // Reconnect after 5s
      reconnectTimer.current = setTimeout(() => {
        setStatus("connecting");
        connect();
      }, 5000);
    };
  }, [useSSE, sseUrl, handleEvent]);

  useEffect(() => {
    connect();
    return () => {
      esRef.current?.close();
      esRef.current = null;
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
    };
  }, [connect]);

  return { status, lastEvent, reconnect: connect };
}