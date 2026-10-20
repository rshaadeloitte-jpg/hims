package com.hims.adt.domain.model;

import java.util.Set;

/** Multi-department discharge clearance checklist (BR-DIS-001). All four must clear. */
public enum ClearanceDepartment {
    NURSING, PHARMACY, BILLING, LAB;

    public static final Set<String> ALL_CODES =
            Set.of(NURSING.name(), PHARMACY.name(), BILLING.name(), LAB.name());

    public static boolean isKnown(String dept) {
        return dept != null && ALL_CODES.contains(dept);
    }
}
