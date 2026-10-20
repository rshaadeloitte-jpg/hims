package com.hims.appointment.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L0 — VisitStatus 11/12 split-brain resolution rules with the exact legacy
 * guards (BR-OPD-007 addBillPayment / BR-PHAR-001 dispenseItemSale).
 */
class LegacyVisitStatusSplitBrainTest {

    @Test
    void billPayment_advancesTo11_onlyIfBelow() {
        assertThat(LegacyVisitStatus.advanceBillPayment(null)).isEqualTo((short) 11);   // first write
        assertThat(LegacyVisitStatus.advanceBillPayment((short) 5)).isEqualTo((short) 11);   // legacy <11 advance
        assertThat(LegacyVisitStatus.advanceBillPayment((short) 10)).isEqualTo((short) 11);
        assertThat(LegacyVisitStatus.advanceBillPayment((short) 11)).isEqualTo((short) 11);  // idempotent
        assertThat(LegacyVisitStatus.advanceBillPayment((short) 12)).isEqualTo((short) 12);  // NEVER regresses
    }

    @Test
    void dispense_reaches12_onlyFromBilledWithNoPendingItems() {
        // unreachable before billed
        assertThat(LegacyVisitStatus.advanceDispense(null, false)).isNull();
        assertThat(LegacyVisitStatus.advanceDispense((short) 5, false)).isNull();
        // blocked while undispensed item sales remain
        assertThat(LegacyVisitStatus.advanceDispense((short) 11, true)).isNull();
        // happy path: 11 -> 12
        assertThat(LegacyVisitStatus.advanceDispense((short) 11, false)).isEqualTo((short) 12);
        // already terminal stays terminal
        assertThat(LegacyVisitStatus.advanceDispense((short) 12, true)).isEqualTo((short) 12);
    }

    @Test
    void effectiveResolution_takesTheHigherOfBothSides() {
        assertThat(LegacyVisitStatus.resolveEffective(null, null)).isNull();
        assertThat(LegacyVisitStatus.resolveEffective((short) 11, null)).isEqualTo((short) 11);
        assertThat(LegacyVisitStatus.resolveEffective(null, (short) 12)).isEqualTo((short) 12);
        assertThat(LegacyVisitStatus.resolveEffective((short) 11, (short) 12)).isEqualTo((short) 12);
        assertThat(LegacyVisitStatus.resolveEffective((short) 12, (short) 11)).isEqualTo((short) 12);
        assertThat(LegacyVisitStatus.resolveEffective((short) 9, (short) 9)).isEqualTo((short) 9);
    }
}
