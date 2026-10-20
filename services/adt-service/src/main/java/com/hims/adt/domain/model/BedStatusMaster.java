package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Bed status master (STATUS-CODE-DICTIONARY §A5 semantics, seeded in V1):
 *   0 = Available (reset on cancel/transfer)  — isrelease=true
 *   1 = Free                                   — isrelease=true
 *   3 = Free (free-bed predicate)              — isrelease=true
 *   5 = Occupied / retained                    — isrelease=false
 *   6 = Released (post-discharge, BR-DIS-003 hard-pinned literal) — isrelease=true
 * {@code isrelease} governs allocation (BR-IPD-003 gate at admission → BERB005).
 */
@Entity
@Table(name = "m_bed_status_master")
public class BedStatusMaster {

    @Id
    @Column(name = "bed_status_id")
    private Short bedStatusId;

    @Column(name = "description", nullable = false, length = 100)
    private String description;

    /** True when a bed carrying this status may be allocated (BR-IPD-003). */
    @Column(name = "isrelease", nullable = false)
    private Boolean isrelease;

    public Short getBedStatusId() { return bedStatusId; }
    public void setBedStatusId(Short bedStatusId) { this.bedStatusId = bedStatusId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsrelease() { return isrelease; }
    public void setIsrelease(Boolean isrelease) { this.isrelease = isrelease; }
}
