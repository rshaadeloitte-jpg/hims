package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Insurance agent contacts (V1 insurance_agent_master) — clean target of
 * legacy M_INSURANCEAGENT. Same contact shape as the AMC agency master.
 * Server-owned id from ihms_equipment.insurance_agent_master_seq.
 */
@Entity
@Table(name = "insurance_agent_master")
public class InsuranceAgentMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Agent code (NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Agency name (NOT NULL). */
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_person_number")
    private String contactPersonNumber;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    /** Landline / primary contact number of the agency. */
    @Column(name = "contact_number")
    private String contactNumber;

    /** Agency email (legacy EMAILID). */
    @Column(name = "email_id")
    private String emailId;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getContactPersonNumber() { return contactPersonNumber; }
    public void setContactPersonNumber(String contactPersonNumber) { this.contactPersonNumber = contactPersonNumber; }
    public String getContactPersonEmail() { return contactPersonEmail; }
    public void setContactPersonEmail(String contactPersonEmail) { this.contactPersonEmail = contactPersonEmail; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
