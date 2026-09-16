import { useState } from "react";
import { Button, Textarea } from "@chakra-ui/react";
import { FormField, TextInput } from "../../../components/ui";
import {
  useAbhaForPatient,
  useConfirmAbhaLink,
  useScanAbhaQr,
  useStartAbhaEnrollment,
  useVerifyAbhaOtp,
} from "../api/queries";
import { PatientSearch } from "../components/PatientSearch";
import { PatientBanner } from "../components/PatientBanner";
import { AbhaStatusBadge } from "../components/StatusBadges";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { usePatient } from "../api/usePatient";
import type { AbhaEnrollment, AbhaStatus, PatientSearchHit } from "../api/types";

/**
 * AbhaPage — legacy `abha:*` mapped to React.
 *
 * Four flows are supported, matching the 07 §ABDM surface:
 *
 *  1. Aadhaar / Mobile OTP enrolment  (PENDING_OTP -> OTP_VERIFIED -> LINKED)
 *  2. QR code scan                     (PHR pull; the scanned PHR address is
 *                                       shown as a candidate for confirm-link)
 *  3. ABHA card display                (status-driven; the linked PHR address
 *                                       + a scannable QR payload)
 *  4. ABHA consent                     (confirm-link step that finalizes the
 *                                       address binding to the patient record)
 *
 * The flow uses the session patient (looked up via PatientSearch) as the
 * bind target. The session also has access to Fg.g()-style flags (isNhnc, duz)
 * but ABHA link does not depend on them.
 */
