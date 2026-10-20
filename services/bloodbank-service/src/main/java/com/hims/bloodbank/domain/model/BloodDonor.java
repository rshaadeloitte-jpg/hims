package com.hims.bloodbank.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Donor registry row (clean target of T_BLOODDONOR).
 *
 * donationStatus per legacy DNR enum (BloodBankConstants.java:8-17):
 *   1 = DNR_NEW, 2 = DNR_FIT, 3..6 = post-med-exam stages, 7 = DNR_CANCELLATION.
 * RISK A8: legacy literal 5 means BOTH DNR_REJECTED and DNR_DONATED — ambiguity
 * documented here; this service only ever writes 1/2/7.
 *
 * BR-BLD-005: donors parked in 1/2 with no action within 72h are auto-cancelled (7)
 * by POST /api/v1/jobs/cancel-stale-donors; {@code lastActionAt} is refreshed by the
 * touch endpoint and gates that rule.
 */
@Entity
@Table(name = "t_blooddonor")
public class BloodDonor extends BloodBankBase {

    public static final int DNR_NEW = 1;
    public static final int DNR_FIT = 2;
    public static final int DNR_CANCELLATION = 7;

    @Id
    @Column(name = "donor_id")
    private Long donorId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "blood_group", nullable = false)
    private String bloodGroup;

    @Column(name = "donation_status", nullable = false)
    private Integer donationStatus = DNR_NEW;

    /** Last workflow action timestamp — refreshed by POST /donors/{id}/touch. */
    @Column(name = "last_action_at", nullable = false)
    private Timestamp lastActionAt;

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }
    public Long getId() { return donorId; }
    public void setId(Long id) { this.donorId = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public Integer getDonationStatus() { return donationStatus; }
    public void setDonationStatus(Integer donationStatus) { this.donationStatus = donationStatus; }
    public Timestamp getLastActionAt() { return lastActionAt; }
    public void setLastActionAt(Timestamp lastActionAt) { this.lastActionAt = lastActionAt; }
}
