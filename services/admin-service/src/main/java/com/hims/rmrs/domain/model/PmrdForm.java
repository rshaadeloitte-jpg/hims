package com.hims.rmrs.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pmrd_forms")
public class PmrdForm {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pmrd_form_seq")
    @SequenceGenerator(name = "pmrd_form_seq", sequenceName = "pmrd_form_seq", allocationSize = 1)
    private Long id;

    @Column(name = "form_type", nullable = false)
    private String formType; // J1, J2, J3, H_FORM

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "status")
    private String status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFormType() { return formType; }
    public void setFormType(String formType) { this.formType = formType; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
