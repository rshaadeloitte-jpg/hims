package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Service agent details (V1 service_agent_master) — clean target of legacy
 * M_SERVICEAGENTDETAILS. Individual service engineer/technician contacts.
 * Server-owned id from ihms_equipment.service_agent_master_seq.
 */
@Entity
@Table(name = "service_agent_master")
public class ServiceAgentMaster extends EquipmentBase {

    @Id
    private Long id;

    /** Agent code (NOT NULL). */
    @Column(name = "code", nullable = false)
    private String code;

    /** Agent name (NOT NULL). */
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "designation")
    private String designation;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "mobile_no")
    private String mobileNo;

    /** Agent email (legacy EMAILID). */
    @Column(name = "email_id")
    private String emailId;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }
}
