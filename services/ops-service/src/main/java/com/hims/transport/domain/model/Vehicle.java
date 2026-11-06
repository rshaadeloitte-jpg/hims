package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Clean target of legacy M_AMBULANCEMASTER (Ambulancemaster.java). Vehicle /
 * ambulance master owned by the transport domain; isAvailable flips false on
 * allocation / maintenance and back to true on vehicle-in / cancel /
 * maintenance-complete.
 */
@Entity
@Table(name = "vehicle")
public class Vehicle extends TransportBase {

    @Id
    private Long id;

    @Column(name = "vehicle_name")
    private String vehicleName;

    @Column(name = "vehicle_make")
    private String vehicleMake;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "vehicle_no")
    private String vehicleNo;

    @Column(name = "purchase_date")
    private Timestamp purchaseDate;

    @Column(name = "purchase_amount")
    private BigDecimal purchaseAmount;

    @Column(name = "cash_lease_donated")
    private Integer cashLeaseDonated;

    @Column(name = "institute_name")
    private String instituteName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "chasy_no")
    private String chasyNo;

    @Column(name = "address")
    private String address;

    @Column(name = "no_of_lease_months")
    private Integer noOfLeaseMonths;

    @Column(name = "amount_each_month")
    private BigDecimal amountEachMonth;

    @Column(name = "invoice_no")
    private String invoiceNo;

    @Column(name = "vehicle_location")
    private String vehicleLocation;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(name = "fare_per_km")
    private BigDecimal farePerKm;

    @Column(name = "vehicle_average")
    private Long vehicleAverage;

    @Column(name = "insurance_no")
    private String insuranceNo;

    @Column(name = "insurance_expiry_date")
    private Timestamp insuranceExpiryDate;

    @Column(name = "puc_no")
    private String pucNo;

    @Column(name = "puc_expiry_date")
    private Timestamp pucExpiryDate;

    @Column(name = "is_108")
    private Boolean is108 = false;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getVehicleMake() { return vehicleMake; }
    public void setVehicleMake(String vehicleMake) { this.vehicleMake = vehicleMake; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public void setPurchaseAmount(BigDecimal purchaseAmount) { this.purchaseAmount = purchaseAmount; }
    public Integer getCashLeaseDonated() { return cashLeaseDonated; }
    public void setCashLeaseDonated(Integer cashLeaseDonated) { this.cashLeaseDonated = cashLeaseDonated; }
    public String getInstituteName() { return instituteName; }
    public void setInstituteName(String instituteName) { this.instituteName = instituteName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }
    public String getChasyNo() { return chasyNo; }
    public void setChasyNo(String chasyNo) { this.chasyNo = chasyNo; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Integer getNoOfLeaseMonths() { return noOfLeaseMonths; }
    public void setNoOfLeaseMonths(Integer noOfLeaseMonths) { this.noOfLeaseMonths = noOfLeaseMonths; }
    public BigDecimal getAmountEachMonth() { return amountEachMonth; }
    public void setAmountEachMonth(BigDecimal amountEachMonth) { this.amountEachMonth = amountEachMonth; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public String getVehicleLocation() { return vehicleLocation; }
    public void setVehicleLocation(String vehicleLocation) { this.vehicleLocation = vehicleLocation; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public BigDecimal getFarePerKm() { return farePerKm; }
    public void setFarePerKm(BigDecimal farePerKm) { this.farePerKm = farePerKm; }
    public Long getVehicleAverage() { return vehicleAverage; }
    public void setVehicleAverage(Long vehicleAverage) { this.vehicleAverage = vehicleAverage; }
    public String getInsuranceNo() { return insuranceNo; }
    public void setInsuranceNo(String insuranceNo) { this.insuranceNo = insuranceNo; }
    public Timestamp getInsuranceExpiryDate() { return insuranceExpiryDate; }
    public void setInsuranceExpiryDate(Timestamp insuranceExpiryDate) { this.insuranceExpiryDate = insuranceExpiryDate; }
    public String getPucNo() { return pucNo; }
    public void setPucNo(String pucNo) { this.pucNo = pucNo; }
    public Timestamp getPucExpiryDate() { return pucExpiryDate; }
    public void setPucExpiryDate(Timestamp pucExpiryDate) { this.pucExpiryDate = pucExpiryDate; }
    public Boolean getIs108() { return is108; }
    public void setIs108(Boolean is108) { this.is108 = is108; }
}
