package com.hims.adt.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L0 — bed occupancy state machine (STATUS-CODE-DICTIONARY §A5 + CLEANING).
 * Pins the operational contract so no code path can move a bed into an
 * impossible state.
 */
class BedStateTransitionTest {

    @Test
    void lockedDictionary_codesMatchTheStatusMasterSeeds() {
        assertThat(BedState.RESET.code()).isEqualTo((short) 0);
        assertThat(BedState.FREE.code()).isEqualTo((short) 1);
        assertThat(BedState.CLEANING.code()).isEqualTo((short) 2);
        assertThat(BedState.FREE_PREDICATE.code()).isEqualTo((short) 3);
        assertThat(BedState.OCCUPIED.code()).isEqualTo((short) 5);
        assertThat(BedState.RELEASED.code()).isEqualTo((short) 6);
    }

    @Test
    void releasableStatuses_allowAllocation_BR_IPD_003() {
        // master isrelease=true -> claimable; CLEANING(2) and OCCUPIED(5) never are
        assertThat(BedState.canClaim((short) 0)).isTrue();
        assertThat(BedState.canClaim((short) 1)).isTrue();
        assertThat(BedState.canClaim((short) 2)).isFalse();
        assertThat(BedState.canClaim((short) 3)).isTrue();
        assertThat(BedState.canClaim((short) 5)).isFalse();
        assertThat(BedState.canClaim((short) 6)).isTrue();
    }

    @Test
    void transferReleasesVacatedBedToCleaning() {
        assertThat(BedState.transferReleaseTarget()).isEqualTo((short) 2);
        // and a CLEANING bed cannot be claimed by another admission while dirty
        assertThat(BedState.canClaim(BedState.transferReleaseTarget())).isFalse();
    }

    @Test
    void dischargeReleaseTarget_isThePinnedLiteral6_BR_DIS_003() {
        assertThat(BedState.dischargeReleaseTarget()).isEqualTo((short) 6);   // RISK R5 contract
    }

    @Test
    void onlyCleaningBedsCanBeMarkedCleaned() {
        assertThat(BedState.canFinishCleaning((short) 2)).isTrue();
        assertThat(BedState.canFinishCleaning((short) 1)).isFalse();
        assertThat(BedState.canFinishCleaning((short) 5)).isFalse();
        assertThat(BedState.canFinishCleaning((short) 6)).isFalse();
    }

    @Test
    void roundTrip_fullBedLifecycle() {
        short s = BedState.FREE.code();                 // new bed seeded FREE
        assertThat(BedState.canClaim(s)).isTrue();      // admit/transfer claims it
        s = BedState.OCCUPIED.code();
        s = BedState.transferReleaseTarget();           // patient moves out
        assertThat(s).isEqualTo((short) 2);
        assertThat(BedState.canFinishCleaning(s)).isTrue();
        s = BedState.FREE.code();                       // housekeeping done
        assertThat(BedState.canClaim(s)).isTrue();
        s = BedState.OCCUPIED.code();                   // re-admitted
        s = BedState.dischargeReleaseTarget();          // discharged this time
        assertThat(s).isEqualTo((short) 6);
        assertThat(BedState.canClaim(s)).isTrue();      // released bed can host again
    }

    @Test
    void unknownCode_rejected() {
        assertThatThrownBy(() -> BedState.fromCode((short) 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
