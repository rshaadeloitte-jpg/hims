import { useRef } from "react";
import { useDeathCertificate, useRequestCertificatePrint } from "./queries";
import { deathCertificateRequestSchema } from "./zodSchema";

/**
 * M28 DeathCertificate — print-ready certificate.
 *
 * Apple clean white-space layout:
 *   - Generous padding / line-height
 *   - Serif typography for formal document feel
 *   - Ornate but minimal border
 *   - Centered, single-column layout for print
 *
 * @media print strips all chrome and renders only the certificate.
 * Ported from `death_Certificate.vm` template.
 */

interface DeathCertificateProps {
  patientId: number;
}

export function DeathCertificate({ patientId }: DeathCertificateProps) {
  const { data: cert, isLoading, isError } = useDeathCertificate(patientId);
  const printCert = useRequestCertificatePrint();
  const printRef = useRef<HTMLDivElement>(null);

  const handlePrint = () => {
    // Persist cert number first, then open print dialog.
    if (cert) {
      const parse = deathCertificateRequestSchema.safeParse({
        patientId: cert.patientId,
        certNumber: cert.certNumber,
        placeOfDeath: cert.placeOfDeath,
      });
      if (parse.success) {
        printCert.mutate(parse.data, { onSuccess: () => window.print() });
      }
    }
  };

  if (isLoading) return <p className="muted">Loading certificate…</p>;
  if (isError || !cert) return <p className="error">Could not load death certificate.</p>;

  return (
    <section className="page">
      <header className="page-header no-print">
        <h1>Death Certificate</h1>
        <button className="btn btn-primary" onClick={handlePrint} disabled={printCert.isPending}>
          {printCert.isPending ? "Preparing…" : "Print Certificate"}
        </button>
      </header>

      <div ref={printRef} className="death-certificate">
        <div className="death-certificate__border">
          <header className="death-certificate__header">
            <div className="death-certificate__emblem">★</div>
            <p className="death-certificate__jurisdiction">
              DEATH CERTIFICATE
            </p>
            <h1 className="death-certificate__hospital">{cert.hospitalName}</h1>
            <p className="death-certificate__subtitle">
              (Issued under the Births and Deaths Registration Act)
            </p>
          </header>

          <hr className="death-certificate__divider" />

          <dl className="death-certificate__grid">
            <dt>Certificate No.</dt>
            <dd className="death-certificate__mono">{cert.certNumber}</dd>

            <dt>Name of Deceased</dt>
            <dd className="death-certificate__value">{cert.patientName}</dd>

            <dt>Age / Gender</dt>
            <dd>
              {cert.age ?? "—"} / {cert.gender ?? "—"}
            </dd>

            <dt>Date of Death</dt>
            <dd>{formatLongDate(cert.deathDate)}</dd>

            <dt>Place of Death</dt>
            <dd>{cert.placeOfDeath || cert.hospitalName}</dd>

            <dt>Cause of Death</dt>
            <dd className="death-certificate__value">{cert.causeOfDeath}</dd>

            <dt>Death Type</dt>
            <dd>{labelDeathType(cert.deathType)}</dd>

            <dt>Certified By</dt>
            <dd>{cert.certifiedBy}</dd>

            <dt>Date of Issue</dt>
            <dd>{formatLongDate(cert.printedAt)}</dd>
          </dl>

          <hr className="death-certificate__divider" />

          <footer className="death-certificate__footer">
            <div className="death-certificate__signature-block">
              <div className="death-certificate__signature-line" />
              <p>Signature of Certifying Authority</p>
            </div>
            <div className="death-certificate__signature-block">
              <div className="death-certificate__signature-line" />
              <p>Seal / Stamp</p>
            </div>
          </footer>

          <p className="death-certificate__note">
            This certificate is issued as a legal document. Any falsification is
            punishable under law.
          </p>
        </div>
      </div>

      <style>{PRINT_CSS}</style>
    </section>
  );
}

