package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of T_ATTENDANTPASSDETAILS (AttendantPassDetails).
 * pass_no = ATT/yy/seq, status ISSUED -> RETURNED.
 */
@Entity
@Table(name = "t_attendant_pass")
public class AttendantPass extends AdtBase {

    @Id
    @Column(name = "pass_id")
    private Long passId;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "attendee_name")
    private String attendeeName;

    @Column(name = "pass_no", nullable = false, length = 50)
    private String passNo;

    @Column(name = "pass_status", nullable = false, length = 20)
    private String passStatus = "ISSUED";

    @Column(name = "pass_price")
    private Long passPrice;

    @Column(name = "print_count", nullable = false)
    private Long printCount = 1L;

    @Column(name = "max_refundable")
    private Long maxRefundable;

    @Column(name = "return_date")
    private Timestamp returnDate;

    @Override public Long getId() { return passId; }
    @Override public void setId(Long id) { this.passId = id; }
    public Long getPassId() { return passId; }
    public void setPassId(Long passId) { this.passId = passId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getAttendeeName() { return attendeeName; }
    public void setAttendeeName(String attendeeName) { this.attendeeName = attendeeName; }
    public String getPassNo() { return passNo; }
    public void setPassNo(String passNo) { this.passNo = passNo; }
    public String getPassStatus() { return passStatus; }
    public void setPassStatus(String passStatus) { this.passStatus = passStatus; }
    public Long getPassPrice() { return passPrice; }
    public void setPassPrice(Long passPrice) { this.passPrice = passPrice; }
    public Long getPrintCount() { return printCount; }
    public void setPrintCount(Long printCount) { this.printCount = printCount; }
    public Long getMaxRefundable() { return maxRefundable; }
    public void setMaxRefundable(Long maxRefundable) { this.maxRefundable = maxRefundable; }
    public Timestamp getReturnDate() { return returnDate; }
    public void setReturnDate(Timestamp returnDate) { this.returnDate = returnDate; }

    public boolean isIssued() { return "ISSUED".equals(passStatus); }
    public boolean isReturned() { return "RETURNED".equals(passStatus); }
}
