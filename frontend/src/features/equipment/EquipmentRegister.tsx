import { useState } from "react";
import { Button, HStack, SimpleGrid, Table, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
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
        <Button
          variant={showForm ? "secondary" : "primary"}
          onClick={() => {
            setShowForm(!showForm);
            setEditingId(null);
            setForm(emptyForm);
            setErrors({});
          }}
        >
          {showForm ? "Cancel" : "Register Equipment"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>{editingId ? "Edit Equipment" : "Register New Equipment"}</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Item ID" error={errors.itemId} required>
              <TextInput
                id="eq-itemId"
                aria-label="Item ID"
                type="number"
                value={form.itemId || ""}
                onChange={(e) => setForm({ ...form, itemId: Number(e.target.value) })}
              />
            </FormField>

            <FormField label="Accession No." error={errors.accessionNo} required>
              <TextInput
                id="eq-accessionNo"
                aria-label="Accession No."
                value={form.accessionNo}
                onChange={(e) => setForm({ ...form, accessionNo: e.target.value })}
              />
            </FormField>

            <FormField label="Serial No.">
              <TextInput
                id="eq-serialNo"
                aria-label="Serial No."
                value={form.serialNo ?? ""}
                onChange={(e) => setForm({ ...form, serialNo: e.target.value })}
              />
            </FormField>

            <FormField label="Model">
              <TextInput
                id="eq-model"
                aria-label="Model"
                value={form.model ?? ""}
                onChange={(e) => setForm({ ...form, model: e.target.value })}
              />
            </FormField>

            <FormField label="Make">
              <TextInput
                id="eq-make"
                aria-label="Make"
                value={form.make ?? ""}
                onChange={(e) => setForm({ ...form, make: e.target.value })}
              />
            </FormField>

            <FormField label="Purchase Date">
              <TextInput
                id="eq-purchaseDate"
                aria-label="Purchase Date"
                type="date"
                value={form.purchaseDate ?? ""}
                onChange={(e) => setForm({ ...form, purchaseDate: e.target.value })}
              />
            </FormField>

            <FormField label="Installation Date">
              <TextInput
                id="eq-installationDate"
                aria-label="Installation Date"
                type="date"
                value={form.installationDate ?? ""}
                onChange={(e) => setForm({ ...form, installationDate: e.target.value })}
              />
            </FormField>

            <FormField label="Warranty Expiry">
              <TextInput
                id="eq-warrantyExpiry"
                aria-label="Warranty Expiry"
                type="date"
                value={form.warrantyExpiry ?? ""}
                onChange={(e) => setForm({ ...form, warrantyExpiry: e.target.value })}
              />
            </FormField>

            <FormField label="Department ID">
              <TextInput
                id="eq-departmentId"
                aria-label="Department ID"
                type="number"
                value={form.departmentId ?? ""}
                onChange={(e) => setForm({ ...form, departmentId: Number(e.target.value) || undefined })}
              />
            </FormField>

            <FormField label="Store ID">
              <TextInput
                id="eq-storeId"
                aria-label="Store ID"
                type="number"
                value={form.storeId ?? ""}
                onChange={(e) => setForm({ ...form, storeId: Number(e.target.value) || undefined })}
              />
            </FormField>

            <FormField label="Location">
              <TextInput
                id="eq-location"
                aria-label="Location"
                value={form.location ?? ""}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
              />
            </FormField>

            <FormField label="Cost">
              <TextInput
                id="eq-cost"
                aria-label="Cost"
                type="number"
                step="0.01"
                value={form.cost ?? ""}
                onChange={(e) => setForm({ ...form, cost: Number(e.target.value) || undefined })}
              />
            </FormField>

            <FormField label="Supplier ID">
              <TextInput
                id="eq-supplierId"
                aria-label="Supplier ID"
                type="number"
                value={form.supplierId ?? ""}
                onChange={(e) => setForm({ ...form, supplierId: Number(e.target.value) || undefined })}
              />
            </FormField>

            <FormField label="Custodian Name">
              <TextInput
                id="eq-custodianName"
                aria-label="Custodian Name"
                value={form.custodianName ?? ""}
                onChange={(e) => setForm({ ...form, custodianName: e.target.value })}
              />
            </FormField>

            <FormField label="Working Status">
              <HimsSelect
                id="eq-workingStatus"
                aria-label="Working Status"
                value={form.workingStatus}
                onChange={(e) => setForm({ ...form, workingStatus: Number(e.target.value) })}
              >
                {WORKING_STATUS_OPTIONS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </HimsSelect>
            </FormField>
          </SimpleGrid>
          <Button
            variant="primary"
            onClick={handleSubmit}
            disabled={register.isPending || update.isPending}
            loading={register.isPending || update.isPending}
            loadingText="Saving…"
          >
            {editingId ? "Update Equipment" : "Register"}
          </Button>
        </div>
      )}

      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Accession No.</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Serial No.</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Model</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Make</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Location</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Working Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {items.map((e) => (
              <Table.Row key={e.id}>
                <Table.Cell>{e.id}</Table.Cell>
                <Table.Cell>{e.accessionNo}</Table.Cell>
                <Table.Cell>{e.serialNo ?? "—"}</Table.Cell>
                <Table.Cell>{e.model ?? "—"}</Table.Cell>
                <Table.Cell>{e.make ?? "—"}</Table.Cell>
                <Table.Cell>{e.location ?? "—"}</Table.Cell>
                <Table.Cell>
                  <span className={`badge ${STATUS_BADGE[e.workingStatus] ?? "badge-muted"}`}>
                    {WORKING_STATUS_OPTIONS.find((s) => s.value === e.workingStatus)?.label ??
                      e.workingStatus}
                  </span>
                </Table.Cell>
                <Table.Cell>
                  <HStack gap={1}>
                    <Button variant="secondary" size="sm" onClick={() => startEdit(e)}>
                      Edit
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => setTransferTarget(transferTarget === e.id ? null : e.id)}
                    >
                      Transfer
                    </Button>
                  </HStack>
                </Table.Cell>
              </Table.Row>
            ))}
            {items.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={8}>
                  No equipment registered.
                </Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </div>

      {transferTarget && (
        <div className="card">
          <h2>Transfer Equipment #{transferTarget}</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Target Department ID" error={transferErrors.toDepartmentId} required>
              <TextInput
                id="tr-toDepartmentId"
                aria-label="Target Department ID"
                type="number"
                value={transferForm.toDepartmentId || ""}
                onChange={(e) =>
                  setTransferForm({ ...transferForm, toDepartmentId: Number(e.target.value) })
                }
              />
            </FormField>

            <FormField label="Transfer Date">
              <TextInput
                id="tr-transferDate"
                aria-label="Transfer Date"
                type="date"
                value={transferForm.transferDate ?? ""}
                onChange={(e) => setTransferForm({ ...transferForm, transferDate: e.target.value })}
              />
            </FormField>

            <FormField label="Target Location">
              <TextInput
                id="tr-toLocation"
                aria-label="Target Location"
                value={transferForm.toLocation ?? ""}
                onChange={(e) => setTransferForm({ ...transferForm, toLocation: e.target.value })}
              />
            </FormField>

            <FormField label="Target Store ID">
              <TextInput
                id="tr-toStoreId"
                aria-label="Target Store ID"
                type="number"
                value={transferForm.toStoreId ?? ""}
                onChange={(e) =>
                  setTransferForm({ ...transferForm, toStoreId: Number(e.target.value) || undefined })
                }
              />
            </FormField>

            <FormField label="Reason">
              <Textarea
                id="tr-reason"
                aria-label="Reason"
                value={transferForm.reason ?? ""}
                onChange={(e) => setTransferForm({ ...transferForm, reason: e.target.value })}
                bg="white"
                borderColor="gray.300"
              />
            </FormField>
          </SimpleGrid>
          <Button
            variant="primary"
            onClick={handleTransfer}
            disabled={transfer.isPending}
            loading={transfer.isPending}
            loadingText="Transferring…"
          >
            Confirm Transfer
          </Button>
        </div>
      )}
    </section>
  );
}
