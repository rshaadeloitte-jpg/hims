package com.hims.nursing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

@Entity
@Table(name = "t_nurse_allocation")
public class NurseAllocation extends NursingBase {

    @Id
    @Column(name = "allocation_id")
    private Long allocationId;

    @Column(name = "ward_id", nullable = false)
    private Long wardId;

    @Column(name = "nurse_id", nullable = false)
    private Long nurseId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "shift", length = 20)
    private String shift;

    @Column(name = "allocated_at", nullable = false)
    private Timestamp allocatedAt;

    @Column(name = "released_at")
    private Timestamp releasedAt;

    @Override public Long getId() { return allocationId; }
    @Override public void setId(Long id) { this.allocationId = id; }
    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long v) { this.allocationId = v; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long v) { this.wardId = v; }
    public Long getNurseId() { return nurseId; }
    public void setNurseId(Long v) { this.nurseId = v; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long v) { this.admissionId = v; }
    public String getShift() { return shift; }
    public void setShift(String v) { this.shift = v; }
    public Timestamp getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(Timestamp v) { this.allocatedAt = v; }
    public Timestamp getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Timestamp v) { this.releasedAt = v; }
}
