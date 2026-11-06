package com.hims.diet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of legacy T_DIETORDERDETAILS (DietOrderDetails.java). A doctor's
 * diet order for an admitted patient. Lifecycle (legacy DietServiceImpl):
 * orderStatus 1=UNRELEASED (raised), 2=ACTIVE/SIGNED, 3=CANCELLED/REPLACED.
 * patient/admission/ward/bed/doctor/service ids are opaque caller-supplied FKs
 * (deferred cross-domain resolution).
 */
@Entity
@Table(name = "diet_order_details")
public class DietOrderDetails extends DietBase {

    @Id
    private Long id;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "admission_id")
    private Long admissionId;

    @Column(name = "ward_id")
    private Long wardId;

    @Column(name = "bed_id")
    private Long bedId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "from_date")
    private Timestamp fromDate;

    @Column(name = "to_date")
    private Timestamp toDate;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_status")
    private Long orderStatus = 1L;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAdmissionId() { return admissionId; }
    public void setAdmissionId(Long admissionId) { this.admissionId = admissionId; }
    public Long getWardId() { return wardId; }
    public void setWardId(Long wardId) { this.wardId = wardId; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public Timestamp getFromDate() { return fromDate; }
    public void setFromDate(Timestamp fromDate) { this.fromDate = fromDate; }
    public Timestamp getToDate() { return toDate; }
    public void setToDate(Timestamp toDate) { this.toDate = toDate; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Long orderStatus) { this.orderStatus = orderStatus; }
}
