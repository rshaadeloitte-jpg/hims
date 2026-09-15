import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  useAnesthesiaRecords,
  useCreatePreOp,
  useAddVitalSign,
  useVitalSigns,
  useAddAnesthesiaDrug,
  useAnesthesiaDrugs,
  useCompletePostOp,
  type AnesthesiaRecord,
  type VitalSignRecord,
  type AnesthesiaDrug,
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
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Schedule ID *</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("scheduleId", { valueAsNumber: true })} />
          {errors.scheduleId && <p className="text-xs text-red-600">{errors.scheduleId.message}</p>}
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Patient ID *</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("patientId", { valueAsNumber: true })} />
          {errors.patientId && <p className="text-xs text-red-600">{errors.patientId.message}</p>}
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">ASA Grade *</label>
          <select className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("asaGrade")}>
            <option value="">Select...</option>
            <option value="ASA_I">ASA I</option>
            <option value="ASA_II">ASA II</option>
            <option value="ASA_III">ASA III</option>
            <option value="ASA_IV">ASA IV</option>
            <option value="ASA_V">ASA V</option>
          </select>
          {errors.asaGrade && <p className="text-xs text-red-600">{errors.asaGrade.message}</p>}
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Airway Assessment *</label>
          <input type="text" placeholder="e.g. Mallampati I" className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("airway")} />
          {errors.airway && <p className="text-xs text-red-600">{errors.airway.message}</p>}
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-gray-700 mb-1">Comorbidities</label>
          <textarea rows={2} className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("comorbidities")} />
        </div>
      </div>
      <button type="submit" disabled={mutation.isPending} className="px-3 py-1.5 bg-blue-600 text-white text-xs font-medium rounded hover:bg-blue-700 disabled:opacity-50">
        {mutation.isPending ? "Saving..." : "Save Pre-Op Assessment"}
      </button>
    </form>
  );
}

function VitalsForm({ recordId }: { recordId: number }) {
  const mutation = useAddVitalSign();
  const { data: vitals = [] } = useVitalSigns(recordId);
  const {
    register,
    handleSubmit,
    formState: { errors },
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
          <table className="w-full text-xs border border-gray-200 rounded">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-2 py-1 text-left">Time</th>
                <th className="px-2 py-1 text-left">HR</th>
                <th className="px-2 py-1 text-left">BP</th>
                <th className="px-2 py-1 text-left">SpO2</th>
                <th className="px-2 py-1 text-left">RR</th>
                <th className="px-2 py-1 text-left">Temp</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {vitals.map((v) => (
                <tr key={v.vitalId ?? v.id}>
                  <td className="px-2 py-1">{v.recordedAt ? new Date(v.recordedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}</td>
                  <td className="px-2 py-1">{v.heartRate ?? "—"}</td>
                  <td className="px-2 py-1">{v.bpSystolic ?? "—"}/{v.bpDiastolic ?? "—"}</td>
                  <td className="px-2 py-1">{v.spo2 ?? "—"}</td>
                  <td className="px-2 py-1">{v.respiratoryRate ?? "—"}</td>
                  <td className="px-2 py-1">{v.temperature ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <form onSubmit={onSubmit} className="grid grid-cols-2 md:grid-cols-3 gap-2 p-3 bg-gray-50 rounded border border-gray-200">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Recorded At</label>
          <input type="datetime-local" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("recordedAt")} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">HR (bpm)</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("hr", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">BP Sys</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("bpSys", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">BP Dia</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("bpDia", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">SpO2 (%)</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("spo2", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">RR</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("rr", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Temp (°C)</label>
          <input type="number" step="0.1" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("temp", { valueAsNumber: true })} />
        </div>
        <div className="col-span-2 md:col-span-3">
          <button type="submit" disabled={mutation.isPending} className="px-3 py-1.5 bg-green-600 text-white text-xs font-medium rounded hover:bg-green-700 disabled:opacity-50">
            {mutation.isPending ? "Adding..." : "Add Vital Sign"}
          </button>
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
          <table className="w-full text-xs border border-gray-200 rounded">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-2 py-1 text-left">Drug</th>
                <th className="px-2 py-1 text-left">Dose</th>
                <th className="px-2 py-1 text-left">Route</th>
                <th className="px-2 py-1 text-left">Time</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {drugs.map((d) => (
                <tr key={d.drugId ?? d.id}>
                  <td className="px-2 py-1">{d.drugName}</td>
                  <td className="px-2 py-1">{d.dose}</td>
                  <td className="px-2 py-1">{d.route}</td>
                  <td className="px-2 py-1">{d.administeredAt ? new Date(d.administeredAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <form onSubmit={onSubmit} className="grid grid-cols-2 md:grid-cols-4 gap-2 p-3 bg-gray-50 rounded border border-gray-200">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Drug Name *</label>
          <input type="text" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("drugName")} />
          {errors.drugName && <p className="text-xs text-red-600">{errors.drugName.message}</p>}
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Dose *</label>
          <input type="text" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("dose")} />
          {errors.dose && <p className="text-xs text-red-600">{errors.dose.message}</p>}
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Route *</label>
          <select className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("route")}>
            <option value="">Select...</option>
            <option value="IV">IV</option>
            <option value="IM">IM</option>
            <option value="SC">SC</option>
            <option value="INHALATION">Inhalation</option>
            <option value="EPIDURAL">Epidural</option>
            <option value="SPINAL">Spinal</option>
          </select>
          {errors.route && <p className="text-xs text-red-600">{errors.route.message}</p>}
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Administered At *</label>
          <input type="datetime-local" className="w-full border border-gray-300 rounded px-2 py-1 text-xs" {...register("administeredAt")} />
          {errors.administeredAt && <p className="text-xs text-red-600">{errors.administeredAt.message}</p>}
        </div>
        <div className="col-span-2 md:col-span-4">
          <button type="submit" disabled={mutation.isPending} className="px-3 py-1.5 bg-purple-600 text-white text-xs font-medium rounded hover:bg-purple-700 disabled:opacity-50">
            {mutation.isPending ? "Adding..." : "Add Drug"}
          </button>
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
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">PACU Stay (min)</label>
          <input type="number" className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("pacuMinutes", { valueAsNumber: true })} />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Recovery Score (0-10)</label>
          <input type="number" min={0} max={10} className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("recoveryScore", { valueAsNumber: true })} />
          {errors.recoveryScore && <p className="text-xs text-red-600">{errors.recoveryScore.message}</p>}
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-700 mb-1">Complications</label>
          <input type="text" className="w-full border border-gray-300 rounded px-2 py-1.5 text-sm" {...register("complications")} />
        </div>
      </div>
      <button type="submit" disabled={mutation.isPending} className="px-3 py-1.5 bg-indigo-600 text-white text-xs font-medium rounded hover:bg-indigo-700 disabled:opacity-50">
        {mutation.isPending ? "Saving..." : "Complete Post-Op"}
      </button>
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
                  <button
                    type="button"
                    onClick={() => setActiveRecordId(isActive ? null : id)}
                    className="w-full text-left flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-sm font-medium text-gray-900">Record #{id}</span>
                      <span className="text-xs text-gray-500">Patient #{r.patientId}</span>
                      {r.asaGrade && <span className="text-xs px-2 py-0.5 bg-blue-100 text-blue-700 rounded">{r.asaGrade}</span>}
                    </div>
                    <svg className={`w-4 h-4 text-gray-400 transition-transform ${isActive ? "rotate-180" : ""}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
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
