package com.hims.bloodbank.domain.model;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Blood group compatibility gate for transfusion safety.
 *
 * Table source: standard transfusion practice, not legacy, because legacy has
 * no compatibility table; it needs review by a blood bank physician.
 *
 * Rules are split by component:
 * - Red cell products (PRBC, PACKED RED BLOOD CELLS, PACKED RED CELLS, RED BLOOD CELLS, RBC):
 *   governed by {@link #RED_CELL_COMPATIBILITY_TABLE} (O- universal red-cell donor, AB+ universal recipient).
 * - Whole blood (WHOLE BLOOD, WB): governed by {@link #WHOLE_BLOOD_COMPATIBILITY_TABLE}, where the
 *   donor's ABO must equal the recipient's ABO; Rh-negative recipients get Rh-negative only;
 *   Rh-positive recipients get Rh-positive or Rh-negative.
 *
 * A null, blank, or unknown component returns false. There is no default fallback treating
 * a missing component as red cells. A missing or unknown blood group on either side also blocks.
 */
public final class BloodCompatibilityGate {

    private BloodCompatibilityGate() {}

    /**
     * Standard red blood cell compatibility table written as immutable data.
     * Maps Recipient ABO/Rh group -> Set of compatible Donor ABO/Rh groups.
     */
    public static final Map<String, Set<String>> RED_CELL_COMPATIBILITY_TABLE = Map.of(
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
     * Standard whole blood compatibility table written as immutable data.
     * The donor's ABO must equal the recipient's ABO; Rh-negative recipients get Rh-negative only;
     * Rh-positive recipients get Rh-positive or Rh-negative.
     * Maps Recipient ABO/Rh group -> Set of compatible Donor ABO/Rh groups.
     */
    public static final Map<String, Set<String>> WHOLE_BLOOD_COMPATIBILITY_TABLE = Map.of(
            "O-",  Set.of("O-"),
            "O+",  Set.of("O-", "O+"),
            "A-",  Set.of("A-"),
            "A+",  Set.of("A-", "A+"),
            "B-",  Set.of("B-"),
            "B+",  Set.of("B-", "B+"),
            "AB-", Set.of("AB-"),
            "AB+", Set.of("AB-", "AB+")
    );

    public static final Set<String> ALL_BLOOD_GROUPS = Set.of(
            "O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"
    );

    /**
     * Normalizes an ABO/Rh blood group string (e.g. "o+" -> "O+").
     * Returns null if missing, blank, or not recognized as one of the standard 8 groups.
     */
    public static String normalizeBloodGroup(String group) {
        if (group == null) {
            return null;
        }
        String trimmed = group.trim().toUpperCase(Locale.ROOT);
        return ALL_BLOOD_GROUPS.contains(trimmed) ? trimmed : null;
    }

    /**
     * Determines whether the requested component is a red blood cell product
     * governed by standard red-cell compatibility rules.
     * Null, blank, or unrecognized strings return false.
     */
    public static boolean isRedCellComponent(String component) {
        if (component == null || component.isBlank()) {
            return false;
        }
        String c = component.trim().toUpperCase(Locale.ROOT);
        return switch (c) {
            case "PRBC", "PACKED RED BLOOD CELLS", "PACKED RED CELLS", "RED BLOOD CELLS", "RBC" -> true;
            default -> false;
        };
    }

    /**
     * Determines whether the requested component is a whole blood product
     * governed by standard whole blood compatibility rules.
     * Null, blank, or unrecognized strings return false.
     */
    public static boolean isWholeBloodComponent(String component) {
        if (component == null || component.isBlank()) {
            return false;
        }
        String c = component.trim().toUpperCase(Locale.ROOT);
        return switch (c) {
            case "WHOLE BLOOD", "WB" -> true;
            default -> false;
        };
    }

    /**
     * Determines whether the component is recognized (red cell or whole blood).
     */
    public static boolean isKnownComponent(String component) {
        return isRedCellComponent(component) || isWholeBloodComponent(component);
    }

    /**
     * Checks compatibility between recipient and donor blood groups for the given component.
     * A null, blank, or unknown component returns false.
     * Blocks if either blood group is missing or unknown.
     */
    public static boolean isCompatible(String recipientGroup, String donorGroup, String component) {
        if (component == null || component.isBlank()) {
            return false;
        }
        String normRecipient = normalizeBloodGroup(recipientGroup);
        String normDonor = normalizeBloodGroup(donorGroup);
        if (normRecipient == null || normDonor == null) {
            return false;
        }

        Map<String, Set<String>> table;
        if (isRedCellComponent(component)) {
            table = RED_CELL_COMPATIBILITY_TABLE;
        } else if (isWholeBloodComponent(component)) {
            table = WHOLE_BLOOD_COMPATIBILITY_TABLE;
        } else {
            return false;
        }

        Set<String> compatibleDonors = table.get(normRecipient);
        return compatibleDonors != null && compatibleDonors.contains(normDonor);
    }

    /**
     * Overload checking compatibility without a component.
     * Always returns false because a missing component cannot be determined.
     */
    public static boolean isCompatible(String recipientGroup, String donorGroup) {
        return isCompatible(recipientGroup, donorGroup, null);
    }

    /**
     * Returns the set of compatible donor blood groups for the given recipient blood group
     * and component. Returns empty set if component or recipient group is unknown or missing.
     */
    public static Set<String> getCompatibleDonorGroups(String recipientGroup, String component) {
        if (component == null || component.isBlank()) {
            return Collections.emptySet();
        }
        String normRecipient = normalizeBloodGroup(recipientGroup);
        if (normRecipient == null) {
            return Collections.emptySet();
        }
        Map<String, Set<String>> table;
        if (isRedCellComponent(component)) {
            table = RED_CELL_COMPATIBILITY_TABLE;
        } else if (isWholeBloodComponent(component)) {
            table = WHOLE_BLOOD_COMPATIBILITY_TABLE;
        } else {
            return Collections.emptySet();
        }
        return table.getOrDefault(normRecipient, Collections.emptySet());
    }

    /**
     * Red-cell compatible donor groups overload for backwards compatibility.
     */
    public static Set<String> getCompatibleDonorGroups(String recipientGroup) {
        return getCompatibleDonorGroups(recipientGroup, "PRBC");
    }
}