export function AbhaPage() {
  const [patient, setPatient] = useState<PatientSearchHit | null>(null);
  const { patient: fullPatient, loading: patientLoading } = usePatient(patient?.id);
  const enrollment = useAbhaForPatient(patient?.id);
  const start = useStartAbhaEnrollment();
  const verify = useVerifyAbhaOtp();
  const scan = useScanAbhaQr();
  const confirm = useConfirmAbhaLink();

  const [otp, setOtp] = useState<string>("");
  const [qrInput, setQrInput] = useState<string>("");
  const [showConsent, setShowConsent] = useState<boolean>(false);
  const [scannedAbhaAddress, setScannedAbhaAddress] = useState<string>("");

  const current: AbhaEnrollment | undefined = enrollment.data;
  const status: AbhaStatus = (current?.status as AbhaStatus) ?? "PENDING_OTP";

  const startAadhaar = (channel: "aadhaar" | "mobile") => {
    if (!patient) return;
    void start.mutateAsync({
      patientId: patient.id,
      ...(channel === "aadhaar" ? { aadhaar: "" } : { mobile: "" }),
    });
  };

  const submitOtp = () => {
    if (!current) return;
    void verify.mutateAsync({ enrollmentId: current.id, otp: otp.trim() });
  };

  const submitQr = () => {
    const payload = qrInput.trim();
    if (!payload) return;
    void scan
      .mutateAsync(payload)
      .then((enr) => {
        setScannedAbhaAddress(enr.abhaAddress ?? "");
        if (enr.patientId) setPatient({ ...(patient ?? {}), id: enr.patientId } as PatientSearchHit);
        setShowConsent(Boolean(enr.abhaAddress));
      });
  };

  const confirmLink = () => {
    if (!patient) return;
    void confirm
      .mutateAsync({ patientId: patient.id, abhaAddress: scannedAbhaAddress })
      .then(() => setShowConsent(false));
  };

  return (
    <div className="page page--abha">
      <header className="page-header">
        <div>
          <h1>ABHA / ABDM</h1>
          <p className="muted">
            Aadhaar/Mobile OTP enrolment, QR scan, consent, ABHA card display.
          </p>
        </div>
      </header>

      <section className="card" aria-label="Patient">
        <PatientSearch onPick={setPatient} autoFocus label="Find patient" />
      </section>

      {patient && fullPatient ? (
        <div className="card card--banner">
          <PatientBanner
            patient={fullPatient}
            rightSlot={<AbhaStatusBadge status={status} />}
          />
          {patientLoading ? <p className="muted small">Loading full record…</p> : null}
        </div>
      ) : null}

      {patient ? (
        <>
          <section className="card" aria-label="Aadhaar / Mobile OTP enrolment">
            <h2>1. Aadhaar / Mobile OTP enrolment</h2>
            <p className="muted small">
              Send a one-time-password to the patient's Aadhaar-linked mobile. The legacy webapp
              uses the same two channels; ABHA M1 (07 §ABDM) is implemented here.
            </p>
            <div className="form-row form-row--two-col">
              <Button
                type="button"
                variant="secondary"
                onClick={() => startAadhaar("aadhaar")}
                loading={start.isPending}
                loadingText="Sending…"
              >
                Send OTP to Aadhaar mobile
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={() => startAadhaar("mobile")}
                loading={start.isPending}
                loadingText="Sending…"
              >
                Send OTP to registered mobile
              </Button>
            </div>
            {current ? (
              <p className="muted small">
                Enrolment id <code>{current.id}</code> · status <AbhaStatusBadge status={status} />
              </p>
            ) : null}
            {status === "PENDING_OTP" && current ? (
              <div className="form-row form-row--two-col">
                <FormField label="Enter 6-digit OTP">
                  <TextInput
                    type="text"
                    inputMode="numeric"
                    pattern="\d{6}"
                    maxLength={6}
                    value={otp}
                    onChange={(e) => setOtp(e.target.value)}
                  />
                </FormField>
                <div className="field field--end">
                  <Button
                    type="button"
                    variant="primary"
                    onClick={submitOtp}
                    disabled={otp.length < 6}
                    loading={verify.isPending}
                    loadingText="Verifying…"
                  >
                    Verify OTP
                  </Button>
                </div>
              </div>
            ) : null}
          </section>

          <section className="card" aria-label="QR code scan">
            <h2>2. QR code scan (PHR pull)</h2>
            <p className="muted small">
              Paste the raw PHR address / QR payload below; the interop service will resolve
              it and offer the discovered ABHA address for confirm-link.
            </p>
            <Textarea
              bg="white"
              borderColor="gray.300"
              rows={3}
              value={qrInput}
              onChange={(e) => setQrInput(e.target.value)}
              placeholder="PHR payload (e.g. 'abha://...?token=...')"
            />
            <Button
              type="button"
              variant="primary"
              onClick={submitQr}
              disabled={!qrInput.trim()}
              loading={scan.isPending}
              loadingText="Scanning…"
            >
              Scan
            </Button>
          </section>

          {status === "LINKED" && current ? (
            <section className="card abha-card" aria-label="ABHA card">
              <h2>3. ABHA card</h2>
              <div className="abha-card__body">
                <div className="abha-card__ids">
                  <p className="abha-card__name">
                    {fullPatient ? `${fullPatient.firstName} ${fullPatient.lastName ?? ""}`.trim() : ""}
                  </p>
                  <p className="abha-card__num">ABHA: {current.abhaNumber ?? "—"}</p>
                  <p className="abha-card__addr muted">PHR: {current.abhaAddress ?? "—"}</p>
                </div>
                <div className="abha-card__qr" aria-label="ABHA QR code">
                  <pre>{current.qrPayload ?? "no QR payload"}</pre>
                </div>
              </div>
              <p className="muted small">
                Linked at {current.linkedAt ? new Date(current.linkedAt).toLocaleString() : "—"}
              </p>
            </section>
          ) : null}
        </>
      ) : null}

      <ConfirmDialog
        open={showConsent}
        title="Confirm ABHA address linkage"
        body={
          <p>
            Link ABHA address <code>{scannedAbhaAddress}</code> to the selected patient?
            This requires an explicit consent under ABDM.
          </p>
        }
        confirmLabel="Link ABHA"
        busy={confirm.isPending}
        onCancel={() => setShowConsent(false)}
        onConfirm={confirmLink}
      />
    </div>
  );
}
