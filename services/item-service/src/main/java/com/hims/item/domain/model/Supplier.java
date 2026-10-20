package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Supplier master — maps onto the EXISTING ihms_item.supplier table created by
 * V1__init_ihms_item.sql (ONE-TABLE-ONE-OWNER SS56: exactly one supplier master;
 * no separate t_supplier_master is introduced). Server-owned id from
 * ihms_item.supplier_seq per IMP-DEC-003 (assigned before persist).
 */
@Entity
@Table(name = "supplier")
public class Supplier extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false) private String code;
    @Column(name = "description", nullable = false) private String description;
    @Column(name = "address_line1") private String addressLine1;
    @Column(name = "address_line2") private String addressLine2;
    @Column(name = "city") private String city;
    @Column(name = "state") private String state;
    @Column(name = "country") private String country;
    @Column(name = "pincode") private String pincode;
    @Column(name = "contact_person1_name") private String contactPerson1Name;
    @Column(name = "contact_person1_mobile") private String contactPerson1Mobile;
    @Column(name = "contact_person1_email") private String contactPerson1Email;
    @Column(name = "phone_no") private String phoneNo;
    @Column(name = "fax") private String fax;
    @Column(name = "drug_licence_number") private String drugLicenceNumber;
    @Column(name = "pan_number") private String panNumber;
    @Column(name = "is_manufacturer") private Boolean isManufacturer = false;
    @Column(name = "is_inhouse_supplier") private Boolean isInhouseSupplier = false;
    @Column(name = "is_empanelled") private Boolean isEmpanelled = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getContactPerson1Name() { return contactPerson1Name; }
    public void setContactPerson1Name(String contactPerson1Name) { this.contactPerson1Name = contactPerson1Name; }
    public String getContactPerson1Mobile() { return contactPerson1Mobile; }
    public void setContactPerson1Mobile(String contactPerson1Mobile) { this.contactPerson1Mobile = contactPerson1Mobile; }
    public String getContactPerson1Email() { return contactPerson1Email; }
    public void setContactPerson1Email(String contactPerson1Email) { this.contactPerson1Email = contactPerson1Email; }
    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }
    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }
    public String getDrugLicenceNumber() { return drugLicenceNumber; }
    public void setDrugLicenceNumber(String drugLicenceNumber) { this.drugLicenceNumber = drugLicenceNumber; }
    public String getPanNumber() { return panNumber; }
    public void setPanNumber(String panNumber) { this.panNumber = panNumber; }
    public Boolean getIsManufacturer() { return isManufacturer; }
    public void setIsManufacturer(Boolean isManufacturer) { this.isManufacturer = isManufacturer; }
    public Boolean getIsInhouseSupplier() { return isInhouseSupplier; }
    public void setIsInhouseSupplier(Boolean isInhouseSupplier) { this.isInhouseSupplier = isInhouseSupplier; }
    public Boolean getIsEmpanelled() { return isEmpanelled; }
    public void setIsEmpanelled(Boolean isEmpanelled) { this.isEmpanelled = isEmpanelled; }
}
