package com.hims.pmrd.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "jsy_form_data")
public class JsyFormData {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_type")
    private String formType;

    @Column(name = "pcts_no")
    private String pctsNo;

    @Column(name = "caste_id")
    private Long casteId;

    @Column(name = "ration_card_number")
    private String rationCardNumber;

    @Column(name = "bpl_number")
    private String bplNumber;

    @Column(name = "res_city_id")
    private Long resCityId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "status")
    private String status = "SUBMITTED";

    @Column(name = "submitted_date")
    private String submittedDate;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_date")
    private String approvedDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "form_data", columnDefinition = "TEXT")
    private String formData;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public JsyFormData() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFormType() { return formType; }
    public void setFormType(String formType) { this.formType = formType; }
    public String getPctsNo() { return pctsNo; }
    public void setPctsNo(String pctsNo) { this.pctsNo = pctsNo; }
    public Long getCasteId() { return casteId; }
    public void setCasteId(Long casteId) { this.casteId = casteId; }
    public String getRationCardNumber() { return rationCardNumber; }
    public void setRationCardNumber(String rationCardNumber) { this.rationCardNumber = rationCardNumber; }
    public String getBplNumber() { return bplNumber; }
    public void setBplNumber(String bplNumber) { this.bplNumber = bplNumber; }
    public Long getResCityId() { return resCityId; }
    public void setResCityId(Long resCityId) { this.resCityId = resCityId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(String submittedDate) { this.submittedDate = submittedDate; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public String getApprovedDate() { return approvedDate; }
    public void setApprovedDate(String approvedDate) { this.approvedDate = approvedDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getFormData() { return formData; }
    public void setFormData(String formData) { this.formData = formData; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}