package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of legacy t_vehiclerequest (TransportRequest.java). A vehicle /
 * ambulance transport request. Lifecycle flags (legacy TransportServiceImpl):
 * isApproved (auto-true for patient requests), isVehicleOut, completed,
 * isCancelled. No requisition number in legacy — the requester name
 * (requisitionBy) identifies the request.
 */
@Entity
@Table(name = "vehicle_request")
public class VehicleRequest extends TransportBase {

    @Id
    private Long id;

    @Column(name = "requisition_by")
    private String requisitionBy;

    @Column(name = "mobile_no")
    private Long mobileNo;

    @Column(name = "mobile_country_code")
    private Long mobileCountryCode;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "location")
    private String location;

    @Column(name = "request_type")
    private Long requestType;

    @Column(name = "is_charge_exempted")
    private Boolean isChargeExempted = false;

    @Column(name = "exemption_catg_id")
    private Long exemptionCatgId;

    @Column(name = "bid_aid")
    private String bidAid;

    @Column(name = "request_date_time")
    private Timestamp requestDateTime;

    @Column(name = "expiry_date_time")
    private Timestamp expiryDateTime;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "is_doctor_request")
    private Boolean isDoctorRequest = false;

    @Column(name = "user_type")
    private Long userType;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "is_pickup_trip")
    private Boolean isPickupTrip = false;

    @Column(name = "is_drop_trip")
    private Boolean isDropTrip = false;

    @Column(name = "is_deadbody_trip")
    private Boolean isDeadbodyTrip = false;

    @Column(name = "staff_instrument_req")
    private String staffInstrumentReq;

    @Column(name = "is_vehicle_out")
    private Boolean isVehicleOut = false;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    @Column(name = "allocated_doctors")
    private String allocatedDoctors;

    @Column(name = "allocated_nurses")
    private String allocatedNurses;

    @Column(name = "request_to_date_time")
    private Timestamp requestToDateTime;

    @Column(name = "is_cancelled")
    private Boolean isCancelled = false;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "is_charged")
    private Boolean isCharged = false;

    @Column(name = "opd_ipd")
    private Short opdIpd;

    @Column(name = "opd_ipd_id")
    private Long opdIpdId;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getRequisitionBy() { return requisitionBy; }
    public void setRequisitionBy(String requisitionBy) { this.requisitionBy = requisitionBy; }
    public Long getMobileNo() { return mobileNo; }
    public void setMobileNo(Long mobileNo) { this.mobileNo = mobileNo; }
    public Long getMobileCountryCode() { return mobileCountryCode; }
    public void setMobileCountryCode(Long mobileCountryCode) { this.mobileCountryCode = mobileCountryCode; }
    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Long getRequestType() { return requestType; }
    public void setRequestType(Long requestType) { this.requestType = requestType; }
    public Boolean getIsChargeExempted() { return isChargeExempted; }
    public void setIsChargeExempted(Boolean isChargeExempted) { this.isChargeExempted = isChargeExempted; }
    public Long getExemptionCatgId() { return exemptionCatgId; }
    public void setExemptionCatgId(Long exemptionCatgId) { this.exemptionCatgId = exemptionCatgId; }
    public String getBidAid() { return bidAid; }
    public void setBidAid(String bidAid) { this.bidAid = bidAid; }
    public Timestamp getRequestDateTime() { return requestDateTime; }
    public void setRequestDateTime(Timestamp requestDateTime) { this.requestDateTime = requestDateTime; }
    public Timestamp getExpiryDateTime() { return expiryDateTime; }
    public void setExpiryDateTime(Timestamp expiryDateTime) { this.expiryDateTime = expiryDateTime; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Boolean getIsDoctorRequest() { return isDoctorRequest; }
    public void setIsDoctorRequest(Boolean isDoctorRequest) { this.isDoctorRequest = isDoctorRequest; }
    public Long getUserType() { return userType; }
    public void setUserType(Long userType) { this.userType = userType; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public Boolean getIsPickupTrip() { return isPickupTrip; }
    public void setIsPickupTrip(Boolean isPickupTrip) { this.isPickupTrip = isPickupTrip; }
    public Boolean getIsDropTrip() { return isDropTrip; }
    public void setIsDropTrip(Boolean isDropTrip) { this.isDropTrip = isDropTrip; }
    public Boolean getIsDeadbodyTrip() { return isDeadbodyTrip; }
    public void setIsDeadbodyTrip(Boolean isDeadbodyTrip) { this.isDeadbodyTrip = isDeadbodyTrip; }
    public String getStaffInstrumentReq() { return staffInstrumentReq; }
    public void setStaffInstrumentReq(String staffInstrumentReq) { this.staffInstrumentReq = staffInstrumentReq; }
    public Boolean getIsVehicleOut() { return isVehicleOut; }
    public void setIsVehicleOut(Boolean isVehicleOut) { this.isVehicleOut = isVehicleOut; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDriverId() { return driverId; }
    public void setDriverId(Long driverId) { this.driverId = driverId; }
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    public String getAllocatedDoctors() { return allocatedDoctors; }
    public void setAllocatedDoctors(String allocatedDoctors) { this.allocatedDoctors = allocatedDoctors; }
    public String getAllocatedNurses() { return allocatedNurses; }
    public void setAllocatedNurses(String allocatedNurses) { this.allocatedNurses = allocatedNurses; }
    public Timestamp getRequestToDateTime() { return requestToDateTime; }
    public void setRequestToDateTime(Timestamp requestToDateTime) { this.requestToDateTime = requestToDateTime; }
    public Boolean getIsCancelled() { return isCancelled; }
    public void setIsCancelled(Boolean isCancelled) { this.isCancelled = isCancelled; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Boolean getIsCharged() { return isCharged; }
    public void setIsCharged(Boolean isCharged) { this.isCharged = isCharged; }
    public Short getOpdIpd() { return opdIpd; }
    public void setOpdIpd(Short opdIpd) { this.opdIpd = opdIpd; }
    public Long getOpdIpdId() { return opdIpdId; }
    public void setOpdIpdId(Long opdIpdId) { this.opdIpdId = opdIpdId; }
}
