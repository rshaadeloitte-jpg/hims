import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Box, Button, SimpleGrid, Table, Textarea } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../components/ui";
import {
  useAnesthesiaRecords,
  useCreatePreOp,
  useAddVitalSign,
  useVitalSigns,
  useAddAnesthesiaDrug,
  useAnesthesiaDrugs,
  useCompletePostOp,
  type AnesthesiaRecord,
} from "./queries";
import {
  anesthesiaPreOpSchema,
  vitalSignSchema,
  anesthesiaDrugSchema,
  anesthesiaPostOpSchema,
  type AnesthesiaPreOpForm,
  type VitalSignForm,
  type AnesthesiaDrugForm,
  type AnesthesiaPostOpForm,
} from "./zodSchema";

function PreOpForm({ onCreated }: { onCreated: (recordId: number) => void }) {
  const mutation = useCreatePreOp();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AnesthesiaPreOpForm>({
    resolver: zodResolver(anesthesiaPreOpSchema),
    defaultValues: { scheduleId: 0, patientId: 0, asaGrade: "", airway: "", comorbidities: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      const record = await mutation.mutateAsync(values);
      onCreated(record.recordId ?? record.id ?? 0);
    } catch (error) {
      console.error("Pre-op creation failed:", error);
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-3 p-4 border border-gray-200 rounded-lg bg-white">
      <h4 className="text-sm font-semibold text-gray-900">Pre-Operative Assessment</h4>
      <SimpleGrid columns={{ base: 1, md: 2 }} gap={3}>
        <FormField label="Schedule ID *" error={errors.scheduleId?.message} required>
          <TextInput type="number" {...register("scheduleId", { valueAsNumber: true })} />
        </FormField>
        <FormField label="Patient ID *" error={errors.patientId?.message} required>
          <TextInput type="number" {...register("patientId", { valueAsNumber: true })} />
        </FormField>
        <FormField label="ASA Grade *" error={errors.asaGrade?.message} required>
          <HimsSelect {...register("asaGrade")}>
            <option value="">Select...</option>
            <option value="ASA_I">ASA I</option>
            <option value="ASA_II">ASA II</option>
            <option value="ASA_III">ASA III</option>
            <option value="ASA_IV">ASA IV</option>
            <option value="ASA_V">ASA V</option>
          </HimsSelect>
        </FormField>
        <FormField label="Airway Assessment *" error={errors.airway?.message} required>
          <TextInput type="text" placeholder="e.g. Mallampati I" {...register("airway")} />
        </FormField>
        <Box gridColumn={{ base: "auto", md: "span 2" }}>
          <FormField label="Comorbidities">
            <Textarea bg="white" borderColor="gray.300" rows={2} {...register("comorbidities")} />
          </FormField>
        </Box>
      </SimpleGrid>
      <Button type="submit" variant="primary" loading={mutation.isPending} loadingText="Saving...">
        Save Pre-Op Assessment
      </Button>
    </form>
  );
}

function VitalsForm({ recordId }: { recordId: number }) {
  const mutation = useAddVitalSign();
  const { data: vitals = [] } = useVitalSigns(recordId);
  const {
    register,
    handleSubmit,
    reset,
  } = useForm<VitalSignForm>({
    resolver: zodResolver(vitalSignSchema),
    defaultValues: { recordedAt: new Date().toISOString().slice(0, 16) },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({ recordId, data: values });
      reset({ ...values, recordedAt: new Date().toISOString().slice(0, 16) });
    } catch (error) {
      console.error("Vital sign add failed:", error);
    }
  });

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-900">Vital Signs</h4>
      {vitals.length > 0 && (
        <div className="overflow-x-auto">
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Time</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">HR</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">BP</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">SpO2</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">RR</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Temp</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {vitals.map((v) => (
                <Table.Row key={v.vitalId ?? v.id}>
                  <Table.Cell>{v.recordedAt ? new Date(v.recordedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}</Table.Cell>
                  <Table.Cell>{v.heartRate ?? "—"}</Table.Cell>
                  <Table.Cell>{v.bpSystolic ?? "—"}/{v.bpDiastolic ?? "—"}</Table.Cell>
                  <Table.Cell>{v.spo2 ?? "—"}</Table.Cell>
                  <Table.Cell>{v.respiratoryRate ?? "—"}</Table.Cell>
                  <Table.Cell>{v.temperature ?? "—"}</Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </div>
      )}
      <form onSubmit={onSubmit} className="grid grid-cols-2 md:grid-cols-3 gap-2 p-3 bg-gray-50 rounded border border-gray-200">
        <div>
          <FormField label="Recorded At">
            <TextInput type="datetime-local" {...register("recordedAt")} />
          </FormField>
        </div>
        <div>
          <FormField label="HR (bpm)">
            <TextInput type="number" {...register("hr", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div>
          <FormField label="BP Sys">
            <TextInput type="number" {...register("bpSys", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div>
          <FormField label="BP Dia">
            <TextInput type="number" {...register("bpDia", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div>
          <FormField label="SpO2 (%)">
            <TextInput type="number" {...register("spo2", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div>
          <FormField label="RR">
            <TextInput type="number" {...register("rr", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div>
          <FormField label="Temp (°C)">
            <TextInput type="number" step="0.1" {...register("temp", { valueAsNumber: true })} />
          </FormField>
        </div>
        <div className="col-span-2 md:col-span-3">
          <Button type="submit" variant="primary" loading={mutation.isPending} loadingText="Adding...">
            Add Vital Sign
          </Button>
        </div>
      </form>
    </div>
  );
}

function DrugForm({ recordId }: { recordId: number }) {
  const mutation = useAddAnesthesiaDrug();
  const { data: drugs = [] } = useAnesthesiaDrugs(recordId);
  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<AnesthesiaDrugForm>({
    resolver: zodResolver(anesthesiaDrugSchema),
    defaultValues: { drugName: "", dose: "", route: "", administeredAt: new Date().toISOString().slice(0, 16) },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({ recordId, data: values });
      reset({ drugName: "", dose: "", route: "", administeredAt: new Date().toISOString().slice(0, 16) });
    } catch (error) {
      console.error("Drug add failed:", error);
    }
  });

  return (
    <div className="space-y-3">
      <h4 className="text-sm font-semibold text-gray-900">Anesthesia Drugs</h4>
      {drugs.length > 0 && (
        <div className="overflow-x-auto">
          <Table.Root size="sm" variant="line">
            <Table.Header>
              <Table.Row>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Drug</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Dose</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Route</Table.ColumnHeader>
                <Table.ColumnHeader fontSize="xs" color="gray.600" textTransform="uppercase">Time</Table.ColumnHeader>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {drugs.map((d) => (
                <Table.Row key={d.drugId ?? d.id}>
                  <Table.Cell>{d.drugName}</Table.Cell>
                  <Table.Cell>{d.dose}</Table.Cell>
                  <Table.Cell>{d.route}</Table.Cell>
                  <Table.Cell>{d.administeredAt ? new Date(d.administeredAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}</Table.Cell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table.Root>
        </div>
      )}
      <form onSubmit={onSubmit} className="grid grid-cols-2 md:grid-cols-4 gap-2 p-3 bg-gray-50 rounded border border-gray-200">
        <div>
          <FormField label="Drug Name *" error={errors.drugName?.message} required>
            <TextInput type="text" {...register("drugName")} />
          </FormField>
        </div>
        <div>
          <FormField label="Dose *" error={errors.dose?.message} required>
            <TextInput type="text" {...register("dose")} />
          </FormField>
        </div>
        <div>
          <FormField label="Route *" error={errors.route?.message} required>
            <HimsSelect {...register("route")}>
              <option value="">Select...</option>
              <option value="IV">IV</option>
              <option value="IM">IM</option>
              <option value="SC">SC</option>
              <option value="INHALATION">Inhalation</option>
              <option value="EPIDURAL">Epidural</option>
              <option value="SPINAL">Spinal</option>
            </HimsSelect>
          </FormField>
        </div>
        <div>
          <FormField label="Administered At *" error={errors.administeredAt?.message} required>
            <TextInput type="datetime-local" {...register("administeredAt")} />
          </FormField>
        </div>
        <div className="col-span-2 md:col-span-4">
          <Button type="submit" variant="primary" loading={mutation.isPending} loadingText="Adding...">
            Add Drug
          </Button>
        </div>
      </form>
    </div>
  );
}

function PostOpForm({ recordId }: { recordId: number }) {
  const mutation = useCompletePostOp();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<AnesthesiaPostOpForm>({
    resolver: zodResolver(anesthesiaPostOpSchema),
    defaultValues: { pacuMinutes: 0, recoveryScore: 0, complications: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await mutation.mutateAsync({ recordId, ...values });
    } catch (error) {
      console.error("Post-op completion failed:", error);
    }
  });

  return (
    <form onSubmit={onSubmit} className="space-y-3 p-4 border border-gray-200 rounded-lg bg-white">
      <h4 className="text-sm font-semibold text-gray-900">Post-Operative / PACU</h4>
      <SimpleGrid columns={{ base: 1, md: 3 }} gap={3}>
        <FormField label="PACU Stay (min)">
          <TextInput type="number" {...register("pacuMinutes", { valueAsNumber: true })} />
        </FormField>
        <FormField label="Recovery Score (0-10)" error={errors.recoveryScore?.message}>
          <TextInput type="number" min={0} max={10} {...register("recoveryScore", { valueAsNumber: true })} />
        </FormField>
        <FormField label="Complications">
          <TextInput type="text" {...register("complications")} />
        </FormField>
      </SimpleGrid>
      <Button type="submit" variant="primary" loading={mutation.isPending} loadingText="Saving...">
        Complete Post-Op
      </Button>
    </form>
  );
}

export function AnesthesiaRecord() {
  const [activeRecordId, setActiveRecordId] = useState<number | null>(null);
  const { data: records = [], isLoading } = useAnesthesiaRecords();

  if (isLoading) return <div className="text-center py-8 text-gray-500 text-sm">Loading anesthesia records...</div>;

  return (
    <section className="space-y-6" aria-label="anesthesia-record">
      <header>
        <h2 className="text-xl font-semibold text-gray-900">Anesthesia Record</h2>
        <p className="text-sm text-gray-500">Pre-op assessment, intra-op vitals & drugs, post-op PACU notes</p>
      </header>

      <PreOpForm onCreated={(id) => setActiveRecordId(id)} />

      {records.length > 0 && (
        <div className="border border-gray-200 rounded-lg bg-white">
          <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
            <h3 className="text-sm font-medium text-gray-900">Active Records</h3>
          </div>
          <div className="divide-y divide-gray-200">
            {records.map((r) => {
              const id = r.recordId ?? r.id ?? 0;
              const isActive = id === activeRecordId;
              return (
                <div key={id} className="p-4">
                  <Button
                    type="button"
                    variant="ghost"
                    onClick={() => setActiveRecordId(isActive ? null : id)}
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-medium text-gray-900">Record #{id}</span>
                      <span className="text-xs text-gray-500">Patient #{r.patientId}</span>
                      {r.asaGrade && <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded">{r.asaGrade}</span>}
                    </div>
                    <svg className={`w-4 h-4 text-gray-400 transition-transform ${isActive ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </Button>
                  {isActive && (
                    <div className="mt-4 space-y-4">
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                        <div><span className="font-medium text-gray-700">Airway:</span> <span className="text-gray-600">{r.airwayAssessment ?? "—"}</span></div>
                        <div><span className="font-medium text-gray-700">Comorbidities:</span> <span className="text-gray-600">{r.comorbidities ?? "—"}</span></div>
                      </div>
                      <VitalsForm recordId={id} />
                      <DrugForm recordId={id} />
                      <PostOpForm recordId={id} />
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}
    </section>
  );
}
