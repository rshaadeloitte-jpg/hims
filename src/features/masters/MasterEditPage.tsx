import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { ApiError } from "../../api/client";
import { getEntityMeta } from "./entityMeta";
import { formatCellValue } from "./format";
import { MasterForm } from "./components/MasterForm";
import { useMasterDetail, useSaveMaster } from "./queries";

/**
 * MasterEditPage — create (/masters/:entity/new) and edit (/masters/:entity/:id).
 *
 * The form is generated from EntityMeta (React Hook Form + zodResolver); save
 * uses PUT (edit) / POST (create) (MIG-010 W1 write surface) and invalidates
 * the list/detail caches on success.
 *
 * Unit scope (red-team F-6): the list page carries the record's owning unitId
 * as ?unitId=... (refresh-safe). It is required on GET-by-id and PUT for
 * unit-scoped entities (departmentmaster/companymaster); Unitmaster is id-only.
 */
export function MasterEditPage() {
  const { entity = "", id = "" } = useParams<{ entity: string; id: string }>();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isCreate = id === "new";

  const unitIdParam = searchParams.get("unitId");
  const unitId =
    unitIdParam !== null && !Number.isNaN(Number(unitIdParam)) ? Number(unitIdParam) : undefined;

  const meta = getEntityMeta(entity);
  const detailQuery = useMasterDetail(entity, id, isCreate ? undefined : unitId);
  const save = useSaveMaster(entity, id, unitId);
  const submitting = save.isPending;

  const backToList = () => navigate(`/masters/${entity}`);

  if (!meta) {
    return (
      <div className="alert alert--error" role="alert">
        Unknown entity: {entity}
      </div>
    );
  }

  if (!isCreate && detailQuery.isLoading) {
    return (
      <div className="loading-state" role="status">
        <span className="spinner" aria-hidden="true" /> Loading {meta.label} #{id}…
      </div>
    );
  }

  if (!isCreate && detailQuery.isError) {
    const err = detailQuery.error;
    return (
      <div className="alert alert--error" role="alert">
        <strong>{err instanceof ApiError ? `${err.code} (HTTP ${err.status})` : "Error"}</strong>:{" "}
        {err instanceof ApiError ? err.message : String(err.message ?? err)}
        <button type="button" className="btn btn-secondary btn-sm" onClick={() => detailQuery.refetch()}>
          Retry
        </button>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>
            {isCreate ? `New ${meta.label}` : `Edit ${meta.label}`}
          </h1>
          <p className="muted">
            {meta.name} · {isCreate ? "create mode (POST)" : `ID ${id} (PUT)`}
            {unitId !== undefined ? ` · unitId ${unitId}` : ""}
          </p>
        </div>
        <button type="button" className="btn btn-secondary" onClick={backToList}>
          ← Back to list
        </button>
      </header>

      {save.isError ? (
        <div className="alert alert--error" role="alert">
          <strong>{save.error instanceof ApiError ? `${save.error.code} (HTTP ${save.error.status})` : "Save failed"}</strong>:{" "}
          {save.error instanceof ApiError ? save.error.message : String(save.error?.message ?? save.error)}
        </div>
      ) : null}

      <div className="card">
        <MasterForm
          meta={meta}
          mode={isCreate ? "create" : "edit"}
          record={isCreate ? (unitId ? { unitId } : null) : detailQuery.data}
          submitting={submitting}
          onSubmit={(values) => {
            save.mutate(values, {
              onSuccess: () => backToList(),
            });
          }}
          onCancel={backToList}
        />
      </div>

      {!isCreate && detailQuery.data ? (
        <div className="card card--readonly">
          <h2>System fields</h2>
          <dl className="readonly-list">
            {meta.fields
              .filter((f) => f.readOnly)
              .map((f) => (
                <div key={f.name} className="readonly-item">
                  <dt>{f.label}</dt>
                  <dd>{formatCellValue(detailQuery.data?.[f.name], f)}</dd>
                </div>
              ))}
          </dl>
        </div>
      ) : null}
    </div>
  );
}
