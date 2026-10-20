package com.hims.billing.application;

import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.VisitCharge;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * M06 Doctor Share service interface.
 * Handles share calculation logic (percentage-based + fixed amounts),
 * visit charges, and doctor incentives.
 */
public interface DoctorShareService {

    // ---- Doctor Share CRUD ----
    Page<DoctorShare> listShares(int page, int size);
    DoctorShare getShare(Long shareId);
    DoctorShare createShare(DoctorShare share);
    DoctorShare markPaid(Long id);
    List<DoctorShare> getSharesByBillId(Long billId);
    List<DoctorShare> getSharesByDoctorId(Long doctorId);

    // ---- Share Calculation (percentage-based + fixed amounts) ----
    /**
     * Calculate doctor share from a charge line.
     * BR-BILL-002: share = totalAmount * pct / 100 CEILING scale 2;
     * hospital = total - share HALF_UP scale 2.
     */
    DoctorShareCalculation calculateShare(BigDecimal totalAmount, BigDecimal sharePercentage, BigDecimal fixedAmount);

    /**
     * Calculate doctor share for a bill with all its charges.
     */
    List<DoctorShareCalculation> calculateSharesForBill(Long billId);

    // ---- Visit Charges ----
    VisitCharge createVisitCharge(VisitCharge visitCharge);
    VisitCharge getVisitCharge(Long id);
    List<VisitCharge> getVisitChargesByDoctorId(Long doctorId);
    List<VisitCharge> getVisitChargesByDoctorAndVisitType(Long doctorId, Long visitType);
    List<VisitCharge> getVisitChargesByServiceId(Long serviceId);
    void deleteVisitCharge(Long id);

    // ---- Doctor Incentives (Add/Deduct) ----
    DoctorIncentive addIncentive(DoctorIncentive incentive);
    DoctorIncentive deductIncentive(DoctorIncentive incentive);
    List<DoctorIncentive> getIncentivesByDoctorId(Long doctorId);
    List<DoctorIncentive> getUnbilledIncentivesByDoctorId(Long doctorId);
    DoctorIncentive markIncentiveBilled(Long id);

    // ---- Bills ----
    List<DoctorShare> getBillsByDoctorId(Long doctorId);

    /**
     * Result record for share calculation.
     */
    record DoctorShareCalculation(
            Long chargeId,
            Long doctorId,
            BigDecimal totalAmount,
            BigDecimal sharePercentage,
            BigDecimal fixedAmount,
            BigDecimal shareAmount,
            BigDecimal hospitalAmount,
            String calculationType  // PERCENTAGE or FIXED
    ) {}
}
