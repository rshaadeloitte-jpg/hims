import type { ChargeValues } from "./zodSchema";
import type { ConcessionPlan, Package, SponsorCompany } from "./queries";

interface EstimationPreviewProps {
  patientId?: number;
  charges: ChargeValues[];
  isSelfCompany: boolean;
  sponsorCompanyId?: number;
  concessionPlans?: ConcessionPlan[];
  packages?: Package[];
  sponsors: SponsorCompany[];
}

/**
 * EstimationPreview — shows a real-time bill estimation before finalizing.
 * Vercel-style card layout with breakdown of charges, taxes, concessions.
 */
export function EstimationPreview({
  charges,
  isSelfCompany,
  sponsorCompanyId,
  packages = [],
  sponsors,
}: EstimationPreviewProps) {

  const subtotal = charges.reduce((s, c) => s + c.rate * c.qty, 0);
  const estimatedTax = subtotal * 0.05; // Default 5% tax estimate
  const estimatedTotal = subtotal + estimatedTax;

  return (
    <div className="estimation-preview">
      <div className="estimation-preview__header">
        <h3 className="estimation-preview__title">Bill Estimation</h3>
        <span className="estimation-preview__badge">Preview</span>
      </div>

      <div className="estimation-preview__body">
        {/* Charge summary */}
        <div className="estimation-preview__section">
          <h4 className="estimation-preview__section-title">Charge Summary</h4>
          <div className="estimation-preview__lines">
            {charges.map((charge, i) => (
              <div key={i} className="estimation-preview__line">
                <span className="estimation-preview__line-name">{charge.serviceName}</span>
                <span className="estimation-preview__line-qty">{charge.qty}x</span>
                <span className="estimation-preview__line-rate">Rs {charge.rate.toFixed(2)}</span>
                <span className="estimation-preview__line-total">Rs {(charge.rate * charge.qty).toFixed(2)}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Totals */}
        <div className="estimation-preview__section">
          <h4 className="estimation-preview__section-title">Estimated Total</h4>
          <div className="estimation-preview__totals">
            <div className="estimation-preview__total-row">
              <span>Subtotal</span>
              <span>Rs {subtotal.toFixed(2)}</span>
            </div>
            <div className="estimation-preview__total-row">
              <span>Estimated Tax (5%)</span>
              <span>Rs {estimatedTax.toFixed(2)}</span>
            </div>
            <div className="estimation-preview__total-row estimation-preview__total-row--grand">
              <span>Estimated Grand Total</span>
              <span>Rs {estimatedTotal.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Sponsor info */}
        {!isSelfCompany && sponsorCompanyId && (
          <div className="estimation-preview__section">
            <h4 className="estimation-preview__section-title">Sponsor Coverage</h4>
            <div className="estimation-preview__sponsor">
              <span className="estimation-preview__sponsor-name">
                {sponsors.find((s) => s.companyId === sponsorCompanyId)?.companyName ?? "Unknown"}
              </span>
              <span className="estimation-preview__sponsor-note">
                Coverage will be calculated at finalization
              </span>
            </div>
          </div>
        )}

        {/* Available packages */}
        {packages.length > 0 && (
          <div className="estimation-preview__section">
            <h4 className="estimation-preview__section-title">Available Packages</h4>
            <div className="estimation-preview__packages">
              {packages.slice(0, 3).map((pkg) => (
                <div key={pkg.packageId} className="estimation-preview__package">
                  <span className="estimation-preview__package-name">{pkg.packageName}</span>
                  <span className="estimation-preview__package-price">Rs {pkg.sellPrice.toFixed(2)}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
