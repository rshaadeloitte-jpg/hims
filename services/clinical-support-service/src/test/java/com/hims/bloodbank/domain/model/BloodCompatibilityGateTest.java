package com.hims.bloodbank.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the ABO/Rh compatibility gate data tables and rules.
 * Source: standard transfusion practice, not legacy (requires blood bank physician review).
 * Covers both Red Cell (27 compatible / 64 pairs) and Whole Blood (12 compatible / 64 pairs) tables.
 */
class BloodCompatibilityGateTest {

    private static final List<String> ALL_8_GROUPS = List.of(
            "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"
    );

    /**
     * Standard red-cell compatibility table:
     * O- recipient: O- (1)
     * O+ recipient: O-, O+ (2)
     * A- recipient: O-, A- (2)
     * A+ recipient: O-, O+, A-, A+ (4)
     * B- recipient: O-, B- (2)
     * B+ recipient: O-, O+, B-, B+ (4)
     * AB- recipient: O-, A-, B-, AB- (4)
     * AB+ recipient: universal recipient (all 8)
     * Total = 27 compatible out of 64 pairs.
     */
    private static final Map<String, Set<String>> EXPECTED_RED_CELL_COMPATIBILITY = Map.of(
            "O-",  Set.of("O-"),
            "O+",  Set.of("O-", "O+"),
            "A-",  Set.of("O-", "A-"),
            "A+",  Set.of("O-", "O+", "A-", "A+"),
            "B-",  Set.of("O-", "B-"),
            "B+",  Set.of("O-", "O+", "B-", "B+"),
            "AB-", Set.of("O-", "A-", "B-", "AB-"),
            "AB+", Set.of("O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+")
    );

    /**
     * Standard whole blood compatibility table:
     * Donor ABO must equal recipient ABO.
     * Rh-negative recipients get Rh-negative only;
     * Rh-positive recipients get Rh-positive or Rh-negative.
     * That is:
     * O-:  O- (1)
     * O+:  O-, O+ (2)
     * A-:  A- (1)
     * A+:  A-, A+ (2)
     * B-:  B- (1)
     * B+:  B-, B+ (2)
     * AB-: AB- (1)
     * AB+: AB-, AB+ (2)
     * Total = 12 compatible out of 64 pairs.
     */
    private static final Map<String, Set<String>> EXPECTED_WHOLE_BLOOD_COMPATIBILITY = Map.of(
            "O-",  Set.of("O-"),
            "O+",  Set.of("O-", "O+"),
            "A-",  Set.of("A-"),
            "A+",  Set.of("A-", "A+"),
            "B-",  Set.of("B-"),
            "B+",  Set.of("B-", "B+"),
            "AB-", Set.of("AB-"),
            "AB+", Set.of("AB-", "AB+")
    );

    @Test
    @DisplayName("Verify every recipient and donor pair (64 pairs each) matches red cell (27 compatible) and whole blood (12 compatible) tables")
    void testAll64RecipientAndDonorPairs() {
        int redCellCompatibleCount = 0;
        int redCellIncompatibleCount = 0;
        int wholeBloodCompatibleCount = 0;
        int wholeBloodIncompatibleCount = 0;

        for (String recipient : ALL_8_GROUPS) {
            Set<String> expectedRedCell = EXPECTED_RED_CELL_COMPATIBILITY.get(recipient);
            Set<String> expectedWholeBlood = EXPECTED_WHOLE_BLOOD_COMPATIBILITY.get(recipient);

            for (String donor : ALL_8_GROUPS) {
                // 1. Red cell products (test PRBC and RBC aliases)
                boolean expRedCell = expectedRedCell.contains(donor);
                boolean actualPrbc = BloodCompatibilityGate.isCompatible(recipient, donor, "PRBC");
                boolean actualRbc = BloodCompatibilityGate.isCompatible(recipient, donor, "RBC");

                assertThat(actualPrbc)
                        .as("PRBC compatibility for recipient %s with donor %s", recipient, donor)
                        .isEqualTo(expRedCell);
                assertThat(actualRbc)
                        .as("RBC compatibility for recipient %s with donor %s", recipient, donor)
                        .isEqualTo(expRedCell);

                if (expRedCell) {
                    redCellCompatibleCount++;
                } else {
                    redCellIncompatibleCount++;
                }

                // 2. Whole blood products (test WHOLE BLOOD and WB aliases)
                boolean expWholeBlood = expectedWholeBlood.contains(donor);
                boolean actualWbLong = BloodCompatibilityGate.isCompatible(recipient, donor, "WHOLE BLOOD");
                boolean actualWbShort = BloodCompatibilityGate.isCompatible(recipient, donor, "WB");

                assertThat(actualWbLong)
                        .as("WHOLE BLOOD compatibility for recipient %s with donor %s", recipient, donor)
                        .isEqualTo(expWholeBlood);
                assertThat(actualWbShort)
                        .as("WB compatibility for recipient %s with donor %s", recipient, donor)
                        .isEqualTo(expWholeBlood);

                if (expWholeBlood) {
                    wholeBloodCompatibleCount++;
                } else {
                    wholeBloodIncompatibleCount++;
                }
            }
        }

        // Exactly 27 compatible and 37 incompatible for red cells out of 64 pairs
        assertThat(redCellCompatibleCount).isEqualTo(27);
        assertThat(redCellIncompatibleCount).isEqualTo(37);

        // Exactly 12 compatible and 52 incompatible for whole blood out of 64 pairs
        assertThat(wholeBloodCompatibleCount).isEqualTo(12);
        assertThat(wholeBloodIncompatibleCount).isEqualTo(52);
    }

