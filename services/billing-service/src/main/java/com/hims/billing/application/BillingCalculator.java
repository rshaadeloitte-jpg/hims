package com.hims.billing.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure finalize-time calculation engine (no Spring, no persistence) so the money
 * rules are unit-testable in isolation.
 *
 * Per charge, with lineTotal = rate x qty (HALF_UP scale 2 everywhere unless noted):
 *
 * BR-BILL-005 asymmetric split (RISK R2 — pinned by dedicated tests):
 *   effectiveCoPay = coPayPct == 0 ? 100 : coPayPct   (deductable 0 => 100% self)
 *   selfBase     = lineTotal x effectiveCoPay / 100
 *   nonSelfBase  = lineTotal - selfBase
 *   conPct       = SELF company: planPct + billPct   (bill% ADDED to plan%)
 *                  NON-SELF:      billPct             (bill% REPLACES plan%)
 *   SELF path:     selfRate    = selfBase - selfBase x conPct / 100;  nonSelf = nonSelfBase
 *   NON-SELF path: nonSelfRate = nonSelfBase - nonSelfBase x conPct / 100; self = selfBase
 *   concession  = lineTotal - selfRate - nonSelfRate
 *
 * BR-BILL-004 taxes (up to 3, per charge tax_pct_a/b/c):
 *   taxBase = applyAfterConcession ? lineTotal - lineTotal x conPct / 100 : lineTotal
 *   taxN    = taxBase x pctN / 100 ; TaxAmount = sum(taxN) — added to the SELF side.
 *
 * BR-BILL-002 doctor share:
 *   skipped when doctorSharePct == 0 or isOtProcedure
 *   share    = lineTotal x pct / 100   CEILING scale 2   (CEILING for shares in DB)
 *   hospital = lineTotal - share       HALF_UP scale 2   (HALF_UP on the complement)
 */
public final class BillingCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;
    /**
     * Intermediate calculation scale (legacy uses 4-decimal precision internally).
     * Final results are rounded to MONEY_SCALE.
     */
    private static final int CALC_SCALE = 4;

    private BillingCalculator() {}

    /** Finalize-time money result for ONE charge line. */
    public record ChargeCalculation(
            BigDecimal lineTotal,
            BigDecimal selfRate,
            BigDecimal nonSelfRate,
            BigDecimal concession,
            BigDecimal taxA,
            BigDecimal taxB,
            BigDecimal taxC,
            BigDecimal taxTotal,
            BigDecimal selfAmount,       // selfRate + taxTotal (tax added to self side)
            BigDecimal nonSelfAmount,
            BigDecimal doctorShare,      // null when skipped (pct=0 or OT)
            BigDecimal hospitalShare) {  // null when skipped
    }

    public static ChargeCalculation calculate(BigDecimal rate, int qty,
                                              BigDecimal taxPctA, BigDecimal taxPctB, BigDecimal taxPctC,
                                              boolean applyAfterConcession,
                                              BigDecimal coPayPct,
                                              BigDecimal planPct, BigDecimal billPct,
                                              BigDecimal doctorSharePct, boolean isOtProcedure,
                                              boolean selfCompany) {
        BigDecimal lineTotal = money(rate).multiply(BigDecimal.valueOf(qty));

        // deductable 0 => treat as 100% self (BR-BILL-005)
        BigDecimal effectiveCoPay = nz(coPayPct).compareTo(BigDecimal.ZERO) == 0
                ? HUNDRED : nz(coPayPct);
        BigDecimal selfBase = pctOf(lineTotal, effectiveCoPay);
        BigDecimal nonSelfBase = lineTotal.subtract(selfBase);

        // BR-BILL-005 asymmetric concession combination (RISK R2):
        BigDecimal conPct = selfCompany
                ? nz(planPct).add(nz(billPct))      // SELF: plan% then bill% ADDED
                : nz(billPct);                       // NON-SELF: bill% REPLACES plan%

        BigDecimal selfRate;
        BigDecimal nonSelfRate;
        if (selfCompany) {
            selfRate = selfBase.subtract(pctOf(selfBase, conPct));
            nonSelfRate = nonSelfBase;
        } else {
            selfRate = selfBase;
            nonSelfRate = nonSelfBase.subtract(pctOf(nonSelfBase, conPct));
        }

        // BR-BILL-004: tax base honours applyAfterConcession with the SAME conPct mode
        BigDecimal taxBase = applyAfterConcession
                ? lineTotal.subtract(pctOf(lineTotal, conPct))
                : lineTotal;
        BigDecimal taxA = pctOf(taxBase, nz(taxPctA));
        BigDecimal taxB = pctOf(taxBase, nz(taxPctB));
        BigDecimal taxC = pctOf(taxBase, nz(taxPctC));
        BigDecimal taxTotal = taxA.add(taxB).add(taxC);

        BigDecimal concession = lineTotal.subtract(selfRate).subtract(nonSelfRate);
        BigDecimal selfAmount = selfRate.add(taxTotal);   // TaxAmount added to self side

        // BR-BILL-002: skip when pct=0 or OT procedure; CEILING share / HALF_UP complement
        BigDecimal doctorShare = null;
        BigDecimal hospitalShare = null;
        if (nz(doctorSharePct).compareTo(BigDecimal.ZERO) > 0 && !isOtProcedure) {
            doctorShare = lineTotal.multiply(nz(doctorSharePct))
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.CEILING);
            hospitalShare = lineTotal.subtract(doctorShare);   // exact at scale 2
        }

        return new ChargeCalculation(lineTotal, selfRate, nonSelfRate, concession,
                taxA, taxB, taxC, taxTotal, selfAmount, nonSelfRate, doctorShare, hospitalShare);
    }

    /** pct% of base, using intermediate CALC_SCALE precision (legacy 4-decimal). */
    static BigDecimal pctOf(BigDecimal base, BigDecimal pct) {
        return base.multiply(pct).divide(HUNDRED, CALC_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
