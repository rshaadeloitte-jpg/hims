import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Checkbox, HStack, Table, Text, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  useChambers,
  useAllocateChamber,
  useReleaseChamber,
  useUpdateDeathDetails,
  type ChamberRow,
} from "./queries";
import {
  chamberAllocationSchema,
  mortuaryClearanceSchema,
  deathDetailsSchema,
  CHAMBER_STATUS_TONE,
  CHAMBER_STATUS_LABEL,
  type ChamberAllocationValues,
  type MortuaryClearanceValues,
  type DeathDetailsValues,
} from "./zodSchema";

/**
 * M28 MortuaryBox — box management + death details.
 *
 * Two panels:
 *   1. Chamber grid — shows occupancy, allows allocation + release.
 *   2. Death details form — updatePatientDeathDetails (cause, type, MLC).
 *
 * Ported from `mortuaryBoxManagement.json` + `mortuaryClearanceSchema.json`
 * + `death_form.vm`.
 */

export function MortuaryBox() {
  const { data: chambers = [], isLoading } = useChambers();
  const allocate = useAllocateChamber();
  const release = useReleaseChamber();
  const updateDeath = useUpdateDeathDetails();

  const [showAllocate, setShowAllocate] = useState(false);
  const [showRelease, setShowRelease] = useState<number | null>(null);
  const [showDeathDetails, setShowDeathDetails] = useState<number | null>(null);

  if (isLoading) return <p className="muted">Loading chambers…</p>;

  return (
    <section className="page">
      <header className="page-header">
        <h1>Mortuary Box Management</h1>
        <Button variant={showAllocate ? "secondary" : "primary"} onClick={() => setShowAllocate(!showAllocate)}>
          {showAllocate ? "Cancel" : "Allocate Chamber"}
        </Button>
      </header>

      {showAllocate && <AllocationForm onSubmit={(v) => allocate.mutate(v)} />}

      <div className="card">
        <h2>Chambers</h2>
        <Table.Root size="sm" variant="line">
          <Table.Header>
            <Table.Row>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Chamber</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Body</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Admitted</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Status</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Death Cert</Table.ColumnHeader>
              <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Actions</Table.ColumnHeader>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {chambers.map((c) => (
              <ChamberRowView
                key={c.id}
                chamber={c}
                onRelease={() => setShowRelease(c.id)}
                onDeathDetails={() => setShowDeathDetails(c.patientId ?? 0)}
              />
            ))}
            {chambers.length === 0 && (
              <Table.Row>
                <Table.Cell colSpan={6}>
                  <Text color="gray.500">No chambers allocated.</Text>
                </Table.Cell>
              </Table.Row>
            )}
          </Table.Body>
        </Table.Root>
      </div>

      {showRelease !== null && (
        <ReleaseForm
          chamberId={showRelease}
          onSubmit={(v) => {
            release.mutate(v);
            setShowRelease(null);
          }}
          onCancel={() => setShowRelease(null)}
        />
      )}

      {showDeathDetails !== null && showDeathDetails > 0 && (
        <DeathDetailsForm
          patientId={showDeathDetails}
          onSubmit={(v) => {
            updateDeath.mutate(v);
            setShowDeathDetails(null);
          }}
          onCancel={() => setShowDeathDetails(null)}
        />
      )}
    </section>
  );
}

// ---- Chamber row ------------------------------------------------------------

function ChamberRowView({
  chamber,
  onRelease,
  onDeathDetails,
}: {
  chamber: ChamberRow;
  onRelease: () => void;
  onDeathDetails: () => void;
}) {
  const tone = CHAMBER_STATUS_TONE[(chamber.status as keyof typeof CHAMBER_STATUS_TONE) ?? "EMPTY"] ?? "badge--muted";
  const label = CHAMBER_STATUS_LABEL[(chamber.status as keyof typeof CHAMBER_STATUS_LABEL) ?? chamber.status] ?? chamber.status;

  return (
    <Table.Row>
      <Table.Cell>{chamber.chamberNo}</Table.Cell>
      <Table.Cell>{chamber.bodyName ?? "—"}</Table.Cell>
      <Table.Cell>{chamber.admittedAt?.slice(0, 10) ?? "—"}</Table.Cell>
      <Table.Cell>
        <span className={`badge ${tone}`}>{label}</span>
      </Table.Cell>
      <Table.Cell>{chamber.deathCertNo ?? "—"}</Table.Cell>
      <Table.Cell>
        <HStack gap={2}>
          {chamber.status === "OCCUPIED" && (
            <>
              <Button variant="secondary" size="sm" onClick={onRelease}>
                Release
              </Button>
              {chamber.patientId && (
                <Button variant="secondary" size="sm" onClick={onDeathDetails}>
                  Death Details
                </Button>
              )}
            </>
          )}
        </HStack>
      </Table.Cell>
    </Table.Row>
  );
}

// ---- Allocation form --------------------------------------------------------

