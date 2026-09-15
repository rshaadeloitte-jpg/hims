import { useMemo, useState } from "react";
import { ApiError } from "../../api/client";
import { useAuditByEntity, useAuditByActor, type AuditLogEntry, type AuditDiffPreview } from "./queries";
import { parseDiff } from "./zodSchema";

/**
 * AuditTrail — append-only audit log viewer with JSONB diff rendering.
 *
 * Linear audit log patterns:
 *   - Chronological feed (newest first), immutable entries
 *   - Entity-scoped (by entityType + entityId) or actor-scoped (by actorId)
 *   - JSONB diff rendered as a structured change-set (field → old/new)
 *   - Action badges with semantic color (CREATE=green, UPDATE=blue, DELETE=red)
 *   - Monospace timestamps, actor attribution, expandable diff detail
 *
 * Data source: reference-service /commons/audit endpoints (append-only table).
 */
export function AuditTrailPage() {
  const [mode, setMode] = useState<"entity" | "actor">("entity");
  const [entityType, setEntityType] = useState("");
  const [entityId, setEntityId] = useState("");
  const [actorId, setActorId] = useState("");

  const eid = Number(entityId);
  const aid = Number(actorId);

  const byEntityQ = useAuditByEntity(
    mode === "entity" ? entityType : "",
    mode === "entity" ? eid : 0,
  );
  const byActorQ = useAuditByActor(mode === "actor" ? aid : 0);

  const isLoading = mode === "entity" ? byEntityQ.isLoading : byActorQ.isLoading;
  const isError = mode === "entity" ? byEntityQ.isError : byActorQ.isError;
  const error = mode === "entity" ? byEntityQ.error : byActorQ.error;
  const entries = mode === "entity" ? byEntityQ.data ?? [] : byActorQ.data ?? [];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Audit Trail</h1>
          <p className="muted">
            Append-only log with JSONB diffs · Linear-style chronological feed
          </p>
        </div>
      </header>

      <section className="card toolbar" aria-label="Scope">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="scope-mode">Scope</label>
            <select
              id="scope-mode"
              value={mode}
              onChange={(e) => setMode(e.target.value as "entity" | "actor")}
            >
              <option value="entity">By Entity</option>
              <option value="actor">By Actor</option>
            </select>
          </div>

          {mode === "entity" ? (
            <>
              <div className="field">
                <label htmlFor="entity-type">Entity Type</label>
                <input
                  id="entity-type"
                  type="text"
                  value={entityType}
                  onChange={(e) => setEntityType(e.target.value)}
                  placeholder="Patient, AppConfig, …"
                />
              </div>
              <div className="field">
                <label htmlFor="entity-id">Entity ID</label>
                <input
                  id="entity-id"
                  type="number"
                  min={1}
                  value={entityId}
                  onChange={(e) => setEntityId(e.target.value)}
                />
              </div>
            </>
          ) : (
            <div className="field">
              <label htmlFor="actor-id">Actor ID</label>
              <input
                id="actor-id"
                type="number"
                min={1}
                value={actorId}
                onChange={(e) => setActorId(e.target.value)}
              />
            </div>
          )}
        </div>
      </section>

      {isError ? (
        <div className="alert alert--error" role="alert">
          <strong>
            {error instanceof ApiError ? `${error.code} (HTTP ${error.status})` : "Error"}
          </strong>
          :{" "}
          {error instanceof ApiError ? error.message : String(error?.message ?? error)}
        </div>
      ) : null}

      <section className="card">
        {isLoading ? (
          <div className="loading-state" role="status">
            <span className="spinner" aria-hidden="true" /> Loading audit log…
          </div>
        ) : entries.length === 0 ? (
          <p className="empty-state">
            {mode === "entity"
              ? "Enter an entity type and ID to view its audit trail."
              : "Enter an actor ID to view their actions."}
          </p>
        ) : (
          <ul className="audit-feed" aria-label="Audit entries">
            {entries.map((entry) => (
              <AuditEntryRow key={entry.id ?? `${entry.entityType}-${entry.entityId}-${entry.occurredAt}`} entry={entry} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

// ---- Feed entry ---------------------------------------------------------

function AuditEntryRow({ entry }: { entry: AuditLogEntry }) {
  const [expanded, setExpanded] = useState(false);
  const diff = parseDiff(entry.diff ?? null);

  const diffRows: AuditDiffPreview[] = useMemo(() => {
    if (!diff) return [];
    return Object.entries(diff).map(([key, value]) => {
      if (value && typeof value === "object" && "old" in value && "new" in value) {
        const v = value as { old?: unknown; new?: unknown };
        return {
          key,
          oldValue: v.old === undefined ? undefined : String(v.old),
          newValue: v.new === undefined ? undefined : String(v.new),
        };
      }
      return { key, newValue: String(value) };
    });
  }, [diff]);

  return (
    <li className="audit-entry">
      <div className="audit-entry__header">
        <span className={`badge action-${entry.action.toLowerCase()}`}>
          {entry.action}
        </span>
        <span className="audit-entry__entity">
          {entry.entityType} #{entry.entityId}
        </span>
        <span className="audit-entry__time">
          {entry.occurredAt?.replace("T", " ").slice(0, 19) ?? "—"}
        </span>
        <span className="audit-entry__actor muted">
          by {entry.actorUsername ?? entry.actorId ?? "system"}
        </span>
        {diff && diffRows.length > 0 ? (
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setExpanded((v) => !v)}
            aria-expanded={expanded}
          >
            {expanded ? "Hide diff" : `Show ${diffRows.length} change${diffRows.length === 1 ? "" : "s"}`}
          </button>
        ) : null}
      </div>

      {expanded && diffRows.length > 0 ? (
        <div className="audit-entry__diff">
          <table className="diff-table">
            <thead>
              <tr>
                <th>Field</th>
                <th>Old</th>
                <th>New</th>
              </tr>
            </thead>
            <tbody>
              {diffRows.map((row) => (
                <tr key={row.key}>
                  <td>
                    <code>{row.key}</code>
                  </td>
                  <td className="diff-old">{row.oldValue ?? <span className="muted">—</span>}</td>
                  <td className="diff-new">{row.newValue ?? <span className="muted">—</span>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </li>
  );
}
