import { useState } from "react";
import { Button, Table } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import { useVitals, useRecordVitals, useNursingNotes, useCreateNursingNote, useNursingTasks, useCreateNursingTask, useCompleteTask } from "./queries";
import { vitalsSchema, nursingNoteSchema, nursingTaskSchema, type VitalsValues, type NursingNoteValues, type NursingTaskValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

// DEPRECATED: Replaced by NursingWorklist.tsx for M09 implementation
// TODO: Remove after verification
export function NursingWorksListPage() {
  const [patientId, setPatientId] = useState(0);
  const [admissionId, setAdmissionId] = useState(0);
  const { data: vitals = [] } = useVitals(patientId);
  const { data: notes = [] } = useNursingNotes(admissionId);
  const { data: tasks = [] } = useNursingTasks();
  const recordVitals = useRecordVitals();
  const createNote = useCreateNursingNote();
  const createTask = useCreateNursingTask();
  const completeTask = useCompleteTask();

  const vitalsForm = useForm<VitalsValues>({ resolver: zodResolver(vitalsSchema) });
  const noteForm = useForm<NursingNoteValues>({ resolver: zodResolver(nursingNoteSchema) });
  const taskForm = useForm<NursingTaskValues>({ resolver: zodResolver(nursingTaskSchema) });

  const submitVitals = async (v: VitalsValues) => {
    await recordVitals.mutateAsync({ patientId, payload: v as unknown as Record<string, unknown> });
    vitalsForm.reset();
  };
  const submitNote = async (v: NursingNoteValues) => {
    await createNote.mutateAsync({ admissionId, payload: v as unknown as Record<string, unknown> });
    noteForm.reset();
  };
  const submitTask = async (v: NursingTaskValues) => {
    await createTask.mutateAsync(v as unknown as Record<string, unknown>);
    taskForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Nursing — Worklist</h1></header>
      <div className="row">
        <TextInput type="number" placeholder="Patient ID" value={patientId || ""} onChange={(e) => setPatientId(Number(e.target.value))} />
        <TextInput type="number" placeholder="Admission ID" value={admissionId || ""} onChange={(e) => setAdmissionId(Number(e.target.value))} />
      </div>

      <h2>Vitals</h2>
      {patientId > 0 && (
        <form className="inline-form" onSubmit={vitalsForm.handleSubmit(submitVitals)}>
          <TextInput type="number" step="0.1" placeholder="Temp" {...vitalsForm.register("temperature")} />
          <TextInput type="number" placeholder="Pulse" {...vitalsForm.register("pulse")} />
          <TextInput type="number" placeholder="RR" {...vitalsForm.register("respiration")} />
          <TextInput type="number" placeholder="BP Sys" {...vitalsForm.register("bloodPressureSystolic")} />
          <TextInput type="number" placeholder="BP Dia" {...vitalsForm.register("bloodPressureDiastolic")} />
          <TextInput type="number" placeholder="SpO2" {...vitalsForm.register("spO2")} />
          <Button type="submit" variant="primary" size="sm" loading={recordVitals.isPending} loadingText="Record" disabled={recordVitals.isPending}>Record</Button>
        </form>
      )}
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Temp</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Pulse</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">RR</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">BP</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">SpO2</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {vitals.slice(0, 10).map((v) => (
            <Table.Row key={v.vitalId}>
              <Table.Cell>{v.recordedAt?.slice(0, 16)}</Table.Cell>
              <Table.Cell>{v.temperature}</Table.Cell>
              <Table.Cell>{v.pulse}</Table.Cell>
              <Table.Cell>{v.respiration}</Table.Cell>
              <Table.Cell>{v.bloodPressureSystolic}/{v.bloodPressureDiastolic}</Table.Cell>
              <Table.Cell>{v.spO2}</Table.Cell>
            </Table.Row>
          ))}
          {vitals.length === 0 && <Table.Row><Table.Cell colSpan={6}>No vitals.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Notes</h2>
      {admissionId > 0 && (
        <form className="inline-form" onSubmit={noteForm.handleSubmit(submitNote)}>
          <TextInput placeholder="Category" {...noteForm.register("category")} />
          <TextInput placeholder="Note content" {...noteForm.register("content")} />
          <Button type="submit" variant="primary" size="sm" loading={createNote.isPending} loadingText="+ Note" disabled={createNote.isPending}>+ Note</Button>
        </form>
      )}
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Date</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Category</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Content</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {notes.slice(0, 10).map((n) => (
            <Table.Row key={n.noteId}><Table.Cell>{n.createdAt?.slice(0, 16)}</Table.Cell><Table.Cell>{n.category ?? "—"}</Table.Cell><Table.Cell>{n.content}</Table.Cell></Table.Row>
          ))}
          {notes.length === 0 && <Table.Row><Table.Cell colSpan={3}>No notes.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Tasks</h2>
      <form className="inline-form" onSubmit={taskForm.handleSubmit(submitTask)}>
        <TextInput type="number" placeholder="Admission ID" {...taskForm.register("admissionId")} />
        <TextInput type="number" placeholder="Patient ID" {...taskForm.register("patientId")} />
        <TextInput placeholder="Task Type" {...taskForm.register("taskType")} />
        <TextInput placeholder="Description" {...taskForm.register("description")} />
        <Button type="submit" variant="primary" size="sm" loading={createTask.isPending} loadingText="+ Task" disabled={createTask.isPending}>+ Task</Button>
      </form>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Type</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Description</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {tasks.map((t) => (
            <Table.Row key={t.taskId}>
              <Table.Cell>{t.taskType}</Table.Cell>
              <Table.Cell>{t.description}</Table.Cell>
              <Table.Cell>{t.completed ? "Done" : t.cancelled ? "Cancelled" : "Open"}</Table.Cell>
              <Table.Cell>{!t.completed && !t.cancelled && <Button variant="secondary" size="sm" onClick={() => completeTask.mutate(t.taskId)}>Complete</Button>}</Table.Cell>
            </Table.Row>
          ))}
          {tasks.length === 0 && <Table.Row><Table.Cell colSpan={4}>No tasks.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
    </section>
  );
}