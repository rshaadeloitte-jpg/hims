import { useState } from "react";
import {
  useEquipmentList,
  useRegisterEquipment,
  useUpdateEquipment,
  useTransferEquipment,
  Equipment,
} from "./queries";
import {
  equipmentSchema,
  equipmentUpdateSchema,
  equipmentTransferSchema,
  type EquipmentForm,
  type EquipmentTransferForm,
} from "./zodSchema";

const WORKING_STATUS_OPTIONS = [
  { value: 1, label: "Working" },
  { value: 2, label: "Not Working" },
  { value: 3, label: "Under Repair" },
  { value: 4, label: "Condemned" },
];

const STATUS_BADGE: Record<number, string> = {
  1: "badge-success",
  2: "badge-danger",
  3: "badge-warning",
  4: "badge-muted",
};

const emptyForm: EquipmentForm = {
  itemId: 0,
  accessionNo: "",
  serialNo: undefined,
  model: undefined,
  make: undefined,
  purchaseDate: undefined,
  installationDate: undefined,
  warrantyExpiry: undefined,
  departmentId: undefined,
  storeId: undefined,
  location: undefined,
  cost: undefined,
  supplierId: undefined,
  custodianName: undefined,
  documents: undefined,
  workingStatus: 1,
};

export function EquipmentRegister() {
  const { data: items = [], isLoading, error } = useEquipmentList();
  const register = useRegisterEquipment();
  const update = useUpdateEquipment();
  const transfer = useTransferEquipment();

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<EquipmentForm>(emptyForm);
  const [errors, setErrors] = useState<Record<string, string>>({});

  const [transferTarget, setTransferTarget] = useState<number | null>(null);
  const [transferForm, setTransferForm] = useState<EquipmentTransferForm>({
    toDepartmentId: 0,
    reason: undefined,
    transferDate: undefined,
    toLocation: undefined,
    toStoreId: undefined,
  });
  const [transferErrors, setTransferErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const schema = editingId ? equipmentUpdateSchema : equipmentSchema;
    const result = schema.safeParse(form);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setErrors(fieldErrors);
      return;
    }
    const payload = { ...result.data };
    if (editingId) {
      update.mutate(
        { id: editingId, payload },
        {
          onSuccess: () => {
            setShowForm(false);
            setEditingId(null);
            setForm(emptyForm);
            setErrors({});
          },
        },
      );
    } else {
      register.mutate(payload as Record<string, unknown>, {
        onSuccess: () => {
          setShowForm(false);
          setForm(emptyForm);
          setErrors({});
        },
      });
    }
  };

  const startEdit = (item: Equipment) => {
    setEditingId(item.id);
    setForm({
      itemId: item.itemId,
      accessionNo: item.accessionNo,
      serialNo: item.serialNo,
      model: item.model,
      make: item.make,
      purchaseDate: item.purchaseDate,
      installationDate: item.installationDate,
      warrantyExpiry: item.warrantyExpiry,
      departmentId: item.departmentId,
      storeId: item.storeId,
      location: item.location,
      cost: item.cost,
      supplierId: item.supplierId,
      custodianName: item.custodianName,
      documents: item.documents,
      workingStatus: item.workingStatus,
    });
    setShowForm(true);
    setErrors({});
  };

  const handleTransfer = () => {
    const result = equipmentTransferSchema.safeParse(transferForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setTransferErrors(fieldErrors);
      return;
    }
    if (transferTarget) {
      transfer.mutate(
        { id: transferTarget, payload: result.data as Record<string, unknown> },
        {
          onSuccess: () => {
            setTransferTarget(null);
            setTransferForm({
              toDepartmentId: 0,
              reason: undefined,
              transferDate: undefined,
              toLocation: undefined,
              toStoreId: undefined,
            });
            setTransferErrors({});
          },
        },
      );
    }
  };

  if (isLoading) return <p className="muted">Loading equipment…</p>;
  if (error) return <p className="error">Error loading equipment: {error.message}</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Equipment Asset Register</h1>
        <button
          className="btn btn-primary"
          onClick={() => {
            setShowForm(!showForm);
            setEditingId(null);
            setForm(emptyForm);
            setErrors({});
          }}
        >
          {showForm ? "Cancel" : "Register Equipment"}
        </button>
      </header>

      {showForm && (
        <div className="card">
          <h2>{editingId ? "Edit Equipment" : "Register New Equipment"}</h2>
          <div className="form-grid">
            <label htmlFor="eq-itemId">
              Item ID *
              {errors.itemId && <span className="error">{errors.itemId}</span>}
            </label>
            <input
              id="eq-itemId"
              type="number"
              value={form.itemId || ""}
              onChange={(e) => setForm({ ...form, itemId: Number(e.target.value) })}
            />

            <label htmlFor="eq-accessionNo">
              Accession No. *
              {errors.accessionNo && <span className="error">{errors.accessionNo}</span>}
            </label>
            <input
              id="eq-accessionNo"
              value={form.accessionNo}
              onChange={(e) => setForm({ ...form, accessionNo: e.target.value })}
            />

            <label htmlFor="eq-serialNo">Serial No.</label>
            <input
              id="eq-serialNo"
              value={form.serialNo ?? ""}
              onChange={(e) => setForm({ ...form, serialNo: e.target.value })}
            />

            <label htmlFor="eq-model">Model</label>
            <input
              id="eq-model"
              value={form.model ?? ""}
              onChange={(e) => setForm({ ...form, model: e.target.value })}
            />

            <label htmlFor="eq-make">Make</label>
            <input
              id="eq-make"
              value={form.make ?? ""}
              onChange={(e) => setForm({ ...form, make: e.target.value })}
            />

            <label htmlFor="eq-purchaseDate">Purchase Date</label>
            <input
              id="eq-purchaseDate"
              type="date"
              value={form.purchaseDate ?? ""}
              onChange={(e) => setForm({ ...form, purchaseDate: e.target.value })}
            />

            <label htmlFor="eq-installationDate">Installation Date</label>
            <input
              id="eq-installationDate"
              type="date"
              value={form.installationDate ?? ""}
              onChange={(e) => setForm({ ...form, installationDate: e.target.value })}
            />

            <label htmlFor="eq-warrantyExpiry">Warranty Expiry</label>
            <input
              id="eq-warrantyExpiry"
              type="date"
              value={form.warrantyExpiry ?? ""}
              onChange={(e) => setForm({ ...form, warrantyExpiry: e.target.value })}
            />

            <label htmlFor="eq-departmentId">Department ID</label>
            <input
              id="eq-departmentId"
              type="number"
              value={form.departmentId ?? ""}
              onChange={(e) => setForm({ ...form, departmentId: Number(e.target.value) || undefined })}
            />

            <label htmlFor="eq-storeId">Store ID</label>
            <input
              id="eq-storeId"
              type="number"
              value={form.storeId ?? ""}
              onChange={(e) => setForm({ ...form, storeId: Number(e.target.value) || undefined })}
            />

            <label htmlFor="eq-location">Location</label>
            <input
              id="eq-location"
              value={form.location ?? ""}
              onChange={(e) => setForm({ ...form, location: e.target.value })}
            />

            <label htmlFor="eq-cost">Cost</label>
            <input
              id="eq-cost"
              type="number"
              step="0.01"
              value={form.cost ?? ""}
              onChange={(e) => setForm({ ...form, cost: Number(e.target.value) || undefined })}
            />

            <label htmlFor="eq-supplierId">Supplier ID</label>
            <input
              id="eq-supplierId"
              type="number"
              value={form.supplierId ?? ""}
              onChange={(e) => setForm({ ...form, supplierId: Number(e.target.value) || undefined })}
            />

            <label htmlFor="eq-custodianName">Custodian Name</label>
            <input
              id="eq-custodianName"
              value={form.custodianName ?? ""}
              onChange={(e) => setForm({ ...form, custodianName: e.target.value })}
            />

            <label htmlFor="eq-workingStatus">Working Status</label>
            <select
              id="eq-workingStatus"
              value={form.workingStatus}
              onChange={(e) => setForm({ ...form, workingStatus: Number(e.target.value) })}
            >
              {WORKING_STATUS_OPTIONS.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </div>
          <button
            className="btn btn-primary"
            onClick={handleSubmit}
            disabled={register.isPending || update.isPending}
          >
            {register.isPending || update.isPending
              ? "Saving…"
              : editingId
                ? "Update Equipment"
                : "Register"}
          </button>
        </div>
      )}

      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Accession No.</th>
              <th>Serial No.</th>
              <th>Model</th>
              <th>Make</th>
              <th>Location</th>
              <th>Working Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.map((e) => (
              <tr key={e.id}>
                <td>{e.id}</td>
                <td>{e.accessionNo}</td>
                <td>{e.serialNo ?? "—"}</td>
                <td>{e.model ?? "—"}</td>
                <td>{e.make ?? "—"}</td>
                <td>{e.location ?? "—"}</td>
                <td>
                  <span className={`badge ${STATUS_BADGE[e.workingStatus] ?? "badge-muted"}`}>
                    {WORKING_STATUS_OPTIONS.find((s) => s.value === e.workingStatus)?.label ??
                      e.workingStatus}
                  </span>
                </td>
                <td>
                  <button className="btn btn-sm" onClick={() => startEdit(e)}>
                    Edit
                  </button>{" "}
                  <button
                    className="btn btn-sm"
                    onClick={() => setTransferTarget(transferTarget === e.id ? null : e.id)}
                  >
                    Transfer
                  </button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={8} className="muted">
                  No equipment registered.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {transferTarget && (
        <div className="card">
          <h2>Transfer Equipment #{transferTarget}</h2>
          <div className="form-grid">
            <label htmlFor="tr-toDepartmentId">
              Target Department ID *
              {transferErrors.toDepartmentId && (
                <span className="error">{transferErrors.toDepartmentId}</span>
              )}
            </label>
            <input
              id="tr-toDepartmentId"
              type="number"
              value={transferForm.toDepartmentId || ""}
              onChange={(e) =>
                setTransferForm({ ...transferForm, toDepartmentId: Number(e.target.value) })
              }
            />

            <label htmlFor="tr-transferDate">Transfer Date</label>
            <input
              id="tr-transferDate"
              type="date"
              value={transferForm.transferDate ?? ""}
              onChange={(e) => setTransferForm({ ...transferForm, transferDate: e.target.value })}
            />

            <label htmlFor="tr-toLocation">Target Location</label>
            <input
              id="tr-toLocation"
              value={transferForm.toLocation ?? ""}
              onChange={(e) => setTransferForm({ ...transferForm, toLocation: e.target.value })}
            />

            <label htmlFor="tr-toStoreId">Target Store ID</label>
            <input
              id="tr-toStoreId"
              type="number"
              value={transferForm.toStoreId ?? ""}
              onChange={(e) =>
                setTransferForm({ ...transferForm, toStoreId: Number(e.target.value) || undefined })
              }
            />

            <label htmlFor="tr-reason">Reason</label>
            <textarea
              id="tr-reason"
              value={transferForm.reason ?? ""}
              onChange={(e) => setTransferForm({ ...transferForm, reason: e.target.value })}
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleTransfer}
            disabled={transfer.isPending}
          >
            {transfer.isPending ? "Transferring…" : "Confirm Transfer"}
          </button>
        </div>
      )}
    </section>
  );
}
