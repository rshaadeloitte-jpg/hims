package com.hims.billing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Package subscription (M04 gap, minimal slice).
 * Ported from old IHMS T_PATIENTPACKAGESUBSCRIPTION / T_PACKAGECHARGESUBSCRIPTION:
 * patient subscribes to a package (packageId) optionally tied to a bill.
 * Validity window fromDate..toDate, covered amount via billing rule.
 * Additive — no foreign key beyond bill_id for loose coupling.
 */
@Entity
@Table(name = "t_package_subscription", schema = "ihms_billing")
public class PackageSubscription extends BillingBase {

    @Id
    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "bill_id")
    private Long billId;

    @Column(name = "subscription_no", nullable = false, unique = true)
    private String subscriptionNo;

    @Column(name = "from_date")
    private Timestamp fromDate;

    @Column(name = "to_date")
    private Timestamp toDate;

    @Column(name = "covered_amount", precision = 19, scale = 2)
    private BigDecimal coveredAmount = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // sponsor change stub: nullable sponsor fields on subscription for audit trail
    @Column(name = "sponsor_company_id")
    private Long sponsorCompanyId;

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Long getId() { return subscriptionId; }
    public void setId(Long id) { this.subscriptionId = id; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getPackageId() { return packageId; }
    public void setPackageId(Long packageId) { this.packageId = packageId; }
    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public String getSubscriptionNo() { return subscriptionNo; }
    public void setSubscriptionNo(String subscriptionNo) { this.subscriptionNo = subscriptionNo; }
    public Timestamp getFromDate() { return fromDate; }
    public void setFromDate(Timestamp fromDate) { this.fromDate = fromDate; }
    public Timestamp getToDate() { return toDate; }
    public void setToDate(Timestamp toDate) { this.toDate = toDate; }
    public BigDecimal getCoveredAmount() { return coveredAmount; }
    public void setCoveredAmount(BigDecimal coveredAmount) { this.coveredAmount = coveredAmount; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Long getSponsorCompanyId() { return sponsorCompanyId; }
    public void setSponsorCompanyId(Long sponsorCompanyId) { this.sponsorCompanyId = sponsorCompanyId; }
}
