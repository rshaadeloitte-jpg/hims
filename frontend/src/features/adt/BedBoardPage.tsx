import { useState } from "react";
import { Button, Checkbox, Table } from "@chakra-ui/react";
import { TextInput } from "../../components/ui";
import { useCensus, useBeds, useCleaningComplete, useAdmissions, useAdmit, useDischarge } from "./queries";
import { admitSchema, type AdmitValues } from "./zodSchema";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

export function BedBoardPage() {
  const { data: census = [] } = useCensus();
  const { data: beds = [] } = useBeds();
  const { data: admissions = [] } = useAdmissions(undefined, true);
  const admit = useAdmit();
  const discharge = useDischarge();
  const cleaning = useCleaningComplete();

  const admitForm = useForm<AdmitValues>({ resolver: zodResolver(admitSchema) });
  const [showAdmit, setShowAdmit] = useState(false);
  const isMlc = admitForm.watch("isMlc");

  const submitAdmit = async (v: AdmitValues) => {
    await admit.mutateAsync(v as unknown as Record<string, unknown>);
    setShowAdmit(false);
    admitForm.reset();
  };

  const statusLabel = (s: number) => {
    const map: Record<number, string> = { 1: "Free", 2: "Occupied", 3: "Reserved", 4: "Maintenance", 5: "Cleaning" };
    return map[s] ?? "Unknown";
  };

  return (
    <section className="page">
      <header className="page__header">
        <h1>ADT — Bed Board</h1>
        <Button variant="primary" size="sm" onClick={() => setShowAdmit(!showAdmit)}>Admit</Button>
      </header>

      {showAdmit && (
        <form className="inline-form" onSubmit={admitForm.handleSubmit(submitAdmit)}>
          <TextInput type="number" placeholder="Patient ID" {...admitForm.register("patientId")} />
          <TextInput type="number" placeholder="Encounter ID" {...admitForm.register("encounterId")} />
          <TextInput placeholder="Admission Type" {...admitForm.register("admissionType")} />
          <TextInput type="number" placeholder="Bed ID (opt)" {...admitForm.register("bedId")} />
          <Checkbox.Root checked={!!isMlc} onCheckedChange={(e) => admitForm.setValue("isMlc", !!e.checked, { shouldValidate: true })}>
            <Checkbox.HiddenInput {...admitForm.register("isMlc")} />
            <Checkbox.Control />
            <Checkbox.Label>MLC</Checkbox.Label>
          </Checkbox.Root>
          <Button type="submit" variant="primary" size="sm" loading={admit.isPending} loadingText="Admit" disabled={admit.isPending}>Admit</Button>
        </form>
      )}

      <h2>Census</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Ward</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Total</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Occupied</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Free</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Cleaning</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Interim</Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {census.map((c) => (
            <Table.Row key={c.wardId}><Table.Cell>{c.wardName}</Table.Cell><Table.Cell>{c.totalBeds}</Table.Cell><Table.Cell>{c.occupied}</Table.Cell><Table.Cell>{c.free}</Table.Cell><Table.Cell>{c.cleaning}</Table.Cell><Table.Cell>{c.interimCount}</Table.Cell></Table.Row>
          ))}
          {census.length === 0 && <Table.Row><Table.Cell colSpan={6}>No census data.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Beds</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Bed</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Ward</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {beds.map((b) => (
            <Table.Row key={b.bedId}>
              <Table.Cell>{b.bedNo}</Table.Cell>
              <Table.Cell>{b.wardName ?? b.wardId}</Table.Cell>
              <Table.Cell>{statusLabel(b.status)}</Table.Cell>
              <Table.Cell>{b.status === 5 && <Button variant="secondary" size="sm" onClick={() => cleaning.mutate(b.bedId)}>Cleaning Done</Button>}</Table.Cell>
            </Table.Row>
          ))}
          {beds.length === 0 && <Table.Row><Table.Cell colSpan={4}>No beds.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>

      <h2>Active Admissions</h2>
      <Table.Root size="sm" variant="line">
        <Table.Header><Table.Row><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">IPD No</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Patient</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Type</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Doctor</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">MLC</Table.ColumnHeader><Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase"></Table.ColumnHeader></Table.Row></Table.Header>
        <Table.Body>
          {admissions.map((a) => (
            <Table.Row key={a.admissionId}>
              <Table.Cell>{a.ipdNo}</Table.Cell>
              <Table.Cell>{a.patientId}</Table.Cell>
              <Table.Cell>{a.admissionType}</Table.Cell>
              <Table.Cell>{a.doctorId}</Table.Cell>
              <Table.Cell>{a.isMlc ? "Yes" : "No"}</Table.Cell>
              <Table.Cell>
                <Button variant="secondary" size="sm" onClick={() => discharge.mutate({ id: a.admissionId, payload: {} })}>Discharge</Button>
              </Table.Cell>
            </Table.Row>
          ))}
          {admissions.length === 0 && <Table.Row><Table.Cell colSpan={6}>No active admissions.</Table.Cell></Table.Row>}
        </Table.Body>
      </Table.Root>
    </section>
  );
}