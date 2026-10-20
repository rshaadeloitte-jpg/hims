package com.hims.ot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean target of T_OTSCHEDULE (legacy OTSchedulerServiceImpl).
 *
 * Lifecycle ({@code status} SMALLINT, register-pinned):
 *   1 = PLANNED -> 2 = IN_PROGRESS -> 3 = DONE
 *         \-> 4 = CANCELLED (from PLANNED or IN_PROGRESS)
 * Guards enforced in OtScheduleService (start/complete/cancel); illegal
 * transitions are rejected with HTTP 400.
 *
 * BR-OT-001: admission_id OR encounter_id required at create (else BEOT004).
 * BR-OT-002: pre_billing flag persisted and echoed in responses. The actual
 * direct-charge creation itself is DEFERRED to Billing (Wave 4) — this service
 * never creates charges; Billing consumes the flag when it lands.
 */
@Entity
@Table(name = "t_otschedule")
public class OtSchedule extends OtBase {

    /** Lifecycle states per register WP-8 / STATUS-CODE-DICTIONARY. */
    public static final short STATUS_PLANNED = 1;
    public static final short STATUS_IN_PROGRESS = 2;
    public static final short STATUS_DONE = 3;
    public static final short STATUS_CANCELLED = 4;

    @Id
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "ot_table", nullable = false, length = 100)
    private String otTable;

    @Column(name = "scheduled_start", nullable = false)
    private Timestamp scheduledStart;

    @Column(name = "scheduled_end")
    private Timestamp scheduledEnd;

    @Column(name = "anesthetist_id")
    private Long anesthetistId;

    @Column(name = "surgeon_id")
    private Long surgeonId;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_PLANNED;

    /** BR-OT-002 pre-billing flag; charge creation deferred to Billing Wave 4. */
    @Column(name = "pre_billing", nullable = false)
    private Boolean preBilling = false;

    /** Transient view of details (populated by queries/responses; not a JPA relation). */
    @jakarta.persistence.Transient
    private List<OtScheduleDetail> details = new ArrayList<>();

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public Long getId() { return scheduleId; }
    public void setId(Long id) { this.scheduleId = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public String getOtTable() { return otTable; }
    public void setOtTable(String otTable) { this.otTable = otTable; }
    public Timestamp getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(Timestamp scheduledStart) { this.scheduledStart = scheduledStart; }
    public Timestamp getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(Timestamp scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public Long getAnesthetistId() { return anesthetistId; }
    public void setAnesthetistId(Long anesthetistId) { this.anesthetistId = anesthetistId; }
    public Long getSurgeonId() { return surgeonId; }
    public void setSurgeonId(Long surgeonId) { this.surgeonId = surgeonId; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Boolean getPreBilling() { return preBilling; }
    public void setPreBilling(Boolean preBilling) { this.preBilling = preBilling; }

    /** BR-OT-001 dependent-surgery discount helper: net rate after discount_pct. */
    public static BigDecimal netRate(BigDecimal rate, BigDecimal discountPct) {
        BigDecimal r = rate == null ? BigDecimal.ZERO : rate;
        BigDecimal d = discountPct == null ? BigDecimal.ZERO : discountPct;
        return r.multiply(BigDecimal.valueOf(100).subtract(d))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    public List<OtScheduleDetail> getDetails() { return details; }
    public void setDetails(List<OtScheduleDetail> details) {
        this.details = details == null ? new ArrayList<>() : details;
    }
}