// ---- Helpers ----------------------------------------------------------------

function formatLongDate(iso: string): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  });
}

function labelDeathType(type: string): string {
  const labels: Record<string, string> = {
    NATURAL: "Natural",
    ACCIDENT: "Accident",
    SUICIDE: "Suicide",
    HOMICIDE: "Homicide / MLR",
    PENDING: "Pending Investigation",
    OTHER: "Other",
  };
  return labels[type] ?? type;
}

// ---- Print styles (Apple clean white-space) ---------------------------------

const PRINT_CSS = `
  @media print {
    @page {
      size: A4 portrait;
      margin: 12mm 14mm;
    }

    body {
      background: #ffffff !important;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }

    .no-print,
    .app-header,
    .app-footer,
    .page-header,
    nav {
      display: none !important;
    }

    .page {
      margin: 0 !important;
      padding: 0 !important;
      max-width: none !important;
    }

    .death-certificate {
      box-shadow: none !important;
      margin: 0 !important;
    }

    .death-certificate__border {
      border: 2px solid #000 !important;
      padding: 18mm 20mm !important;
    }

    .death-certificate {
      page-break-inside: avoid;
      break-inside: avoid;
    }

    /* Ensure footer signatures don't split across pages */
    .death-certificate__footer {
      page-break-inside: avoid;
      break-inside: avoid;
    }

    .death-certificate__note {
      page-break-inside: avoid;
      break-inside: avoid;
    }
  }

  /* Screen + print shared: Apple clean white-space */
  .death-certificate {
    max-width: 210mm;
    margin: 0 auto;
    background: #ffffff;
    color: #000;
    font-family: 'Georgia', 'Times New Roman', serif;
    line-height: 1.7;
    font-size: 12pt;
  }

  .death-certificate__border {
    border: 2px solid #000;
    padding: 14mm 18mm;
    background: #fff;
  }

  .death-certificate__header {
    text-align: center;
    margin-bottom: 1.2rem;
  }

  .death-certificate__emblem {
    font-size: 2rem;
    color: #444;
    letter-spacing: 0.5em;
    margin-bottom: 0.3rem;
  }

  .death-certificate__jurisdiction {
    font-size: 0.7rem;
    letter-spacing: 0.3em;
    text-transform: uppercase;
    color: #555;
    margin: 0;
  }

  .death-certificate__hospital {
    font-size: 1.5rem;
    margin: 0.4rem 0 0.2rem;
    font-weight: 700;
    letter-spacing: 0.04em;
  }

  .death-certificate__subtitle {
    font-size: 0.8rem;
    color: #666;
    margin: 0;
  }

  .death-certificate__divider {
    border: none;
    border-top: 1px solid #000;
    margin: 1rem 0;
  }

  .death-certificate__grid {
    display: grid;
    grid-template-columns: 140px 1fr;
    gap: 0.9rem 1.2rem;
    margin: 0;
  }

  .death-certificate__grid dt {
    font-weight: 600;
    font-size: 0.8rem;
    text-transform: uppercase;
    letter-spacing: 0.08em;
    color: #444;
  }

  .death-certificate__grid dd {
    margin: 0;
    font-size: 1.05rem;
  }

  .death-certificate__value {
    font-weight: 700;
  }

  .death-certificate__mono {
    font-family: 'SF Mono', 'Menlo', 'Consolas', monospace;
    font-size: 1rem;
    letter-spacing: 0.06em;
  }

  .death-certificate__footer {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 3rem;
    margin-top: 3rem;
    text-align: center;
  }

  .death-certificate__signature-block p {
    margin: 0;
    font-size: 0.8rem;
    color: #555;
  }

  .death-certificate__signature-line {
    height: 1px;
    background: #000;
    margin-bottom: 0.4rem;
  }

  .death-certificate__note {
    margin-top: 2rem;
    text-align: center;
    font-size: 0.75rem;
    color: #666;
    font-style: italic;
  }
`;
