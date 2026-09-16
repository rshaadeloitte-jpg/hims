import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { Button, Text } from "@chakra-ui/react";
import { FormField, HimsSelect, TextInput } from "../../../components/ui";
import { useIssueToken, useRegisterPatient } from "../api/queries";
import { useOpSession } from "../store/opSession";
import { PatientSearch } from "../components/PatientSearch";
import { PatientBanner } from "../components/PatientBanner";
import { MlcDetailsSection } from "../components/MlcDetailsSection";
import { QueuePriorityBadge, QueueStatusBadge } from "../components/StatusBadges";
import type { Patient, PatientSearchHit, PatientSponsor, QueueToken } from "../api/types";

/**
 * WalkinVisitPage — legacy `walkinVisit:addwalkinVisit` mapped to React.
 *
 *  Step 1  Patient search (PatientSearch) — if hit: pick + show form
 *                                          with existing patient pre-filled.
 *  Step 2  Demographic form (RHF + zod) — only shown when a patient is
 *                                          selected (existing) or "register new" clicked.
 *  Step 3  MLC / FIR details (MlcDetailsSection) — toggleable.
 *  Step 4  Sponsor / scheme picker
 *  Step 5  Issue queue token on submit, then navigate to appointment list.
 *
 * Session state is persisted to localStorage (Zustand persist) so the form
 * draft survives page refresh — the legacy webapp used Fg.g() cookies for
 * the same purpose (09 §1, §2.1).
 */

const sexEnum = z.enum(["M", "F", "O", "U"]);
const SEX_LABELS: Record<z.infer<typeof sexEnum>, string> = {
  M: "Male",
  F: "Female",
  O: "Other",
  U: "Unknown",
};

const demographicsSchema = z
  .object({
    firstName: z.string().min(1, "First name is required").max(80),
    middleName: z.string().max(80).optional().or(z.literal("")),
    lastName: z.string().min(1, "Last name is required").max(80),
    fatherName: z.string().max(120).optional().or(z.literal("")),
    motherName: z.string().max(120).optional().or(z.literal("")),
    dateOfBirth: z.string().optional().or(z.literal("")),
    age: z.string().optional().or(z.literal("")),
    sex: sexEnum,
    mobile: z.string().regex(/^$|^[+]?[\d\s-]{7,15}$/, "Mobile looks invalid").optional().or(z.literal("")),
    aadhaar: z.string().regex(/^$|^\d{12}$/, "Aadhaar is 12 digits").optional().or(z.literal("")),
  })
  .superRefine((d, ctx) => {
    if (!d.dateOfBirth && !d.age) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["age"], message: "Either DOB or age is required" });
    }
  });

type Demographics = z.infer<typeof demographicsSchema>;

const mlcSchema = z.object({
  enabled: z.boolean(),
  mlcNumber: z.string().optional().or(z.literal("")),
  mlcFirNumber: z.string().optional().or(z.literal("")),
  mlcPoliceStation: z.string().optional().or(z.literal("")),
  mlcNotes: z.string().optional().or(z.literal("")),
});
const fullSchema = z
  .object({ demographics: demographicsSchema, mlc: mlcSchema })
  .superRefine((v, ctx) => {
    if (v.mlc.enabled) {
      if (!v.mlc.mlcNumber || !v.mlc.mlcNumber.trim()) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["mlc", "mlcNumber"], message: "MLC number is required" });
      }
      if (!v.mlc.mlcFirNumber || !v.mlc.mlcFirNumber.trim()) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ["mlc", "mlcFirNumber"], message: "FIR number is required" });
      }
    }
  });

type FullForm = z.infer<typeof fullSchema>;

const EMPTY_DEMOGRAPHICS: Demographics = {
  firstName: "",
  middleName: "",
  lastName: "",
  fatherName: "",
  motherName: "",
  dateOfBirth: "",
  age: "",
  sex: "U",
  mobile: "",
  aadhaar: "",
};

