package com.hims.mortuary.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Dead body registry row (clean target of T_MORTUARY).
 *
 * BR-M28-001 body status machine:
 *   ADMITTED -> body admitted to mortuary
 *   RELEASED -> body released to next of kin
 *   AUTOPSY_PENDING -> autopsy requested, awaiting approval
 *   AUTOPSY_COMPLETE -> autopsy performed
 *   POLICE_CASE -> police/MLC case flagged
 *   HANDED_OVER -> body handed over to police/authority
 *
 * admission_id links to adt-service t_admission (death linkage).
 */
@Entity
@Table(name = "t_dead_body")
public class DeadBody extends MortuaryBase {

    public static final String STATUS_ADMITTED = "ADMITTED";
    public static final String STATUS_RELEASED = "RELEASED";
    public static final String STATUS_AUTOPSY_PENDING = "AUTOPSY_PENDING";
    public static final String STATUS_AUTOPSY_COMPLETE = "AUTOPSY_COMPLETE";
    public static final String STATUS_POLICE_CASE = "POLICE_CASE";
    public static final String STATUS_HANDED_OVER = "HANDED_OVER";

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "death_date", nullable = false)
    private Timestamp deathDate;

    @Column(name = "cause_of_death")
    private String causeOfDeath;

    @Column(name = "death_location")
    private String deathLocation;

    @Column(name = "next_of_kin")
    private String nextOfKin;

    @Column(name = "next_of_kin_relation")
    private String nextOfKinRelation;

    @Column(name = "next_of_kin_phone")
    private String nextOfKinPhone;

    @Column(name = "body_status", nullable = false)
    private String bodyStatus = STATUS_ADMITTED;

    @Column(name = "chamber_no")
    private String chamberNo;

    @Column(name = "death_cert_no")
    private String deathCertNo;

    @Column(name = "police_case", nullable = false)
    private Boolean policeCase = false;

    @Column(name = "mlc_no")
    private String mlcNo;

    @Column(name = "remark")
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public Timestamp getDeathDate() { return deathDate; }
    public void setDeathDate(Timestamp deathDate) { this.deathDate = deathDate; }
    public String getCauseOfDeath() { return causeOfDeath; }
    public void setCauseOfDeath(String causeOfDeath) { this.causeOfDeath = causeOfDeath; }
    public String getDeathLocation() { return deathLocation; }
    public void setDeathLocation(String deathLocation) { this.deathLocation = deathLocation; }
    public String getNextOfKin() { return nextOfKin; }
    public void setNextOfKin(String nextOfKin) { this.nextOfKin = nextOfKin; }
    public String getNextOfKinRelation() { return nextOfKinRelation; }
    public void setNextOfKinRelation(String nextOfKinRelation) { this.nextOfKinRelation = nextOfKinRelation; }
    public String getNextOfKinPhone() { return nextOfKinPhone; }
    public void setNextOfKinPhone(String nextOfKinPhone) { this.nextOfKinPhone = nextOfKinPhone; }
    public String getBodyStatus() { return bodyStatus; }
    public void setBodyStatus(String bodyStatus) { this.bodyStatus = bodyStatus; }
    public String getChamberNo() { return chamberNo; }
    public void setChamberNo(String chamberNo) { this.chamberNo = chamberNo; }
    public String getDeathCertNo() { return deathCertNo; }
    public void setDeathCertNo(String deathCertNo) { this.deathCertNo = deathCertNo; }
    public Boolean getPoliceCase() { return policeCase; }
    public void setPoliceCase(Boolean policeCase) { this.policeCase = policeCase; }
    public String getMlcNo() { return mlcNo; }
    public void setMlcNo(String mlcNo) { this.mlcNo = mlcNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    /** True when the body has been released. */
    public boolean isReleased() {
        return STATUS_RELEASED.equals(bodyStatus) || STATUS_HANDED_OVER.equals(bodyStatus);
    }

    /** True when an autopsy is pending or complete. */
    public boolean hasAutopsy() {
        return STATUS_AUTOPSY_PENDING.equals(bodyStatus) || STATUS_AUTOPSY_COMPLETE.equals(bodyStatus);
    }
}
