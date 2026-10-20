package com.hims.payments.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dbt_claims")
public class DbtClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "dbt_claim_seq")
    @SequenceGenerator(name = "dbt_claim_seq", sequenceName = "dbt_claim_seq", allocationSize = 1)
    private Long id;

    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;

    @Column(name = "scheme", nullable = false)
    private String scheme;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "status")
    private String status;

    @Column(name = "utr_no")
    private String utrNo;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getScheme() { return scheme; }
    public void setScheme(String scheme) { this.scheme = scheme; }
    public BigDecimal getAmount() { return amount; }
    public void setBigDecimal(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getUtrNo() { return utrNo; }
    public void setUtrNo(String utrNo) { this.utrNo = utrNo; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
