package com.hims.payments.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_payment_transfer")
public class PaymentTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payment_transfer_seq")
    @SequenceGenerator(name = "payment_transfer_seq", sequenceName = "payment_transfer_seq", allocationSize = 1)
    private Long id;

    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;

    @Column(name = "beneficiary_account_no", nullable = false)
    private String beneficiaryAccountNo;

    @Column(name = "beneficiary_ifsc", nullable = false)
    private String beneficiaryIfsc;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "scheme", nullable = false)
    private String scheme;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    @Column(name = "utr_no")
    private String utrNo;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @JsonProperty("transferId")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }

    public String getBeneficiaryAccountNo() { return beneficiaryAccountNo; }
    public void setBeneficiaryAccountNo(String beneficiaryAccountNo) { this.beneficiaryAccountNo = beneficiaryAccountNo; }

    public String getBeneficiaryIfsc() { return beneficiaryIfsc; }
    public void setBeneficiaryIfsc(String beneficiaryIfsc) { this.beneficiaryIfsc = beneficiaryIfsc; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getUtrNo() { return utrNo; }
    public void setUtrNo(String utrNo) { this.utrNo = utrNo; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }

    @JsonProperty("createdAt")
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @JsonProperty("updatedAt")
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @JsonProperty("completedAt")
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}