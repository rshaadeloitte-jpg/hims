import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ApiError } from "../../api/client";
import {
  type AppConfigEntry,
  useAppConfigList,
  useSetAppConfig,
} from "./queries";
import { appConfigSchema, type AppConfigValues } from "./zodSchema";

/**
 * AppConfig — per-app / per-unit key-value configuration editor.
 *
 * Linear audit-log pattern: every PUT to /commons/config is also recorded
 * as an audit entry (entityType="AppConfig", action=UPDATE/CREATE) by the
 * backend CommonsService, giving a full change trail without frontend work.
 */
export function AppConfigPage() {
  const [unitIdInput, setUnitIdInput] = useState<string>("");
  const unitId =
    unitIdInput.trim() === ""
      ? undefined
      : Number.isNaN(Number(unitIdInput))
        ? undefined
        : Number(unitIdInput);

  const listQ = useAppConfigList(unitId);
  const setMut = useSetAppConfig(unitId);

  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState<AppConfigEntry | null>(null);

  const configs = listQ.data ?? [];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Application Configuration</h1>
          <p className="muted">
            Per-unit key-value store · scope with a unit ID or leave blank for global
          </p>
        </div>
        <button
          className="btn btn-primary"
          onClick={() => {
            setEditing(null);
            setShowForm(true);
          }}
        >
          + New Config
        </button>
      </header>

      <section className="card toolbar" aria-label="Scope">
        <div className="toolbar-row">
          <div className="field">
            <label htmlFor="unit-scope">Unit ID</label>
            <input
              id="unit-scope"
              type="number"
              min={1}
              value={unitIdInput}
              onChange={(e) => setUnitIdInput(e.target.value)}
              placeholder="blank = global"
            />
          </div>
        </div>
      </section>

      {listQ.isError ? (
        <div className="alert alert--error" role="alert">
          <strong>
            {listQ.error instanceof ApiError
              ? `${listQ.error.code} (HTTP ${listQ.error.status})`
              : "Error"}
          </strong>
          :{" "}
          {listQ.error instanceof ApiError
            ? listQ.error.message
            : String(listQ.error?.message ?? listQ.error)}
        </div>
      ) : null}

      {setMut.isError ? (
        <div className="alert alert--error" role="alert">
          <strong>
            {setMut.error instanceof ApiError
              ? `${setMut.error.code} (HTTP ${setMut.error.status})`
              : "Save failed"}
          </strong>
          :{" "}
          {setMut.error instanceof ApiError
            ? setMut.error.message
            : String(setMut.error?.message ?? setMut.error)}
        </div>
      ) : null}

      {showForm ? (
        <ConfigForm
          editing={editing}
          submitting={setMut.isPending}
          onSubmit={(values) => {
            setMut.mutate(
              {
                key: values.key,
                value: values.value || undefined,
                description: values.description,
              },
              {
                onSuccess: () => {
                  setShowForm(false);
                  setEditing(null);
                },
              },
            );
          }}
          onCancel={() => {
            setShowForm(false);
            setEditing(null);
          }}
        />
      ) : null}

      <section className="card">
        {listQ.isLoading ? (
          <div className="loading-state" role="status">
            <span className="spinner" aria-hidden="true" /> Loading configs…
          </div>
        ) : configs.length === 0 ? (
          <p className="empty-state">
            No configuration entries
            {unitId != null ? ` for unit ${unitId}` : " (global scope)"}.
          </p>
        ) : (
          <div className="table-wrap">
            <table className="data-table" aria-label="Application configurations">
              <thead>
                <tr>
                  <th>Key</th>
                  <th>Value</th>
                  <th>Description</th>
                  <th>Unit</th>
                  <th>Updated</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {configs.map((cfg) => (
                  <tr key={`${cfg.unitId ?? "global"}-${cfg.configKey}`}>
                    <td>
                      <code>{cfg.configKey}</code>
                    </td>
                    <td className="cell" style={{ maxWidth: 280 }}>
                      {cfg.configValue ?? <span className="muted">—</span>}
                    </td>
                    <td className="muted">{cfg.description ?? "—"}</td>
                    <td>{cfg.unitId ?? <span className="muted">global</span>}</td>
                    <td className="muted">
                      {cfg.updateddatetime?.slice(0, 19) ??
                        cfg.addeddatetime?.slice(0, 19) ??
                        "—"}
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => {
                          setEditing(cfg);
                          setShowForm(true);
                        }}
                      >
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

// ---- Inline form ---------------------------------------------------------

interface ConfigFormProps {
  editing: AppConfigEntry | null;
  submitting: boolean;
  onSubmit: (values: AppConfigValues) => void;
  onCancel: () => void;
}

function ConfigForm({ editing, submitting, onSubmit, onCancel }: ConfigFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AppConfigValues>({
    resolver: zodResolver(appConfigSchema),
    defaultValues: {
      key: editing?.configKey ?? "",
      value: editing?.configValue ?? "",
      description: editing?.description ?? "",
    },
  });

  return (
    <section className="card">
      <h2>{editing ? `Edit: ${editing.configKey}` : "New Configuration"}</h2>
      <form
        className="master-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <div className="form-grid">
          <div className={`field ${errors.key ? "field--error" : ""}`}>
            <label htmlFor="cfg-key">
              Key <span className="req" aria-hidden="true">*</span>
            </label>
            <input
              id="cfg-key"
              type="text"
              placeholder="app.feature.enabled"
              disabled={editing !== null}
              aria-invalid={errors.key ? true : undefined}
              {...register("key")}
            />
            {errors.key ? (
              <p className="field-error" role="alert">
                {errors.key.message}
              </p>
            ) : null}
          </div>

          <div className={`field ${errors.value ? "field--error" : ""}`}>
            <label htmlFor="cfg-value">Value</label>
            <input
              id="cfg-value"
              type="text"
              placeholder="true"
              aria-invalid={errors.value ? true : undefined}
              {...register("value")}
            />
            {errors.value ? (
              <p className="field-error" role="alert">
                {errors.value.message}
              </p>
            ) : null}
          </div>

          <div className={`field ${errors.description ? "field--error" : ""}`}>
            <label htmlFor="cfg-desc">Description</label>
            <input
              id="cfg-desc"
              type="text"
              placeholder="Feature flag for the new dashboard"
              aria-invalid={errors.description ? true : undefined}
              {...register("description")}
            />
            {errors.description ? (
              <p className="field-error" role="alert">
                {errors.description.message}
              </p>
            ) : null}
          </div>
        </div>

        <div className="form-actions">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onCancel}
            disabled={submitting}
          >
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? "Saving…" : editing ? "Save Changes" : "Create"}
          </button>
        </div>
      </form>
    </section>
  );
}
