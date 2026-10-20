package com.hims.reference.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Clean target of M_DOCTORMASTER (AGT-02 §4.5, V5). MIG-011.
 * Audit block overridden to the clean snake_case columns (unit_id, added_by, ...);
 * JSON property names are the camelCase field names (new contract).
 * iam_user_id is a logical reference to the IAM user (MIG-004, D-2) — no cross-schema FK.
 */
@Entity
@Table(name = "doctor")
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id")),
        @AttributeOverride(name = "unitId", column = @Column(name = "unit_id")),
        @AttributeOverride(name = "status", column = @Column(name = "status")),
        @AttributeOverride(name = "createdunitid", column = @Column(name = "created_unit_id")),
        @AttributeOverride(name = "updatedunitid", column = @Column(name = "updated_unit_id")),
        @AttributeOverride(name = "addedby", column = @Column(name = "added_by")),
        @AttributeOverride(name = "addeddatetime", column = @Column(name = "added_datetime")),
        @AttributeOverride(name = "updatedby", column = @Column(name = "updated_by")),
        @AttributeOverride(name = "updateddatetime", column = @Column(name = "updated_datetime")),
        @AttributeOverride(name = "synchronizedd", column = @Column(name = "synchronized"))
})
public class Doctor extends MasterEntityBase {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender_id")
    private Long genderId;

    @Column(name = "dob")
    private java.sql.Date dob;

    @Column(name = "marital_status")
    private Short maritalStatus;

    @Column(name = "mobile_code")
    private Long mobileCode;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "email_id")
    private String emailId;

    @Column(name = "iam_user_id")
    private Long iamUserId;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "pf_number")
    private String pfNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "date_of_joining")
    private java.sql.Date dateOfJoining;

    @Column(name = "access_card_number")
    private String accessCardNumber;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "doctor_no")
    private String doctorNo;

    @Column(name = "prefix_id")
    private Long prefixId;

    @Column(name = "is_nurse")
    private Boolean isNurse = false;

    @Column(name = "education")
    private String education;

    @Column(name = "experience")
    private String experience;

    @Column(name = "specialization_id")
    private Long specializationId;

    @Column(name = "subspecialization_id")
    private Long subspecializationId;

    @Column(name = "doctor_type_id")
    private Long doctorTypeId;

    @Column(name = "is_concession_authority")
    private Boolean isConcessionAuthority = false;

    @Column(name = "is_resident_doctor")
    private Boolean isResidentDoctor = false;

    @Column(name = "is_hod")
    private Boolean isHod = false;

    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "contact_number_one")
    private String contactNumberOne;

    @Column(name = "contact_number_two")
    private String contactNumberTwo;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "city_id")
    private Long cityId;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "area")
    private String area;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public Long getGenderId() { return genderId; }
    public void setGenderId(Long genderId) { this.genderId = genderId; }
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    public String getEmailId() { return emailId; }
    public void setEmailId(String emailId) { this.emailId = emailId; }
    public Long getSpecializationId() { return specializationId; }
    public void setSpecializationId(Long specializationId) { this.specializationId = specializationId; }
    public Long getDoctorTypeId() { return doctorTypeId; }
    public void setDoctorTypeId(Long doctorTypeId) { this.doctorTypeId = doctorTypeId; }
    public Long getIamUserId() { return iamUserId; }
    public void setIamUserId(Long iamUserId) { this.iamUserId = iamUserId; }
}
