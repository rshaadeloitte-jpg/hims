package com.hims.patient.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;

/** Clean target of T_REGISTRATION (kickoff demographic subset). One row = one MRN. */
@Entity
@Table(name = "patient_registration")
public class PatientRegistration extends PatientBase {

    @Id
    private Long id;

    @Column(name = "mrno", nullable = false)
    private String mrno;

    @Column(name = "is_temp_mrno", nullable = false)
    private Boolean isTempMrno = false;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "email")
    private String email;

    @Column(name = "aadhaar_no")
    private String aadhaarNo;

    @Column(name = "vid")
    private String vid;

    @Column(name = "janaadhaar_id")
    private String janaadhaarId;

    @Column(name = "health_id")
    private String healthId;

    @Column(name = "health_id_number")
    private String healthIdNumber;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "state")
    private String state;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone")
    private String emergencyContactPhone;

    @Column(name = "place_of_birth")
    private String placeOfBirth;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "civil_id")
    private String civilId;

    @Column(name = "passport_no")
    private String passportNo;

    @Column(name = "marital_status")
    private String maritalStatus;

    @Column(name = "religion")
    private String religion;

    @Column(name = "is_death")
    private Boolean isDeath = false;

    @Column(name = "photo_ref")
    private String photoRef;

    @Column(name = "family_id")
    private String familyId;

    @Column(name = "family_member_id")
    private String familyMemberId;

    @Column(name = "covid_status")
    private String covidStatus;

    @Column(name = "covid_updated_at")
    private Timestamp covidUpdatedAt;

    @PrePersist
    void onCreate() {
        if (addedDatetime == null) addedDatetime = Timestamp.from(Instant.now());
    }

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getMrno() { return mrno; }
    public void setMrno(String mrno) { this.mrno = mrno; }
    public Boolean getIsTempMrno() { return isTempMrno; }
    public void setIsTempMrno(Boolean isTempMrno) { this.isTempMrno = isTempMrno; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAadhaarNo() { return aadhaarNo; }
    public void setAadhaarNo(String aadhaarNo) { this.aadhaarNo = aadhaarNo; }
    public String getVid() { return vid; }
    public void setVid(String vid) { this.vid = vid; }
    public String getJanaadhaarId() { return janaadhaarId; }
    public void setJanaadhaarId(String janaadhaarId) { this.janaadhaarId = janaadhaarId; }
    public String getHealthId() { return healthId; }
    public void setHealthId(String healthId) { this.healthId = healthId; }
    public String getHealthIdNumber() { return healthIdNumber; }
    public void setHealthIdNumber(String healthIdNumber) { this.healthIdNumber = healthIdNumber; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getPincode() { return pincode; }
    public void setPincode(String pincode) { this.pincode = pincode; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getPlaceOfBirth() { return placeOfBirth; }
    public void setPlaceOfBirth(String placeOfBirth) { this.placeOfBirth = placeOfBirth; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getCivilId() { return civilId; }
    public void setCivilId(String civilId) { this.civilId = civilId; }
    public String getPassportNo() { return passportNo; }
    public void setPassportNo(String passportNo) { this.passportNo = passportNo; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }
    public Boolean getIsDeath() { return isDeath; }
    public void setIsDeath(Boolean isDeath) { this.isDeath = isDeath; }
    public String getPhotoRef() { return photoRef; }
    public void setPhotoRef(String photoRef) { this.photoRef = photoRef; }
    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }
    public String getFamilyMemberId() { return familyMemberId; }
    public void setFamilyMemberId(String familyMemberId) { this.familyMemberId = familyMemberId; }
    public String getCovidStatus() { return covidStatus; }
    public void setCovidStatus(String covidStatus) { this.covidStatus = covidStatus; }
    public Timestamp getCovidUpdatedAt() { return covidUpdatedAt; }
    public void setCovidUpdatedAt(Timestamp covidUpdatedAt) { this.covidUpdatedAt = covidUpdatedAt; }
}
