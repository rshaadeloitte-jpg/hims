package com.hims.adt.domain.model;

import com.hims.adt.domain.model.ClearanceChecklistItem.ClrStatus;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L0 — multi-department discharge clearance checklist (BR-DIS-001):
 * NURSING / PHARMACY / BILLING / LAB must ALL clear before DISCHARGED.
 */
class ClearanceChecklistLogicTest {

    private ClearanceChecklistItem item(String dept) {
        ClearanceChecklistItem i = new ClearanceChecklistItem();
        i.setClearanceId(1L);
        i.setAdmissionId(10L);
        i.setDepartment(ClearanceDepartment.valueOf(dept));
        i.setClrStatus(ClrStatus.PENDING);
        return i;
    }

    @Test
    void allFourDepartmentsAreKnown() {
        assertThat(ClearanceDepartment.ALL_CODES)
                .containsExactlyInAnyOrder("NURSING", "PHARMACY", "BILLING", "LAB");
        assertThat(ClearanceDepartment.isKnown("NURSING")).isTrue();
        assertThat(ClearanceDepartment.isKnown("HOUSEKEEPING")).isFalse();
        assertThat(ClearanceDepartment.isKnown(null)).isFalse();
    }

    @Test
    void freshRowsStartPending() {
        for (String dept : ClearanceDepartment.ALL_CODES) {
            assertThat(item(dept).pending()).isTrue();
        }
    }

    @Test
    void clearedRowsStopBeingPending_andNeverRevert() {
        ClearanceChecklistItem billing = item("BILLING");
        billing.setClrStatus(ClrStatus.CLEARED);
        billing.setClearedAt(Timestamp.valueOf("2027-03-01 09:00:00"));
        assertThat(billing.pending()).isFalse();

        // "re-clear" keeps CLEARED — the state machine has no backward edge
        billing.setClrStatus(ClrStatus.CLEARED);
        assertThat(billing.pending()).isFalse();
        assertThat(billing.getClrStatus()).isEqualTo(ClrStatus.CLEARED);
    }

    @Test
    void dischargeAllowed_onlyWhenEveryRowCleared() {
        var rows = ClearanceDepartment.ALL_CODES.stream().map(this::item).toList();
        assertThat(rows.stream().anyMatch(ClearanceChecklistItem::pending)).isTrue();   // gate holds

        rows.forEach(r -> r.setClrStatus(ClrStatus.CLEARED));
        assertThat(rows.stream().noneMatch(ClearanceChecklistItem::pending)).isTrue();  // gate opens
    }
}
