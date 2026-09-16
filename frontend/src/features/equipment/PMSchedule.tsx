import { useState } from "react";
import { Button, Checkbox, HStack, SimpleGrid, Table, Textarea, Wrap } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  usePmSchedules,
  useCreatePmSchedule,
  usePmHistory,
  useCompletePm,
  usePmChecklists,
} from "./queries";
import {
  pmScheduleSchema,
  pmCompletionSchema,
  type PmScheduleForm,
  type PmCompletionForm,
} from "./zodSchema";

const MAINTENANCE_TYPE_LABELS: Record<number, string> = {
  1: "Preventive",
  2: "Corrective",
  3: "Calibration",
};

const PM_STATUS_LABELS: Record<number, string> = {
  1: "Scheduled",
  2: "In Progress",
  3: "Completed",
  4: "Cancelled",
};

const STATUS_BADGE: Record<number, string> = {
  1: "badge-info",
  2: "badge-warning",
  3: "badge-success",
  4: "badge-muted",
};

const MONTHS = [
  { key: "jan", label: "Jan" },
  { key: "feb", label: "Feb" },
  { key: "mar", label: "Mar" },
  { key: "apr", label: "Apr" },
  { key: "may", label: "May" },
  { key: "jun", label: "Jun" },
  { key: "jul", label: "Jul" },
  { key: "aug", label: "Aug" },
  { key: "sep", label: "Sep" },
  { key: "oct", label: "Oct" },
  { key: "nov", label: "Nov" },
  { key: "dec", label: "Dec" },
] as const;

const emptyScheduleForm: PmScheduleForm = {
  equipmentId: 0,
  scheduleCode: undefined,
  frequency: 1,
  pmDate: undefined,
  jan: false,
  feb: false,
  mar: false,
  apr: false,
  may: false,
  jun: false,
  jul: false,
  aug: false,
  sep: false,
  oct: false,
  nov: false,
  dec: false,
  pmType: 1,
  storeId: undefined,
};

const emptyCompletionForm: PmCompletionForm = {
  maintenanceNo: undefined,
  maintenanceDate: undefined,
  performedBy: undefined,
  functionalCheckRemark: undefined,
  preventiveMaintenanceRemark: undefined,
  electricalSafetyRemark: undefined,
  calibration: undefined,
  furtherActions: undefined,
  commentsOnUse: undefined,
  remarks: undefined,
  deptInCharge: undefined,
  biomedicalEngineerId: undefined,
  biomedicalInChargeId: undefined,
  verifiedBy: undefined,
  amountIncurred: undefined,
  serviceAgentId: undefined,
  pmScheduleDetailId: undefined,
  itemId: undefined,
};