    // ------------------------------------------------------------ Red Cell specifics

    @Test
    void redCell_universalDonor_O_negative_canDonateToAll8() {
        for (String recipient : ALL_8_GROUPS) {
            assertThat(BloodCompatibilityGate.isCompatible(recipient, "O-", "PRBC"))
                    .as("O- red cells should be compatible for recipient %s", recipient)
                    .isTrue();
        }
    }

    @Test
    void redCell_universalRecipient_AB_positive_canReceiveFromAll8() {
        for (String donor : ALL_8_GROUPS) {
            assertThat(BloodCompatibilityGate.isCompatible("AB+", donor, "PRBC"))
                    .as("AB+ should receive red cells from donor %s", donor)
                    .isTrue();
        }
    }

    // ------------------------------------------------------------ Whole Blood specifics

    @Test
    @DisplayName("O whole blood to an A+ patient is refused")
    void oWholeBloodToAPositivePatientIsRefused() {
        // Recipient A+ CANNOT receive O Whole Blood (donor ABO must equal recipient ABO)
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O-", "WB")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "WB")).isFalse();

        // But Recipient A+ CAN receive O Red Cells (universal donor red cells)
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O-", "PRBC")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "PRBC")).isTrue();

        // And Recipient A+ CAN receive A Whole Blood
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A-", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "WHOLE BLOOD")).isTrue();
    }

    @Test
    void wholeBlood_hasNoUniversalDonorOrRecipient() {
        // O- whole blood can only donate to O- and O+ (not to A, B, AB)
        assertThat(BloodCompatibilityGate.isCompatible("O-", "O-", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O-", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A-", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("B-", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("B+", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB-", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", "WHOLE BLOOD")).isFalse();

        // AB+ whole blood can only receive from AB- and AB+ (not from O, A, B)
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "AB-", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "AB+", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O+", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "A-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "A+", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "B-", "WHOLE BLOOD")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "B+", "WHOLE BLOOD")).isFalse();
    }

    // ------------------------------------------------------------ Missing / unknown component checks

    @Test
    @DisplayName("Null, blank, and unknown component returns false (no fallback default)")
    void nullBlankAndUnknownComponentReturnsFalse() {
        // Null component
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", null)).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", null)).isFalse();

        // Blank component
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "   ")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", "")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", "   ")).isFalse();

        // Default overload (passes null component) returns false
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-")).isFalse();

        // Unknown component strings
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "FFP")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "PLASMA")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "PLATELETS")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "O+", "CRYOPRECIPITATE")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O-", "UNKNOWN_COMPONENT")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "RANDOM")).isFalse();
    }

    // ------------------------------------------------------------ Recognized component aliases

    @Test
    void recognizedComponentAliasesAllowed() {
        // Red cell aliases
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "PRBC")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "prbc")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "PACKED RED BLOOD CELLS")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "Packed Red Blood Cells")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "PACKED RED CELLS")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "packed red cells")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "RED BLOOD CELLS")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "red blood cells")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "RBC")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "O+", "rbc")).isTrue();

        // Whole blood aliases
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "WHOLE BLOOD")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "whole blood")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A-", "Whole Blood")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "WB")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("A+", "A+", "wb")).isTrue();
    }

    // ------------------------------------------------------------ Missing / unknown group checks

    @Test
    void missingOrUnknownGroupOnEitherSideBlocks() {
        // Missing recipient
        assertThat(BloodCompatibilityGate.isCompatible(null, "O-", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("", "O-", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("   ", "O-", "PRBC")).isFalse();

        // Missing donor
        assertThat(BloodCompatibilityGate.isCompatible("O+", null, "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("O+", "   ", "PRBC")).isFalse();

        // Unknown group recipient
        assertThat(BloodCompatibilityGate.isCompatible("X+", "O-", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("Bombay", "O-", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("UNKNOWN", "O-", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("A", "O-", "PRBC")).isFalse(); // missing Rh (+/-)

        // Unknown group donor
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "X+", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "C+", "PRBC")).isFalse();
        assertThat(BloodCompatibilityGate.isCompatible("AB+", "O", "PRBC")).isFalse(); // missing Rh

        // Normalization handles whitespace and lowercase
        assertThat(BloodCompatibilityGate.isCompatible(" o+ ", " o- ", "PRBC")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible("ab+ ", "a+", "PRBC")).isTrue();
        assertThat(BloodCompatibilityGate.isCompatible(" a+ ", " a- ", "WHOLE BLOOD")).isTrue();
    }
}
