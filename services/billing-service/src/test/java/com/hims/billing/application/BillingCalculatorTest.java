package com.hims.billing.application;

import com.hims.billing.application.BillingCalculator.ChargeCalculation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L1 — money-rule pins (GAP-REGISTER WP-11 "MUST pin"):
 *   BR-BILL-005 asymmetry BOTH paths (SELF adds bill% to plan%; NON-SELF replaces),
 *   deductable-0 => 100% self,
 *   BR-BILL-004 tax before/after concession,
 *   BR-BILL-002 rounding (CEILING share, HALF_UP complement) and skip rules.
 */
class BillingCalculatorTest {

    private static BigDecimal d(String v) { return new BigDecimal(v); }

    private static ChargeCalculation calc(String rate, int qty,
            String taxA, String taxB, String taxC, boolean afterConc,
            String coPay, String plan, String bill, String sharePct, boolean ot, boolean selfCo) {
        return BillingCalculator.calculate(d(rate), qty,
                taxA == null ? BigDecimal.ZERO : d(taxA),
                taxB == null ? BigDecimal.ZERO : d(taxB),
                taxC == null ? BigDecimal.ZERO : d(taxC),
                afterConc, d(coPay), d(plan), d(bill), d(sharePct), ot, selfCo);
    }

    // ---------------------------------------------------------- BR-BILL-005 SELF

    @Test
    void brBill005_selfCompany_concessionPctIsPlanPlusBill_addedOnSelfRate() {
        // rate 1000 qty 1, coPay 40 -> selfBase 400, nonSelf 600.
        // SELF path: conPct = 10 + 5 = 15 (ADDED) -> selfRate = 400 - 60 = 340.
        ChargeCalculation c = calc("1000", 1, null, null, null, false,
                "40", "10", "5", "0", false, true);
        assertThat(c.lineTotal()).isEqualByComparingTo("1000.00");
        assertThat(c.selfRate()).isEqualByComparingTo("340.00");      // plan% + bill%
        assertThat(c.nonSelfRate()).isEqualByComparingTo("600.00");   // untouched on SELF path
        assertThat(c.concession()).isEqualByComparingTo("60.00");
    }

    @Test
    void brBill005_nonSelfCompany_billPctReplacesPlan_onNonSelfRate() {
        // same inputs, NON-SELF path: conPct = 5 (bill% REPLACES plan%) applied to nonSelf:
        // nonSelfRate = 600 - 600*5/100 = 570; selfRate stays 400.
        ChargeCalculation c = calc("1000", 1, null, null, null, false,
                "40", "10", "5", "0", false, false);
        assertThat(c.selfRate()).isEqualByComparingTo("400.00");      // untouched on NON-SELF path
        assertThat(c.nonSelfRate()).isEqualByComparingTo("570.00");   // plan% 10 IGNORED (replaced)
        assertThat(c.concession()).isEqualByComparingTo("30.00");
    }

    @Test
    void brBill005_asymmetryDivergence_sameInputsDifferentPaths() {
        // THE RISK R2 pin: identical inputs, only the company path differs -> different money.
        ChargeCalculation self = calc("2000", 1, null, null, null, false,
                "50", "20", "10", "0", false, true);
        ChargeCalculation nonSelf = calc("2000", 1, null, null, null, false,
                "50", "20", "10", "0", false, false);
        // SELF: conPct 30 on selfBase 1000 -> self 700.00, nonSelf 1000.00
        assertThat(self.selfRate()).isEqualByComparingTo("700.00");
        assertThat(self.nonSelfRate()).isEqualByComparingTo("1000.00");
        // NON-SELF: conPct 10 on nonSelfBase 1000 -> self 1000.00, nonSelf 900.00
        assertThat(nonSelf.selfRate()).isEqualByComparingTo("1000.00");
        assertThat(nonSelf.nonSelfRate()).isEqualByComparingTo("900.00");
        // divergence is real, not accidental
        assertThat(self.selfRate()).isNotEqualByComparingTo(nonSelf.selfRate());
        assertThat(self.nonSelfRate()).isNotEqualByComparingTo(nonSelf.nonSelfRate());
    }