export function PMSchedule() {
  const { data: schedules = [], isLoading, error } = usePmSchedules();
  const create = useCreatePmSchedule();
  const complete = useCompletePm();
  const { data: checklists = [] } = usePmChecklists();

  const [showForm, setShowForm] = useState(false);
  const [scheduleForm, setScheduleForm] = useState<PmScheduleForm>(emptyScheduleForm);
  const [scheduleErrors, setScheduleErrors] = useState<Record<string, string>>({});

  const [completingId, setCompletingId] = useState<number | null>(null);
  const [completionForm, setCompletionForm] = useState<PmCompletionForm>(emptyCompletionForm);

  const [expandedId, setExpandedId] = useState<number | null>(null);
  const { data: history } = usePmHistory(expandedId ?? 0);

  const handleCreate = () => {
    const result = pmScheduleSchema.safeParse(scheduleForm);
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => {
        fieldErrors[e.path[0] as string] = e.message;
      });
      setScheduleErrors(fieldErrors);
      return;
    }
    create.mutate(result.data as Record<string, unknown>, {
      onSuccess: () => {
        setShowForm(false);
        setScheduleForm(emptyScheduleForm);
        setScheduleErrors({});
      },
    });
  };

  const handleComplete = () => {
    const result = pmCompletionSchema.safeParse(completionForm);
    if (!result.success) {
      return;
    }
    if (completingId) {
      complete.mutate(
        { scheduleId: completingId, payload: result.data as Record<string, unknown> },
        {
          onSuccess: () => {
            setCompletingId(null);
            setCompletionForm(emptyCompletionForm);
          },
        },
      );
    }
  };

  const toggleMonth = (key: keyof PmScheduleForm) => {
    setScheduleForm({ ...scheduleForm, [key]: !scheduleForm[key] });
  };

  if (isLoading) return <p className="muted">Loading PM schedules…</p>;
  if (error) return <p className="error">Error loading schedules: {error.message}</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Preventive Maintenance & Calibration</h1>
        <Button
          variant={showForm ? "secondary" : "primary"}
          onClick={() => {
            setShowForm(!showForm);
            setCompletingId(null);
          }}
        >
          {showForm ? "Cancel" : "New PM Schedule"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>Create PM Schedule</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Equipment ID" error={scheduleErrors.equipmentId} required>
              <TextInput
                id="pm-equipmentId"
                aria-label="Equipment ID"
                type="number"
                value={scheduleForm.equipmentId || ""}
                onChange={(e) =>
                  setScheduleForm({ ...scheduleForm, equipmentId: Number(e.target.value) })
                }
              />
            </FormField>

            <FormField label="Schedule Code">
              <TextInput
                id="pm-scheduleCode"
                aria-label="Schedule Code"
                value={scheduleForm.scheduleCode ?? ""}
                onChange={(e) => setScheduleForm({ ...scheduleForm, scheduleCode: e.target.value })}
              />
            </FormField>

            <FormField label="Frequency (months)" error={scheduleErrors.frequency} required>
              <TextInput
                id="pm-frequency"
                aria-label="Frequency (months)"
                type="number"
                value={scheduleForm.frequency}
                onChange={(e) => setScheduleForm({ ...scheduleForm, frequency: Number(e.target.value) })}
              />
            </FormField>

            <FormField label="PM Start Date">
              <TextInput
                id="pm-pmDate"
                aria-label="PM Start Date"
                type="date"
                value={scheduleForm.pmDate ?? ""}
                onChange={(e) => setScheduleForm({ ...scheduleForm, pmDate: e.target.value })}
              />
            </FormField>

            <FormField label="Maintenance Type">
              <HimsSelect
                id="pm-pmType"
                aria-label="Maintenance Type"
                value={scheduleForm.pmType}
                onChange={(e) => setScheduleForm({ ...scheduleForm, pmType: Number(e.target.value) })}
              >
                <option value={1}>Preventive</option>
                <option value={2}>Corrective</option>
                <option value={3}>Calibration</option>
              </HimsSelect>
            </FormField>

            <FormField label="Store ID">
              <TextInput
                id="pm-storeId"
                aria-label="Store ID"
                type="number"
                value={scheduleForm.storeId ?? ""}
                onChange={(e) =>
                  setScheduleForm({ ...scheduleForm, storeId: Number(e.target.value) || undefined })
                }
              />
            </FormField>

            <FormField label="Service Months">
              <Wrap gap={2} className="month-toggles">
                {MONTHS.map((m) => (
                  <Checkbox.Root
                    key={m.key}
                    checked={!!(scheduleForm[m.key] as boolean)}
                    onCheckedChange={() => toggleMonth(m.key)}
                  >
                    <Checkbox.Control />
                    <Checkbox.Label>{m.label}</Checkbox.Label>
                  </Checkbox.Root>
                ))}
              </Wrap>
            </FormField>
          </SimpleGrid>
          <Button variant="primary" onClick={handleCreate} disabled={create.isPending} loading={create.isPending} loadingText="Creating…">
            Create Schedule
          </Button>
        </div>
      )}

      {completingId && (
        <div className="card">
          <h2>Record Completion — Schedule #{completingId}</h2>
          <SimpleGrid columns={{ base: 1, md: 2 }} gap={4} className="form-grid">
            <FormField label="Maintenance No.">
              <TextInput
                id="pmc-maintenanceNo"
                aria-label="Maintenance No."
                value={completionForm.maintenanceNo ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, maintenanceNo: e.target.value })
                }
              />
            </FormField>

            <FormField label="Maintenance Date">
              <TextInput
                id="pmc-maintenanceDate"
                aria-label="Maintenance Date"
                type="date"
                value={completionForm.maintenanceDate ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, maintenanceDate: e.target.value })
                }
              />
            </FormField>

            <FormField label="Performed By (Engineer ID)">
              <TextInput
                id="pmc-performedBy"
                aria-label="Performed By (Engineer ID)"
                type="number"
                value={completionForm.performedBy ?? ""}
                onChange={(e) =>
                  setCompletionForm({
                    ...completionForm,
                    performedBy: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Functional Check Remark">
              <Textarea
                id="pmc-functionalCheckRemark"
                aria-label="Functional Check Remark"
                value={completionForm.functionalCheckRemark ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, functionalCheckRemark: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Preventive Maintenance Remark">
              <Textarea
                id="pmc-preventiveMaintenanceRemark"
                aria-label="Preventive Maintenance Remark"
                value={completionForm.preventiveMaintenanceRemark ?? ""}
                onChange={(e) =>
                  setCompletionForm({
                    ...completionForm,
                    preventiveMaintenanceRemark: e.target.value,
                  })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Electrical Safety Remark">
              <Textarea
                id="pmc-electricalSafetyRemark"
                aria-label="Electrical Safety Remark"
                value={completionForm.electricalSafetyRemark ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, electricalSafetyRemark: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Calibration">
              <Textarea
                id="pmc-calibration"
                aria-label="Calibration"
                value={completionForm.calibration ?? ""}
                onChange={(e) => setCompletionForm({ ...completionForm, calibration: e.target.value })}
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Further Actions">
              <Textarea
                id="pmc-furtherActions"
                aria-label="Further Actions"
                value={completionForm.furtherActions ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, furtherActions: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Comments on Use">
              <Textarea
                id="pmc-commentsOnUse"
                aria-label="Comments on Use"
                value={completionForm.commentsOnUse ?? ""}
                onChange={(e) =>
                  setCompletionForm({ ...completionForm, commentsOnUse: e.target.value })
                }
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Remarks">
              <Textarea
                id="pmc-remarks"
                aria-label="Remarks"
                value={completionForm.remarks ?? ""}
                onChange={(e) => setCompletionForm({ ...completionForm, remarks: e.target.value })}
                bg="white"
                borderColor="gray.300"
              />
            </FormField>

            <FormField label="Amount Incurred">
              <TextInput
                id="pmc-amountIncurred"
                aria-label="Amount Incurred"
                type="number"
                step="0.01"
                value={completionForm.amountIncurred ?? ""}
                onChange={(e) =>
                  setCompletionForm({
                    ...completionForm,
                    amountIncurred: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Verified By">
              <TextInput
                id="pmc-verifiedBy"
                aria-label="Verified By"
                type="number"
                value={completionForm.verifiedBy ?? ""}
                onChange={(e) =>
                  setCompletionForm({
                    ...completionForm,
                    verifiedBy: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>

            <FormField label="Service Agent ID">
              <TextInput
                id="pmc-serviceAgentId"
                aria-label="Service Agent ID"
                type="number"
                value={completionForm.serviceAgentId ?? ""}
                onChange={(e) =>
                  setCompletionForm({
                    ...completionForm,
                    serviceAgentId: Number(e.target.value) || undefined,
                  })
                }
              />
            </FormField>
          </SimpleGrid>
          <Button
            variant="primary"
            onClick={handleComplete}
            disabled={complete.isPending}
            loading={complete.isPending}
            loadingText="Saving…"
          >
            Record Completion
          </Button>
        </div>
      )}

      <div className="card">
        <h2>PM Schedules</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">ID</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Schedule Code</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Equipment</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Type</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Frequency</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Next Due</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Last Completed</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {schedules.map((s) => (
              <Table.Row key={s.id}>
                <Table.Cell>{s.id}</Table.Cell>
                <Table.Cell>{s.scheduleCode ?? "—"}</Table.Cell>
                <Table.Cell>{s.equipmentId}</Table.Cell>
                <Table.Cell>
                  <span className="badge badge-info">
                    {MAINTENANCE_TYPE_LABELS[s.pmType] ?? s.pmType}
                  </span>
                </Table.Cell>
                <Table.Cell>{s.frequency} mo</Table.Cell>
                <Table.Cell>{s.nextDueDate ?? "—"}</Table.Cell>
                <Table.Cell>{s.lastCompletedDate ?? "—"}</Table.Cell>
                <Table.Cell>
                  <HStack gap={1}>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => setExpandedId(expandedId === s.id ? null : s.id)}
                    >
                      {expandedId === s.id ? "Hide" : "History"}
                    </Button>
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => setCompletingId(s.id)}
                    >
                      Complete
                    </Button>
                  </HStack>
                </Table.Cell>
              </Table.Row>
            ))}
            {schedules.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={8}>
                  No PM schedules.
                </Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </div>

      {expandedId && history && history.length > 0 && (
        <div className="card">
          <h2>Execution History — Schedule #{expandedId}</h2>
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Maintenance No.</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Performed By</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Amount</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Calibration</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {history.map((h) => (
                <Table.Row key={h.id}>
                  <Table.Cell>{h.maintenanceNo ?? "—"}</Table.Cell>
                  <Table.Cell>{h.maintenanceDate ?? "—"}</Table.Cell>
                  <Table.Cell>
                    <span className={`badge ${STATUS_BADGE[h.pmStatus] ?? "badge-muted"}`}>
                      {PM_STATUS_LABELS[h.pmStatus] ?? h.pmStatus}
                    </span>
                  </Table.Cell>
                  <Table.Cell>{h.performedBy ?? "—"}</Table.Cell>
                  <Table.Cell>{h.amountIncurred != null ? `₹${h.amountIncurred}` : "—"}</Table.Cell>
                  <Table.Cell>{h.calibration ?? "—"}</Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </div>
      )}

      {checklists.length > 0 && (
        <div className="card">
          <h2>PM Checklist Templates</h2>
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Code</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Description</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {checklists.map((c) => (
                <Table.Row key={c.id}>
                  <Table.Cell>{c.code}</Table.Cell>
                  <Table.Cell>{c.description}</Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </div>
      )}
    </section>
  );
}
