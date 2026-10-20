package com.hims.appointment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.sql.Timestamp;

/**
 * Clean target of T_Encounter. One open encounter per visit; closed either by
 * consultation completion or by the daily closing job (ApplicationJobs:542
 * parity). legacy_visit_status carries the resolved VisitStatus 11/12 state
 * with the exact legacy advance guards ({@link LegacyVisitStatus}) so billing
 * and pharmacy writes converge on one row instead of split-braining across two
 * tables.
 */
@Entity
@Table(name = "encounter")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Encounter extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "encounter_date", nullable = false)
    private LocalDate encounterDate;

    /** Per (encounter_date, unit_id) sequence from daily_encounter_counter. */
    @Column(name = "encounter_no", nullable = false)
    private Integer encounterNo;

    @Column(name = "token_no")
    private Integer tokenNo;

    @Column(name = "cabin")
    private String cabin;

    /** Legacy visitstatus2 mirror ('2' default on visit creation). */
    @Column(name = "visitstatus2", nullable = false)
    private String visitstatus2 = "2";

    /** Resolved VisitStatus (11=billed, 12=dispensed); guards in {@link LegacyVisitStatus}. */
    @Column(name = "legacy_visit_status")
    private Short legacyVisitStatus;

    @Column(name = "pharmacy_pending", nullable = false)
    private boolean pharmacyPending = false;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;

    @Column(name = "closed_at")
    private Timestamp closedAt;

    @Column(name = "closed_by")
    private Long closedBy;

    @Column(name = "close_reason")
    private String closeReason;

    public boolean isOpen() { return !closed; }

    /** Effective VisitStatus for queue/kiosk consumers (split-brain read parity). */
    public Short effectiveLegacyVisitStatus(Short appointmentSideValue) {
        return LegacyVisitStatus.resolveEffective(appointmentSideValue, legacyVisitStatus);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getClinicId() { return clinicId; }
    public void setClinicId(Long clinicId) { this.clinicId = clinicId; }
    public LocalDate getEncounterDate() { return encounterDate; }
    public void setEncounterDate(LocalDate encounterDate) { this.encounterDate = encounterDate; }
    public Integer getEncounterNo() { return encounterNo; }
    public void setEncounterNo(Integer encounterNo) { this.encounterNo = encounterNo; }
    public Integer getTokenNo() { return tokenNo; }
    public void setTokenNo(Integer tokenNo) { this.tokenNo = tokenNo; }
    public String getCabin() { return cabin; }
    public void setCabin(String cabin) { this.cabin = cabin; }
    public String getVisitstatus2() { return visitstatus2; }
    public void setVisitstatus2(String visitstatus2) { this.visitstatus2 = visitstatus2; }
    public Short getLegacyVisitStatus() { return legacyVisitStatus; }
    public void setLegacyVisitStatus(Short legacyVisitStatus) { this.legacyVisitStatus = legacyVisitStatus; }
    public boolean isPharmacyPending() { return pharmacyPending; }
    public void setPharmacyPending(boolean pharmacyPending) { this.pharmacyPending = pharmacyPending; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean closed) { this.closed = closed; }
    public Timestamp getClosedAt() { return closedAt; }
    public void setClosedAt(Timestamp closedAt) { this.closedAt = closedAt; }
    public Long getClosedBy() { return closedBy; }
    public void setClosedBy(Long closedBy) { this.closedBy = closedBy; }
    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }
}
