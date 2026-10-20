package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Clean target of legacy M_DRIVERMASTER (DriverMaster.java). Driver master
 * owned by the transport domain; isAvailable flips false on allocation and
 * back to true on vehicle-in / cancel.
 */
@Entity
@Table(name = "driver")
public class Driver extends TransportBase {

    @Id
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "dob")
    private Timestamp dob;

    @Column(name = "mobile_no")
    private Long mobileNo;

    @Column(name = "gender_id")
    private Long genderId;

    @Column(name = "driving_lic_no")
    private String drivingLicNo;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "license_type")
    private String licenseType;

    @Column(name = "license_exp_date")
    private Timestamp licenseExpDate;

    @Column(name = "driver_experience")
    private Long driverExperience;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public Timestamp getDob() { return dob; }
    public void setDob(Timestamp dob) { this.dob = dob; }
    public Long getMobileNo() { return mobileNo; }
    public void setMobileNo(Long mobileNo) { this.mobileNo = mobileNo; }
    public Long getGenderId() { return genderId; }
    public void setGenderId(Long genderId) { this.genderId = genderId; }
    public String getDrivingLicNo() { return drivingLicNo; }
    public void setDrivingLicNo(String drivingLicNo) { this.drivingLicNo = drivingLicNo; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }
    public Timestamp getLicenseExpDate() { return licenseExpDate; }
    public void setLicenseExpDate(Timestamp licenseExpDate) { this.licenseExpDate = licenseExpDate; }
    public Long getDriverExperience() { return driverExperience; }
    public void setDriverExperience(Long driverExperience) { this.driverExperience = driverExperience; }
}
