import { useState } from "react";
import { Button, Table } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import {
  useCarePlans,
  useCreateCarePlan,
  useResolveCarePlan,
  useShiftHandovers,
  useCreateShiftHandover,
  useSignHandover,
} from "./queries";
import {
  carePlanSchema,
  shiftHandoverSchema,
  type CarePlanValues,
  type ShiftHandoverValues,
} from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function NursingCarePage() {
  const [admissionId, setAdmissionId] = useState(0);
  const { data: carePlans = [] } = useCarePlans(admissionId || undefined);
  const { data: handovers = [] } = useShiftHandovers();
  const createPlan = useCreateCarePlan();
  const resolvePlan = useResolveCarePlan();
  const createHandover = useCreateShiftHandover();
  const signHandover = useSignHandover();

  const planForm = useForm<CarePlanValues>({ resolver: zodResolver(carePlanSchema) });
  const handoverForm = useForm<ShiftHandoverValues>({ resolver: zodResolver(shiftHandoverSchema) });

  const submitPlan = async (v: CarePlanValues) => {
    await createPlan.mutateAsync(v);
    planForm.reset();
  };
  const submitHandover = async (v: ShiftHandoverValues) => {
    await createHandover.mutateAsync(v);
    handoverForm.reset();
  };

  return (
    <section className="page">
      <header className="page__header"><h1>Nursing — Care Plans & Handovers</h1></header>
      <div className="row">
        <TextInput type="number" placeholder="Admission ID" value={admissionId || ""} onChange={(e) => setAdmissionId(Number(e.target.value))} />
      </div>

      <h2>Care Plans</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Title</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Admission</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Goals</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {carePlans.map((p) => (
            <Table.Row key={p.carePlanId}>
              <Table.Cell>{p.title}</Table.Cell>
              <Table.Cell>{p.admissionId}</Table.Cell>
              <Table.Cell>{p.goals?.length ?? 0}</Table.Cell>
              <Table.Cell>{p.resolved ? "Resolved" : "Active"}</Table.Cell>
              <Table.Cell>{!p.resolved && <Button variant="secondary" size="sm" onClick={() => resolvePlan.mutate(p.carePlanId)}>Resolve</Button>}</Table.Cell>
            </Table.Row>
          ))}
          {carePlans.length === 0 && <Table.Row><Table.Cell colSpan={5}>No care plans.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
      <form className="inline-form" onSubmit={planForm.handleSubmit(submitPlan)}>
        <TextInput type="number" placeholder="Admission ID" {...planForm.register("admissionId")} />
        <TextInput type="number" placeholder="Patient ID" {...planForm.register("patientId")} />
        <TextInput placeholder="Title" {...planForm.register("title")} />
        <TextInput placeholder="Description" {...planForm.register("description")} />
        <Button type="submit" variant="primary" size="sm" loading={createPlan.isPending} loadingText="+ Add Plan" disabled={createPlan.isPending}>+ Add Plan</Button>
      </form>

      <h2>Shift Handovers</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Shift</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">From</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">To</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Admission</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Signed</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {handovers.map((h) => (
            <Table.Row key={h.handoverId}>
              <Table.Cell>{h.shift}</Table.Cell>
              <Table.Cell>{h.fromNurseId}</Table.Cell>
              <Table.Cell>{h.toNurseId}</Table.Cell>
              <Table.Cell>{h.admissionId}</Table.Cell>
              <Table.Cell>{h.signed ? "Yes" : "No"}</Table.Cell>
              <Table.Cell>{!h.signed && <Button variant="secondary" size="sm" onClick={() => signHandover.mutate(h.handoverId)}>Sign</Button>}</Table.Cell>
            </Table.Row>
          ))}
          {handovers.length === 0 && <Table.Row><Table.Cell colSpan={6}>No handovers.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
      <form className="inline-form" onSubmit={handoverForm.handleSubmit(submitHandover)}>
        <TextInput type="number" placeholder="Admission ID" {...handoverForm.register("admissionId")} />
        <TextInput type="number" placeholder="Patient ID" {...handoverForm.register("patientId")} />
        <TextInput type="number" placeholder="From Nurse ID" {...handoverForm.register("fromNurseId")} />
        <TextInput type="number" placeholder="To Nurse ID" {...handoverForm.register("toNurseId")} />
        <TextInput placeholder="Shift" {...handoverForm.register("shift")} />
        <Button type="submit" variant="primary" size="sm" loading={createHandover.isPending} loadingText="+ Handover" disabled={createHandover.isPending}>+ Handover</Button>
      </form>
    </section>
  );
}