package com.hims.diet.domain.model;

/**
 * Clean target of legacy com.ohum.util.DietActionStatus. Diet schedule action
 * status (stored as string in diet_item_schedule_details.diet_action_status).
 */
public enum DietActionStatus {
    HOLD("Hold"),
    CANCEL("Cancel"),
    REJECT("Reject"),
    RELEASE("Release");

    private final String statusType;

    DietActionStatus(String statusType) {
        this.statusType = statusType;
    }

    public String getStatusType() {
        return statusType;
    }
}