export function WalkinVisitPage() {
  const navigate = useNavigate();
  const { walkinDraft, saveWalkinDraft, clearWalkinDraft, defaultCounterId } = useOpSession();
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(null);
  const [isNewPatient, setIsNewPatient] = useState<boolean>(false);
  const [showTokenDialog, setShowTokenDialog] = useState<boolean>(false);
  const [lastIssuedToken, setLastIssuedToken] = useState<QueueToken | null>(null);

  const register = useRegisterPatient();
  const issueToken = useIssueToken();

  const form = useForm<FullForm>({
    resolver: zodResolver(fullSchema),
    defaultValues: {
      demographics: EMPTY_DEMOGRAPHICS,
      mlc: { enabled: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" },
    },
    mode: "onBlur",
  });

  // Hydrate from session draft once on mount
  useEffect(() => {
    if (!walkinDraft) return;
    form.reset({
      demographics: {
        firstName: walkinDraft.firstName ?? "",
        middleName: walkinDraft.middleName ?? "",
        lastName: walkinDraft.lastName ?? "",
        fatherName: walkinDraft.fatherName ?? "",
        motherName: walkinDraft.motherName ?? "",
        dateOfBirth: walkinDraft.dateOfBirth ?? "",
        age: walkinDraft.age ?? "",
        sex: (walkinDraft.sex as Demographics["sex"]) ?? "U",
        mobile: walkinDraft.mobile ?? "",
        aadhaar: walkinDraft.aadhaar ?? "",
      },
      mlc: {
        enabled: Boolean(walkinDraft.mlcNumber || walkinDraft.mlcFirNumber),
        mlcNumber: walkinDraft.mlcNumber ?? "",
        mlcFirNumber: walkinDraft.mlcFirNumber ?? "",
        mlcPoliceStation: walkinDraft.mlcPoliceStation ?? "",
        mlcNotes: walkinDraft.mlcNotes ?? "",
      },
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Mirror watch back to the session draft so refresh keeps the form filled
  useEffect(() => {
    const sub = form.watch((v) => {
      const d = v.demographics ?? EMPTY_DEMOGRAPHICS;
      const m = v.mlc ?? { enabled: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" };
      saveWalkinDraft({
        firstName: d.firstName, middleName: d.middleName, lastName: d.lastName,
        fatherName: d.fatherName, motherName: d.motherName,
        dateOfBirth: d.dateOfBirth, age: d.age, sex: d.sex,
        mobile: d.mobile, aadhaar: d.aadhaar,
        mlcNumber: m.mlcNumber, mlcFirNumber: m.mlcFirNumber,
        mlcPoliceStation: m.mlcPoliceStation, mlcNotes: m.mlcNotes,
      });
    });
    return () => sub.unsubscribe();
  }, [form, saveWalkinDraft]);

  const pickExisting = (hit: PatientSearchHit) => {
    setSelectedPatientId(hit.id);
    setIsNewPatient(false);
    form.reset({
      demographics: {
        firstName: hit.firstName,
        middleName: "",
        lastName: (hit as { lastName?: string }).lastName ?? "",
        fatherName: "", motherName: "",
        dateOfBirth: hit.dateOfBirth ?? "", age: hit.age != null ? String(hit.age) : "",
        sex: hit.sex, mobile: hit.mobile ?? "", aadhaar: hit.abha ?? "",
      },
      mlc: { enabled: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" },
    });
  };

  const pickNew = () => {
    setSelectedPatientId(null);
    setIsNewPatient(true);
    form.reset({ demographics: EMPTY_DEMOGRAPHICS, mlc: { enabled: false, mlcNumber: "", mlcFirNumber: "", mlcPoliceStation: "", mlcNotes: "" } });
  };

  const onMlcChange = (next: { mlcNumber?: string; mlcFirNumber?: string; mlcPoliceStation?: string; mlcNotes?: string }) => {
    form.setValue("mlc.mlcNumber", next.mlcNumber ?? "", { shouldValidate: true });
    form.setValue("mlc.mlcFirNumber", next.mlcFirNumber ?? "", { shouldValidate: true });
    form.setValue("mlc.mlcPoliceStation", next.mlcPoliceStation ?? "", { shouldValidate: true });
    form.setValue("mlc.mlcNotes", next.mlcNotes ?? "", { shouldValidate: true });
  };

  const submit = form.handleSubmit(async (values) => {
    const demo = values.demographics;
    let patientId: number;
    if (isNewPatient || selectedPatientId == null) {
      const created = await register.mutateAsync({
        firstName: demo.firstName,
        middleName: demo.middleName || undefined,
        lastName: demo.lastName,
        fatherName: demo.fatherName || undefined,
        motherName: demo.motherName || undefined,
        dateOfBirth: demo.dateOfBirth || undefined,
        age: demo.age ? Number(demo.age) : undefined,
        sex: demo.sex,
        mobile: demo.mobile || undefined,
        aadhaar: demo.aadhaar || undefined,
        mlcNumber: values.mlc.enabled ? values.mlc.mlcNumber : undefined,
        mlcFirNumber: values.mlc.enabled ? values.mlc.mlcFirNumber : undefined,
        mlcPoliceStation: values.mlc.enabled ? values.mlc.mlcPoliceStation : undefined,
        mlcNotes: values.mlc.enabled ? values.mlc.mlcNotes : undefined,
      });
      patientId = created.id;
    } else {
      patientId = selectedPatientId;
    }
    const tok = await issueToken.mutateAsync({
      patientId,
      counterId: defaultCounterId ?? undefined,
      priority: "NORMAL",
    });
    setLastIssuedToken(tok);
    setShowTokenDialog(true);
    clearWalkinDraft();
  });

  return (
    <div className="page page--walkin">
      <header className="page-header">
        <div>
          <h1>Walk-in Visit</h1>
          <p className="muted">Register a new patient or pick an existing one, then issue a queue token.</p>
        </div>
      </header>

      <section className="card" aria-label="Patient lookup">
        <PatientSearch onPick={pickExisting} onRegisterNew={pickNew} autoFocus label="Find patient" />
        {isNewPatient ? (
          <p className="walkin-hint">
            <span className="badge badge--info">NEW PATIENT</span> Fill the demographic form below — MRN
            is generated server-side on submit.
          </p>
        ) : null}
      </section>

      {selectedPatientId != null && !isNewPatient ? <ExistingPatientBanner patientId={selectedPatientId} /> : null}

      {isNewPatient || selectedPatientId != null ? (
        <form className="card walkin-form" onSubmit={submit} noValidate>
          <section className="walkin-form__section" aria-label="Demographics">
            <h2>Demographics</h2>
            <div className="form-row form-row--two-col">
              <FormField label="First name" required error={form.formState.errors.demographics?.firstName?.message}>
                <TextInput {...form.register("demographics.firstName")} />
              </FormField>
              <FormField label="Middle name" error={form.formState.errors.demographics?.middleName?.message}>
                <TextInput {...form.register("demographics.middleName")} />
              </FormField>
            </div>
            <div className="form-row form-row--two-col">
              <FormField label="Last name" required error={form.formState.errors.demographics?.lastName?.message}>
                <TextInput {...form.register("demographics.lastName")} />
              </FormField>
              <FormField label="Father / Guardian" error={form.formState.errors.demographics?.fatherName?.message}>
                <TextInput {...form.register("demographics.fatherName")} />
              </FormField>
            </div>
            <div className="form-row form-row--two-col">
              <FormField label="Mother" error={form.formState.errors.demographics?.motherName?.message}>
                <TextInput {...form.register("demographics.motherName")} />
              </FormField>
              <FormField label="Date of birth" helper="YYYY-MM-DD" error={form.formState.errors.demographics?.dateOfBirth?.message}>
                <TextInput type="date" {...form.register("demographics.dateOfBirth")} />
              </FormField>
            </div>
            <div className="form-row form-row--three-col">
              <FormField label="Age (if DOB unknown)" error={form.formState.errors.demographics?.age?.message}>
                <TextInput type="number" min={0} max={130} {...form.register("demographics.age")} />
              </FormField>
              <FormField label="Sex" required error={form.formState.errors.demographics?.sex?.message}>
                <HimsSelect {...form.register("demographics.sex")}>
                  {Object.entries(SEX_LABELS).map(([v, l]) => (
                    <option key={v} value={v}>{l}</option>
                  ))}
                </HimsSelect>
              </FormField>
              <FormField label="Mobile" error={form.formState.errors.demographics?.mobile?.message}>
                <TextInput type="tel" {...form.register("demographics.mobile")} />
              </FormField>
            </div>
            <div className="form-row form-row--two-col">
              <FormField label="Aadhaar (12 digits)" helper="Optional" error={form.formState.errors.demographics?.aadhaar?.message}>
                <TextInput inputMode="numeric" pattern="\d{12}" {...form.register("demographics.aadhaar")} />
              </FormField>
              <div className="field">
                <Text fontSize="sm" fontWeight="medium">Walk-in date</Text>
                <p className="form-readonly">{new Date().toLocaleDateString()}</p>
              </div>
            </div>
          </section>

          <section className="walkin-form__section" aria-label="MLC and sponsor">
            <h2>MLC / Sponsor</h2>
            <MlcDetailsSection
              value={{
                mlcNumber: form.watch("mlc.mlcNumber"),
                mlcFirNumber: form.watch("mlc.mlcFirNumber"),
                mlcPoliceStation: form.watch("mlc.mlcPoliceStation"),
                mlcNotes: form.watch("mlc.mlcNotes"),
              }}
              onChange={onMlcChange}
              required={form.watch("mlc.enabled")}
              onRequiredChange={(v) => form.setValue("mlc.enabled", v, { shouldValidate: true })}
            />
            <SponsorPicker patientId={selectedPatientId} isNew={isNewPatient} />
          </section>

          <footer className="walkin-form__actions">
            <Button type="button" variant="secondary" onClick={() => navigate(-1)} disabled={register.isPending || issueToken.isPending}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={register.isPending || issueToken.isPending}
              loadingText="Working…"
            >
              Register & Issue Token
            </Button>
          </footer>
        </form>
      ) : null}

      <TokenIssuedDialog
        open={showTokenDialog}
        token={lastIssuedToken}
        onClose={() => {
          setShowTokenDialog(false);
          navigate("/m/op/appointmentList");
        }}
      />
    </div>
  );
}

function ExistingPatientBanner({ patientId }: { patientId: number }) {
  const [patient, setPatient] = useState<Patient | null>(null);
  useEffect(() => {
    let cancelled = false;
    void (async () => {
      try {
        const res = await fetch(`/api/v1/patients/${patientId}`);
        if (!res.ok) return;
        const data = (await res.json()) as Patient;
        if (!cancelled) setPatient(data);
      } catch {
        /* ignore */
      }
    })();
    return () => { cancelled = true; };
  }, [patientId]);
  if (!patient) return null;
  return (
    <div className="card card--banner">
      <PatientBanner patient={patient} />
    </div>
  );
}

function SponsorPicker({ patientId, isNew }: { patientId: number | null; isNew: boolean }) {
  const [sponsors, setSponsors] = useState<PatientSponsor[] | null>(null);
  useEffect(() => {
    if (isNew || patientId == null) { setSponsors([]); return; }
    let cancelled = false;
    void (async () => {
      try {
        const res = await fetch(`/api/v1/patients/${patientId}/sponsors`);
        if (!res.ok) { if (!cancelled) setSponsors([]); return; }
        const data = (await res.json()) as PatientSponsor[];
        if (!cancelled) setSponsors(data ?? []);
      } catch { if (!cancelled) setSponsors([]); }
    })();
    return () => { cancelled = true; };
  }, [patientId, isNew]);

  const [selected, setSelected] = useState<string>("self-pay");

  return (
    <div className="sponsor-picker">
      <FormField
        label="Sponsor / scheme"
        helper={
          isNew
            ? "Sponsor can be attached after the patient is created (no sponsors yet on a new record)."
            : "Self-pay is the default; pick a sponsor/scheme to route the bill to that party."
        }
      >
        <HimsSelect value={selected} onChange={(e) => setSelected(e.target.value)}>
          <option value="self-pay">Self-pay (no sponsor)</option>
          {(sponsors ?? []).map((s) => (
            <option key={s.sponsorId} value={String(s.sponsorId)}>
              {(s.sponsorName ?? s.sponsorCode ?? "Sponsor") + (s.schemeName ? ` — ${s.schemeName}` : "")}
            </option>
          ))}
        </HimsSelect>
      </FormField>
    </div>
  );
}

function TokenIssuedDialog({ open, token, onClose }: { open: boolean; token: QueueToken | null; onClose: () => void }) {
  if (!open) return null;
  return (
    <div className="dialog-backdrop" role="presentation" onClick={onClose}>
      <div className="dialog" role="dialog" aria-modal="true" aria-labelledby="token-issued-title" onClick={(e) => e.stopPropagation()}>
        <h2 id="token-issued-title">Token issued</h2>
        {token ? (
          <>
            <p>Patient enqueued as <strong>{token.tokenNumber}</strong>.</p>
            <p>Status: <QueueStatusBadge status={token.status} /> · Priority: <QueuePriorityBadge priority={token.priority} /></p>
            {token.counterName ? <p>Counter: {token.counterName}</p> : null}
          </>
        ) : null}
        <div className="dialog__actions">
          <Button type="button" variant="primary" onClick={onClose}>Continue</Button>
        </div>
      </div>
    </div>
  );
}
