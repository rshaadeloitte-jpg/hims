package com.hims.kiosk.domain.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "kiosk_registration", schema = "ihms_kiosk")
public class KioskRegistration extends KioskBase {
    @Id
    private Long id;
    @Column(name = "patient_name", nullable = false)
    private String patientName;
    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;
    @Column(name = "email")
    private String email;
    @Column(name = "age")
    private Integer age;
    @Column(name = "gender")
    private String gender;
    @Column(name = "registration_date", nullable = false)
    private LocalDate registrationDate;
    @Column(name = "registration_token", nullable = false, unique = true)
    private String registrationToken;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDate registrationDate) { this.registrationDate = registrationDate; }
    public String getRegistrationToken() { return registrationToken; }
    public void setRegistrationToken(String registrationToken) { this.registrationToken = registrationToken; }
}