function AllocationForm({ onSubmit }: { onSubmit: (v: ChamberAllocationValues) => void }) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ChamberAllocationValues>({
    resolver: zodResolver(chamberAllocationSchema),
    defaultValues: {
      chamberId: 0,
      patientId: 0,
      bodyName: "",
      causeOfDeath: "",
      admittedAt: new Date().toISOString().slice(0, 16),
    },
  });

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Allocate Chamber</h2>
      <div className="form-grid">
        <FormField label="Chamber ID" error={errors.chamberId?.message}>
          <TextInput type="number" {...register("chamberId")} />
        </FormField>

        <FormField label="Patient ID" error={errors.patientId?.message}>
          <TextInput type="number" {...register("patientId")} />
        </FormField>

        <FormField label="Body Name" error={errors.bodyName?.message}>
          <TextInput {...register("bodyName")} />
        </FormField>

        <FormField label="Cause of Death (optional)">
          <Textarea bg="white" borderColor="gray.300" {...register("causeOfDeath")} rows={2} />
        </FormField>

        <FormField label="Admitted At" error={errors.admittedAt?.message}>
          <TextInput type="datetime-local" {...register("admittedAt")} />
        </FormField>
      </div>
      <Button type="submit" variant="primary">
        Allocate
      </Button>
    </form>
  );
}

// ---- Release form -----------------------------------------------------------

function ReleaseForm({
  chamberId,
  onSubmit,
  onCancel,
}: {
  chamberId: number;
  onSubmit: (v: MortuaryClearanceValues) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<MortuaryClearanceValues>({
    resolver: zodResolver(mortuaryClearanceSchema),
    defaultValues: {
      chamberId,
      releasedTo: "",
      relationship: "OTHER",
      recipientId: "",
      releasedAt: new Date().toISOString().slice(0, 16),
      remark: "",
    },
  });

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Release Body — Chamber {chamberId}</h2>
      <div className="form-grid">
        <FormField label="Released To" error={errors.releasedTo?.message}>
          <TextInput {...register("releasedTo")} />
        </FormField>

        <FormField label="Relationship">
          <HimsSelect {...register("relationship")}>
            <option value="SPOUSE">Spouse</option>
            <option value="PARENT">Parent</option>
            <option value="CHILD">Child</option>
            <option value="SIBLING">Sibling</option>
            <option value="LEGAL">Legal</option>
            <option value="OTHER">Other</option>
          </HimsSelect>
        </FormField>

        <FormField label="Recipient ID">
          <TextInput {...register("recipientId")} />
        </FormField>

        <FormField label="Released At" error={errors.releasedAt?.message}>
          <TextInput type="datetime-local" {...register("releasedAt")} />
        </FormField>

        <FormField label="Remark">
          <Textarea bg="white" borderColor="gray.300" {...register("remark")} rows={2} />
        </FormField>
      </div>
      <HStack gap={2}>
        <Button type="submit" variant="primary">
          Confirm Release
        </Button>
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </HStack>
    </form>
  );
}

// ---- Death details form -----------------------------------------------------

function DeathDetailsForm({
  patientId,
  onSubmit,
  onCancel,
}: {
  patientId: number;
  onSubmit: (v: DeathDetailsValues) => void;
  onCancel: () => void;
}) {
  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors },
  } = useForm<DeathDetailsValues>({
    resolver: zodResolver(deathDetailsSchema),
    defaultValues: {
      patientId,
      isDeath: true,
      deathDate: new Date().toISOString().slice(0, 16),
      causeOfDeath: "",
      deathType: "NATURAL",
      certifiedBy: "",
      isMlc: false,
      mlcNumber: "",
    },
  });

  const isMlc = watch("isMlc");

  return (
    <form className="card" onSubmit={handleSubmit(onSubmit)}>
      <h2>Death Details — Patient {patientId}</h2>
      <div className="form-grid">
        <FormField label="Date of Death" error={errors.deathDate?.message}>
          <TextInput type="datetime-local" {...register("deathDate")} />
        </FormField>

        <FormField label="Cause of Death" error={errors.causeOfDeath?.message}>
          <Textarea bg="white" borderColor="gray.300" {...register("causeOfDeath")} rows={2} />
        </FormField>

        <FormField label="Death Type">
          <HimsSelect {...register("deathType")}>
            <option value="NATURAL">Natural</option>
            <option value="ACCIDENT">Accident</option>
            <option value="SUICIDE">Suicide</option>
            <option value="HOMICIDE">Homicide</option>
            <option value="PENDING">Pending</option>
            <option value="OTHER">Other</option>
          </HimsSelect>
        </FormField>

        <FormField label="Certified By" error={errors.certifiedBy?.message}>
          <TextInput {...register("certifiedBy")} />
        </FormField>

        <Checkbox.Root checked={!!isMlc} onCheckedChange={(e) => setValue("isMlc", !!e.checked, { shouldValidate: true })}>
          <Checkbox.HiddenInput {...register("isMlc")} />
          <Checkbox.Control />
          <Checkbox.Label>Medico-Legal Case (MLC)</Checkbox.Label>
        </Checkbox.Root>

        {isMlc && (
          <FormField label="MLC Number" error={errors.mlcNumber?.message}>
            <TextInput {...register("mlcNumber")} />
          </FormField>
        )}
      </div>
      <HStack gap={2}>
        <Button type="submit" variant="primary">
          Save Death Details
        </Button>
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
      </HStack>
    </form>
  );
}
