package com.hims.billing.application;

import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.DoctorIncentive;
import com.hims.billing.domain.model.VisitCharge;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.DoctorIncentiveRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.VisitChargeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DoctorShareServiceImpl implements DoctorShareService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MONEY_SCALE = 2;

    private final DoctorShareRepository doctorShareRepository;
    private final VisitChargeRepository visitChargeRepository;
    private final DoctorIncentiveRepository doctorIncentiveRepository;
    private final ChargeRepository chargeRepository;
    private final EntityManager em;

    public DoctorShareServiceImpl(DoctorShareRepository doctorShareRepository,
                                   VisitChargeRepository visitChargeRepository,
                                   DoctorIncentiveRepository doctorIncentiveRepository,
                                   ChargeRepository chargeRepository,
                                   EntityManager em) {
        this.doctorShareRepository = doctorShareRepository;
        this.visitChargeRepository = visitChargeRepository;
        this.doctorIncentiveRepository = doctorIncentiveRepository;
        this.chargeRepository = chargeRepository;
        this.em = em;
    }

    // ---- Doctor Share CRUD ----

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorShare> listShares(int page, int size) {
        return doctorShareRepository.findAll(PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorShare getShare(Long shareId) {
        return doctorShareRepository.findById(shareId)
                .orElseThrow(() -> new BillingEntityNotFoundException("DOCTOR_SHARE_NOT_FOUND", "Doctor share not found: " + shareId));
    }

    @Override
    public DoctorShare createShare(DoctorShare share) {
        if (share.getShareId() == null) {
            share.setShareId(nextSeq("t_doctor_share_seq"));
        }
        if (share.getIsBilled() == null) {
            share.setIsBilled(false);
        }
        return doctorShareRepository.save(share);
    }

    @Override
    public DoctorShare markPaid(Long id) {
        DoctorShare share = doctorShareRepository.findById(id)
                .orElseThrow(() -> new BillingEntityNotFoundException("DOCTOR_SHARE_NOT_FOUND", "Doctor share not found: " + id));
        share.setIsBilled(true);
        return doctorShareRepository.save(share);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorShare> getSharesByBillId(Long billId) {
        return doctorShareRepository.findByBillIdOrderByShareIdAsc(billId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorShare> getSharesByDoctorId(Long doctorId) {
        return doctorShareRepository.findByDoctorIdOrderByShareIdAsc(doctorId);
    }

    // ---- Share Calculation ----

    @Override
    public DoctorShareCalculation calculateShare(BigDecimal totalAmount, BigDecimal sharePercentage, BigDecimal fixedAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidBillingRequestException("INVALID_TOTAL_AMOUNT", "totalAmount must be non-negative");
        }

        BigDecimal shareAmount;
        BigDecimal hospitalAmount;
        String calculationType;

        // Fixed amount takes priority if specified
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) > 0) {
            shareAmount = fixedAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            calculationType = "FIXED";
            // Cap fixed share at total amount
            if (shareAmount.compareTo(totalAmount) > 0) {
                shareAmount = totalAmount;
            }
            hospitalAmount = totalAmount.subtract(shareAmount);
        } else if (sharePercentage != null && sharePercentage.compareTo(BigDecimal.ZERO) > 0) {
            // BR-BILL-002: share = totalAmount * pct/100 CEILING scale 2
            shareAmount = totalAmount.multiply(sharePercentage)
                    .divide(HUNDRED, MONEY_SCALE, RoundingMode.CEILING);
            calculationType = "PERCENTAGE";
            hospitalAmount = totalAmount.subtract(shareAmount);
        } else {
            shareAmount = BigDecimal.ZERO;
            hospitalAmount = totalAmount;
            calculationType = "NONE";
        }

        return new DoctorShareCalculation(
                null, null, totalAmount, sharePercentage, fixedAmount,
                shareAmount, hospitalAmount, calculationType
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorShareCalculation> calculateSharesForBill(Long billId) {
        List<Charge> charges = chargeRepository.findByBillIdOrderByChargeIdAsc(billId);
        List<DoctorShareCalculation> results = new ArrayList<>();

        for (Charge charge : charges) {
            if (charge.getDoctorSharePct() != null
                    && charge.getDoctorSharePct().compareTo(BigDecimal.ZERO) > 0
                    && !Boolean.TRUE.equals(charge.getIsOtProcedure())) {
                BigDecimal lineTotal = charge.getRate().multiply(BigDecimal.valueOf(charge.getQty()));
                DoctorShareCalculation calc = calculateShare(lineTotal, charge.getDoctorSharePct(), null);
                // Rebuild with charge-specific info
                results.add(new DoctorShareCalculation(
                        charge.getChargeId(),
                        findPrescriberDoctorId(billId),
                        lineTotal,
                        charge.getDoctorSharePct(),
                        null,
                        calc.shareAmount(),
                        calc.hospitalAmount(),
                        calc.calculationType()
                ));
            }
        }
        return results;
    }

    private Long findPrescriberDoctorId(Long billId) {
        try {
            Query q = em.createNativeQuery(
                    "SELECT prescriber_doctor_id FROM ihms_billing.t_bill WHERE bill_id = ?");
            q.setParameter(1, billId);
            Object result = q.getSingleResult();
            return result != null ? ((Number) result).longValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- Visit Charges ----

    @Override
    public VisitCharge createVisitCharge(VisitCharge visitCharge) {
        if (visitCharge.getVisitChargeId() == null) {
            visitCharge.setVisitChargeId(nextSeq("t_visit_charge_seq"));
        }
        return visitChargeRepository.save(visitCharge);
    }

    @Override
    @Transactional(readOnly = true)
    public VisitCharge getVisitCharge(Long id) {
        return visitChargeRepository.findById(id)
                .orElseThrow(() -> new BillingEntityNotFoundException("VISIT_CHARGE_NOT_FOUND", "Visit charge not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitCharge> getVisitChargesByDoctorId(Long doctorId) {
        return visitChargeRepository.findByDoctorIdOrderByVisitChargeIdAsc(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitCharge> getVisitChargesByDoctorAndVisitType(Long doctorId, Long visitType) {
        return visitChargeRepository.findByDoctorIdAndVisitTypeOrderByVisitChargeIdAsc(doctorId, visitType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitCharge> getVisitChargesByServiceId(Long serviceId) {
        return visitChargeRepository.findByServiceIdOrderByVisitChargeIdAsc(serviceId);
    }

    @Override
    public void deleteVisitCharge(Long id) {
        VisitCharge vc = visitChargeRepository.findById(id)
                .orElseThrow(() -> new BillingEntityNotFoundException("VISIT_CHARGE_NOT_FOUND", "Visit charge not found: " + id));
        visitChargeRepository.delete(vc);
    }

    // ---- Doctor Incentives ----

    @Override
    public DoctorIncentive addIncentive(DoctorIncentive incentive) {
        if (incentive.getIncentiveId() == null) {
            incentive.setIncentiveId(nextSeq("t_doctor_incentive_seq"));
        }
        incentive.setIncentiveType("ADDITION");
        if (incentive.getIsBilled() == null) {
            incentive.setIsBilled(false);
        }
        if (incentive.getIncentiveDate() == null) {
            incentive.setIncentiveDate(new Timestamp(System.currentTimeMillis()));
        }
        return doctorIncentiveRepository.save(incentive);
    }

    @Override
    public DoctorIncentive deductIncentive(DoctorIncentive incentive) {
        if (incentive.getIncentiveId() == null) {
            incentive.setIncentiveId(nextSeq("t_doctor_incentive_seq"));
        }
        incentive.setIncentiveType("DEDUCTION");
        if (incentive.getIsBilled() == null) {
            incentive.setIsBilled(false);
        }
        if (incentive.getIncentiveDate() == null) {
            incentive.setIncentiveDate(new Timestamp(System.currentTimeMillis()));
        }
        return doctorIncentiveRepository.save(incentive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorIncentive> getIncentivesByDoctorId(Long doctorId) {
        return doctorIncentiveRepository.findByDoctorIdOrderByIncentiveIdAsc(doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorIncentive> getUnbilledIncentivesByDoctorId(Long doctorId) {
        return doctorIncentiveRepository.findByDoctorIdAndIsBilledOrderByIncentiveIdAsc(doctorId, false);
    }

    @Override
    public DoctorIncentive markIncentiveBilled(Long id) {
        DoctorIncentive incentive = doctorIncentiveRepository.findById(id)
                .orElseThrow(() -> new BillingEntityNotFoundException("DOCTOR_INCENTIVE_NOT_FOUND", "Doctor incentive not found: " + id));
        incentive.setIsBilled(true);
        return doctorIncentiveRepository.save(incentive);
    }

    // ---- Bills ----

    @Override
    @Transactional(readOnly = true)
    public List<DoctorShare> getBillsByDoctorId(Long doctorId) {
        return doctorShareRepository.findByDoctorIdOrderByShareIdAsc(doctorId);
    }

    // ---- Sequence helper ----

    private Long nextSeq(String sequenceName) {
        Query q = em.createNativeQuery("SELECT nextval('" + sequenceName + "')");
        return ((Number) q.getSingleResult()).longValue();
    }
}