    @Test
    void brBill005_deductableZeroIsFullSelf() {
        // coPay 0 => effective 100: selfBase = 500, nonSelfBase = 0.
        ChargeCalculation selfPath = calc("500", 1, null, null, null, false,
                "0", "10", "10", "0", false, true);
        assertThat(selfPath.selfRate()).isEqualByComparingTo("400.00");  // 500 - 500*20/100
        assertThat(selfPath.nonSelfRate()).isEqualByComparingTo("0.00");

        ChargeCalculation nonSelfPath = calc("500", 1, null, null, null, false,
                "0", "10", "10", "0", false, false);
        assertThat(nonSelfPath.selfRate()).isEqualByComparingTo("500.00");   // full self
        assertThat(nonSelfPath.nonSelfRate()).isEqualByComparingTo("0.00");  // nothing non-self to concede
    }

    // ---------------------------------------------------------- BR-BILL-004

    @Test
    void brBill004_taxBeforeConcession_baseIsLineTotal() {
        // rate 1000 qty 1, coPay 40, no concession, taxes 9 + 9 -> taxTotal 180 added to self side.
        ChargeCalculation c = calc("1000", 1, "9", "9", null, false,
                "40", "0", "0", "0", false, true);
        assertThat(c.taxA()).isEqualByComparingTo("90.00");
        assertThat(c.taxB()).isEqualByComparingTo("90.00");
        assertThat(c.taxTotal()).isEqualByComparingTo("180.00");
        assertThat(c.selfAmount()).isEqualByComparingTo("580.00");  // selfRate 400 + 180
        assertThat(c.nonSelfAmount()).isEqualByComparingTo("600.00");
    }

    @Test
    void brBill004_taxAfterConcession_usesSameConcessionMode() {
        // SELF path: conPct = 10+5 = 15 -> tax base 1000-150 = 850 -> 9% = 76.50.
        ChargeCalculation self = calc("1000", 1, "9", null, null, true,
                "100", "10", "5", "0", false, true);
        assertThat(self.taxTotal()).isEqualByComparingTo("76.50");

        // NON-SELF path: conPct = 5 (replaces) -> tax base 1000-50 = 950 -> 9% = 85.50.
        ChargeCalculation nonSelf = calc("1000", 1, "9", null, null, true,
                "100", "10", "5", "0", false, false);
        assertThat(nonSelf.taxTotal()).isEqualByComparingTo("85.50");
    }

    @Test
    void brBill004_noTaxPct_zeroTax() {
        ChargeCalculation c = calc("250", 2, null, null, null, false,
                "0", "0", "0", "0", false, true);
        assertThat(c.taxTotal()).isEqualByComparingTo("0.00");
        assertThat(c.lineTotal()).isEqualByComparingTo("500.00");
    }

    // ---------------------------------------------------------- BR-BILL-002

    @Test
    void brBill002_shareCeiling_hospitalHalfUpComplement() {
        // 1000 * 33.33% = 333.30 exact; use 7% of 100 -> 7.00; pick a value that forces CEILING:
        // 333 * 3.33% = 11.0889 -> CEILING 11.09 (HALF_UP would give 11.09 too, so use .0881):
        // 100 * 3.331% is not representable at pct scale 2 — use rate 13.33 qty 1, 33.33%:
        // 13.33 * 0.3333 = 4.4432889 -> CEILING scale2 = 4.45 (HALF_UP = 4.44) — divergence pinned.
        ChargeCalculation c = calc("13.33", 1, null, null, null, false,
                "0", "0", "0", "33.33", false, true);
        assertThat(c.doctorShare()).isEqualByComparingTo("4.45");      // CEILING
        assertThat(c.hospitalShare()).isEqualByComparingTo("8.88");    // 13.33 - 4.45
    }

    @Test
    void brBill002_skippedWhenPctZero() {
        ChargeCalculation c = calc("100", 1, null, null, null, false,
                "0", "0", "0", "0", false, true);
        assertThat(c.doctorShare()).isNull();
        assertThat(c.hospitalShare()).isNull();
    }

    @Test
    void brBill002_skippedWhenOtProcedure_evenWithPctAboveZero() {
        ChargeCalculation c = calc("1000", 1, null, null, null, false,
                "0", "0", "0", "25", true, true);
        assertThat(c.doctorShare()).isNull();
        assertThat(c.hospitalShare()).isNull();
    }

    @Test
    void brBill002_shareAppliesToFullLineTotal_regardlessOfSplit() {
        // share is computed on totalAmount (lineTotal), not on the self portion.
        ChargeCalculation c = calc("1000", 1, null, null, null, false,
                "40", "0", "0", "10", false, true);
        assertThat(c.doctorShare()).isEqualByComparingTo("100.00");   // 10% of 1000
        assertThat(c.hospitalShare()).isEqualByComparingTo("900.00");
    }
}
