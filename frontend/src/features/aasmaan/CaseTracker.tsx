import { useState, useCallback } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useCases,
  useCreateCase,
  useUpdateCase,
  useDeleteCase,
  type AasmaanCase,
} from "./queries";
import { caseSchema, type CaseValues, type CaseFilterValues } from "./zodSchema";

const STATUS_MAP: Record<number, { label: string; className: string }> = {
  1: { label: "OPEN", className: "bg-blue-100 text-blue-800" },
  2: { label: "IN PROGRESS", className: "bg-yellow-100 text-yellow-800" },
  3: { label: "CLOSED", className: "bg-green-100 text-green-800" },
};

interface CaseTrackerProps {
  onCaseSelect?: (caseId: number) => void;
}

/**
 * CaseTracker — Linear-style field case CRUD dashboard.
 * Left: filterable case list with status badges. Right: create/edit form.
 */
export function CaseTracker({ onCaseSelect }: CaseTrackerProps) {
  const [filter, setFilter] = useState<CaseFilterValues>({ page: 0, size: 20 });
  const [editingCase, setEditingCase] = useState<AasmaanCase | null>(null);
  const [showForm, setShowForm] = useState(false);

  const { data: cases = [], isLoading } = useCases(filter);
  const createCase = useCreateCase();
  const updateCase = useUpdateCase();
  const deleteCase = useDeleteCase();

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CaseValues>({
    resolver: zodResolver(caseSchema),
    defaultValues: {
      status: 1,
      caseType: "",
      patientId: undefined as unknown as number,
    },
  });

  const handleNew = useCallback(() => {
    setEditingCase(null);
    reset({ status: 1, caseType: "", patientId: undefined as unknown as number, openedOn: "", closedOn: "" });
    setShowForm(true);
  }, [reset]);

  const handleEdit = useCallback(
    (c: AasmaanCase) => {
      setEditingCase(c);
      reset({
        patientId: c.patientId,
        caseType: c.caseType,
        status: c.status,
        openedOn: c.openedOn ?? "",
        closedOn: c.closedOn ?? "",
      });
      setShowForm(true);
    },
    [reset],
  );

  const handleDelete = useCallback(
    async (id: number) => {
      if (!confirm("Delete this case? This cannot be undone.")) return;
      try {
        await deleteCase.mutateAsync(id);
      } catch (err) {
        console.error("Delete failed:", err);
      }
    },
    [deleteCase],
  );

  const onSubmit = async (values: CaseValues) => {
    try {
      if (editingCase) {
        await updateCase.mutateAsync({ id: editingCase.caseId, payload: values });
      } else {
        await createCase.mutateAsync(values);
      }
      setShowForm(false);
      setEditingCase(null);
      reset();
    } catch (err) {
      console.error("Save failed:", err);
    }
  };

  const handleFilterChange = (key: keyof CaseFilterValues, value: string) => {
    setFilter((prev) => ({
      ...prev,
      [key]: value === "" ? undefined : value,
      page: 0,
    }));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Field Cases</h2>
          <p className="text-sm text-gray-500">Track and manage field worker cases</p>
        </div>
        <button
          type="button"
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
          onClick={handleNew}
        >
          New Case
        </button>
      </header>

      {/* Filters */}
      <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border border-gray-200">
        <div className="flex-1">
          <label htmlFor="filter-status" className="sr-only">Status</label>
          <select
            id="filter-status"
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm"
            value={filter.status ?? ""}
            onChange={(e) => handleFilterChange("status", e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="1">Open</option>
            <option value="2">In Progress</option>
            <option value="3">Closed</option>
          </select>
        </div>
        <div className="flex-1">
          <label htmlFor="filter-type" className="sr-only">Case Type</label>
          <input
            id="filter-type"
            type="text"
            placeholder="Filter by type..."
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm"
            value={filter.caseType ?? ""}
            onChange={(e) => handleFilterChange("caseType", e.target.value)}
          />
        </div>
        <div className="flex-1">
          <label htmlFor="filter-patient" className="sr-only">Patient ID</label>
          <input
            id="filter-patient"
            type="number"
            placeholder="Patient ID"
            className="w-full px-3 py-1.5 border border-gray-300 rounded-md text-sm"
            value={filter.patientId ?? ""}
            onChange={(e) => handleFilterChange("patientId", e.target.value)}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Case list */}
        <div className="lg:col-span-2 border border-gray-200 rounded-lg overflow-hidden">
          <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
            <h3 className="text-sm font-medium text-gray-900">
              Cases ({cases.length})
            </h3>
          </div>
          {isLoading ? (
            <div className="p-8 text-center text-gray-500">Loading cases...</div>
          ) : cases.length === 0 ? (
            <div className="p-8 text-center text-gray-500">No cases found</div>
          ) : (
            <div className="divide-y divide-gray-200">
              {cases.map((c) => {
                const statusInfo = STATUS_MAP[c.status] ?? { label: "UNKNOWN", className: "bg-gray-100 text-gray-800" };
                return (
                  <div
                    key={c.caseId}
                    className="p-4 hover:bg-gray-50 cursor-pointer transition-colors"
                    onClick={() => onCaseSelect?.(c.caseId)}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-mono text-gray-500">#{c.caseId}</span>
                        <span className="text-sm font-medium text-gray-900">{c.caseType}</span>
                      </div>
                      <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${statusInfo.className}`}>
                        {statusInfo.label}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-gray-500">
                      <span>Patient: {c.patientId}</span>
                      <span>Opened: {c.openedOn ?? "—"}</span>
                    </div>
                    <div className="mt-2 flex items-center gap-2">
                      <button
                        type="button"
                        className="text-xs text-blue-600 hover:text-blue-800"
                        onClick={(e) => { e.stopPropagation(); handleEdit(c); }}
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="text-xs text-red-600 hover:text-red-800"
                        onClick={(e) => { e.stopPropagation(); handleDelete(c.caseId); }}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Create/Edit form */}
        {showForm && (
          <div className="border border-gray-200 rounded-lg p-4">
            <h3 className="text-sm font-medium text-gray-900 mb-4">
              {editingCase ? `Edit Case #${editingCase.caseId}` : "New Case"}
            </h3>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div>
                <label htmlFor="patientId" className="block text-xs font-medium text-gray-700 mb-1">
                  Patient ID *
                </label>
                <input
                  id="patientId"
                  type="number"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...register("patientId")}
                />
                {errors.patientId && (
                  <p className="text-xs text-red-600 mt-1">{errors.patientId.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="caseType" className="block text-xs font-medium text-gray-700 mb-1">
                  Case Type *
                </label>
                <input
                  id="caseType"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  placeholder="e.g. ANC, PNC, GENERAL"
                  {...register("caseType")}
                />
                {errors.caseType && (
                  <p className="text-xs text-red-600 mt-1">{errors.caseType.message}</p>
                )}
              </div>

              <div>
                <label htmlFor="status" className="block text-xs font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  id="status"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...register("status")}
                >
                  <option value={1}>Open</option>
                  <option value={2}>In Progress</option>
                  <option value={3}>Closed</option>
                </select>
              </div>

              <div>
                <label htmlFor="openedOn" className="block text-xs font-medium text-gray-700 mb-1">
                  Opened On
                </label>
                <input
                  id="openedOn"
                  type="date"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...register("openedOn")}
                />
              </div>

              <div>
                <label htmlFor="closedOn" className="block text-xs font-medium text-gray-700 mb-1">
                  Closed On
                </label>
                <input
                  id="closedOn"
                  type="date"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                  {...register("closedOn")}
                />
              </div>

              <div className="flex items-center gap-2 pt-2">
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 text-sm"
                  disabled={createCase.isPending || updateCase.isPending}
                >
                  {createCase.isPending || updateCase.isPending
                    ? "Saving..."
                    : editingCase
                      ? "Update Case"
                      : "Create Case"}
                </button>
                <button
                  type="button"
                  className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-50 text-sm"
                  onClick={() => { setShowForm(false); setEditingCase(null); }}
                >
                  Cancel
                </button>
            </div>

            {(createCase.isError || updateCase.isError) && (
              <div className="mt-3 p-2 bg-red-50 border border-red-200 rounded text-xs text-red-700">
                Save failed. Please try again.
              </div>
            )}
          </form>
        </div>
        )}

        {!showForm && (
          <div className="border border-dashed border-gray-300 rounded-lg p-8 text-center">
            <p className="text-sm text-gray-500">Select a case to edit, or click "New Case" to create one.</p>
          </div>
        )}
      </div>
    </div>
  );
}
