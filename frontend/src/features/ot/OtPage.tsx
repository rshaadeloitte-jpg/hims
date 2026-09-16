import { useState } from "react";
import { Button, Table, Text, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import { useCreateOtSchedule, useOtSchedules, useUpdateOtScheduleStatus } from "./queries";
import { otScheduleSchema } from "./zodSchema";

const STATUS_OPTIONS = ["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"];

export function OtPage() {
  const { data: schedules = [], isLoading } = useOtSchedules();
  const createSchedule = useCreateOtSchedule();
  const updateStatus = useUpdateOtScheduleStatus();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ patientId: "", surgeryDate: "", otRoom: "", anesthesiaType: "", notes: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});

  const handleSubmit = () => {
    const result = otScheduleSchema.safeParse({
      ...form,
      patientId: form.patientId ? Number(form.patientId) : undefined,
    });
    if (!result.success) {
      const fieldErrors: Record<string, string> = {};
      result.error.errors.forEach((e) => { fieldErrors[e.path[0]] = e.message; });
      setErrors(fieldErrors);
      return;
    }
    createSchedule.mutate(result.data, { onSuccess: () => { setShowForm(false); setForm({ patientId: "", surgeryDate: "", otRoom: "", anesthesiaType: "", notes: "" }); setErrors({}); } });
  };

  if (isLoading) return <p className="muted">Loading OT schedules…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>OT & Surgery</h1>
        <Button variant={showForm ? "secondary" : "primary"} onClick={() => setShowForm(!showForm)}>
          {showForm ? "Cancel" : "Schedule Surgery"}
        </Button>
      </header>

      {showForm && (
        <div className="card">
          <h2>New Surgery Schedule</h2>
          <div className="form-grid">
            <FormField label="Patient ID" error={errors.patientId}>
              <TextInput value={form.patientId} onChange={(e) => setForm({ ...form, patientId: e.target.value })} />
            </FormField>
            <FormField label="Surgery Date" error={errors.surgeryDate}>
              <TextInput type="date" value={form.surgeryDate} onChange={(e) => setForm({ ...form, surgeryDate: e.target.value })} />
            </FormField>
            <FormField label="OT Room" error={errors.otRoom}>
              <TextInput value={form.otRoom} onChange={(e) => setForm({ ...form, otRoom: e.target.value })} />
            </FormField>
            <FormField label="Anesthesia Type">
              <HimsSelect value={form.anesthesiaType} onChange={(e) => setForm({ ...form, anesthesiaType: e.target.value })}>
                <option value="">Select…</option>
                <option value="GENERAL">General</option>
                <option value="SPINAL">Spinal</option>
                <option value="EPIDURAL">Epidural</option>
                <option value="LOCAL">Local</option>
              </HimsSelect>
            </FormField>
            <FormField label="Notes">
              <Textarea bg="white" borderColor="gray.300" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
            </FormField>
          </div>
          <Button variant="primary" onClick={handleSubmit} loading={createSchedule.isPending} loadingText="Saving…">
            Save Schedule
          </Button>
        </div>
      )}

      <div className="card">
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Room</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Anesthesia</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {schedules.map((s) => (
              <Table.Row key={s.id}>
                <Table.Cell>{s.patientName}</Table.Cell>
                <Table.Cell>{s.surgeryDate}</Table.Cell>
                <Table.Cell>{s.otRoom}</Table.Cell>
                <Table.Cell>{s.anesthesiaType ?? "—"}</Table.Cell>
                <Table.Cell>{s.status}</Table.Cell>
                <Table.Cell>
                  <HimsSelect value={s.status} onChange={(e) => updateStatus.mutate({ id: s.id ?? s.scheduleId ?? 0, status: e.target.value })}>
                    {STATUS_OPTIONS.map((st) => <option key={st} value={st}>{st}</option>)}
                  </HimsSelect>
                </Table.Cell>
              </Table.Row>
            ))}
            {schedules.length === 0 && <Table.Row><Table.Cell colSpan={6}><Text color="gray.500">No surgeries scheduled.</Text></Table.Cell></Table.Row>}
          </Table.Body>
        </Table.Root>
      </div>
    </section>
  );
}
