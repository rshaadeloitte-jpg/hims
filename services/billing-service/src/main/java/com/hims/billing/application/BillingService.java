package com.hims.billing.application;

import com.hims.billing.application.BillingCalculator.ChargeCalculation;
import com.hims.billing.application.exceptions.BillingEntityNotFoundException;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.CreditNote;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.model.PackageSubscription;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.CreditNoteRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.PackageSubscriptionRepository;
import com.hims.billing.infrastructure.outbox.OutboxPublisher;
import com.hims.billing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Billing & Settlement slice (GAP-REGISTER WP-11, Catalog #5).
 *
 * Lifecycle: TEMP charges (POST /api/v1/charges, bill_id NULL) -> bill header with
 * placeholder billNo 000000000000000 (BR-BILL-001) attaching charges ->
 * finalize (taxes BR-BILL-004 + asymmetric split BR-BILL-005 + doctor share
 * BR-BILL-002 + snapshots + final billNo OPDBILL/IPDBILL-&lt;seq&gt; + E-07
 * InvoiceGenerated ONCE via outbox) -> settlement (BR-BILL-006).
 *
 * E-07 payload keys EXACTLY {billId, billNo, patientId, encounterId|null,
 * admissionId|null, header{total,tax,concession,self,nonSelf},
 * lines[{chargeId,serviceName,rate,qty}],
 * charges{taxTotal,concessionTotal,self,nonSelf},
 * doctorShare{doctorId,shareAmount,hospitalAmount}|null, isInterimOrFinal};
 * type string 'InvoiceGenerated', schema token invoice-generated.v1,
 * partition key billId.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final BillRepository billRepository;
    private final ChargeRepository chargeRepository;
    private final DoctorShareRepository doctorShareRepository;
    private final CreditNoteRepository creditNoteRepository;
    private final PackageSubscriptionRepository packageSubscriptionRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public BillingService(BillRepository billRepository,
                          ChargeRepository chargeRepository,
                          DoctorShareRepository doctorShareRepository,
                          CreditNoteRepository creditNoteRepository,
                          PackageSubscriptionRepository packageSubscriptionRepository,
                          OutboxService outboxService) {
        this.billRepository = billRepository;
        this.chargeRepository = chargeRepository;
        this.doctorShareRepository = doctorShareRepository;
        this.creditNoteRepository = creditNoteRepository;
        this.packageSubscriptionRepository = packageSubscriptionRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ charges

    /** Create a TEMP charge row (not yet on a bill). */
    @Transactional
    public Charge createCharge(NewCharge in) {
        validateNewCharge(in);
        Charge c = new Charge();
        c.setChargeId(nextId("t_charge_seq"));
        c.setServiceId(in.serviceId());
        c.setServiceName(in.serviceName().trim());
        c.setRate(in.rate());
        c.setQty(in.qty() == null ? 1 : in.qty());
        c.setTaxPctA(nzOrZero(in.taxPctA()));
        c.setTaxPctB(nzOrZero(in.taxPctB()));
        c.setTaxPctC(nzOrZero(in.taxPctC()));
        c.setApplyAfterConcession(Boolean.TRUE.equals(in.applyAfterConcession()));
        c.setCoPayPct(nzOrZero(in.coPayPct()));
        c.setConcessionPlanPct(nzOrZero(in.concessionPlanPct()));
        c.setConcessionBillPct(nzOrZero(in.concessionBillPct()));
        c.setDoctorSharePct(nzOrZero(in.doctorSharePct()));
        c.setSpecialization(in.specialization());   // post-saga order-creation marker ONLY
        c.setIsOtProcedure(Boolean.TRUE.equals(in.isOtProcedure()));
        c.setStatus(true);
        c.setSynchronizedd(false);
        c.setAddedBy(AUDIT_ACTOR_ID);
        c.setAddedDatetime(now());
        return chargeRepository.save(c);
    }

    private void validateNewCharge(NewCharge in) {
        if (in == null || in.serviceId() == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "serviceId is required");
        }
        if (in.serviceName() == null || in.serviceName().isBlank()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "serviceName is required");
        }
        if (in.rate() == null || in.rate().signum() < 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "rate must be >= 0");
        }
        if (in.qty() != null && in.qty() <= 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "qty must be > 0");
        }
    }

    // ------------------------------------------------------------------ bills

    /**
     * Create the bill header (BR-BILL-001: Status=true, IsSettled=false, IsCancelled=false,
     * BillNo='000000000000000', CashCounterId default 1) and attach the requested charges.
     */
    @Transactional
    public Bill createBill(Long patientId, Long encounterId, Long admissionId,
                           Long cashCounterId, Long prescriberDoctorId, Boolean selfCompany,
                           List<Long> chargeIds) {
        if (patientId == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "patientId is required");
        }
        if (chargeIds == null || chargeIds.isEmpty()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "At least one chargeId is required");
        }

        Bill bill = new Bill();
        bill.setBillId(nextId("t_bill_seq"));
        bill.setPatientId(patientId);
        bill.setEncounterId(encounterId);
        bill.setAdmissionId(admissionId);
        bill.setCashCounterId(cashCounterId == null ? 1L : cashCounterId);
        bill.setPrescriberDoctorId(prescriberDoctorId);
        bill.setIsSelfCompany(selfCompany == null || selfCompany);   // SELF path by default
        bill.setBillNo(Bill.PLACEHOLDER_BILL_NO);                    // BR-BILL-001 placeholder
        bill.setStatus(true);
        bill.setIsSettled(false);
        bill.setIsCancelled(false);
        bill.setIsInterim(true);
        bill.setSynchronizedd(false);
        bill.setAddedBy(AUDIT_ACTOR_ID);
        bill.setAddedDatetime(now());

        List<Charge> attached = new ArrayList<>();
        for (Long chargeId : chargeIds) {
            Charge charge = chargeRepository.findById(chargeId)
                    .orElseThrow(() -> new InvalidBillingRequestException("BILL_REQ_INVALID",
                            "Charge not found: " + chargeId));
            if (charge.getBillId() != null) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Charge " + chargeId + " is already attached to bill " + charge.getBillId());
            }
            if (Boolean.TRUE.equals(charge.getIsCancelled())) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                        "Charge " + chargeId + " is cancelled");
            }
            charge.setBillId(bill.getBillId());
            charge.setUpdatedBy(AUDIT_ACTOR_ID);
            charge.setUpdatedDatetime(bill.getAddedDatetime());
            attached.add(chargeRepository.save(charge));
        }
        bill = billRepository.save(bill);
        log.info("Created interim bill {} for patient {} with {} charges (placeholder billNo)",
                bill.getBillId(), patientId, attached.size());
        return get(bill.getBillId());
    }

    // ------------------------------------------------------------------ finalize

    /**
     * Finalize ONE interim bill:
     *  - per charge BR-BILL-004 taxes + BR-BILL-005 asymmetric split; snapshot
     *    *BeforeFinalization columns filled;
     *  - BR-BILL-002 doctor share rows (is_billed=false), skipped pct=0 / OT;
     *  - header amounts recomputed (self = sum selfRate + taxes, nonSelf = sum nonSelfRate),
     *    balances initialized to the amounts;
     *  - final billNo from sequence: 'OPDBILL-&lt;seq&gt;' (no admissionId) else 'IPDBILL-&lt;seq&gt;';
     *  - E-07 InvoiceGenerated appended to the outbox EXACTLY ONCE.
     */
    @Transactional
    public Bill finalizeBill(long billId) {
        Bill bill = load(billId);
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Cannot finalize cancelled bill " + billId);
        }
        if (!Boolean.TRUE.equals(bill.getIsInterim())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Bill " + billId + " is already finalized");
        }
        List<Charge> charges = chargeRepository.findByBillIdOrderByChargeIdAsc(billId);
        if (charges.isEmpty()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "Bill " + billId + " has no charges to finalize");
        }

        boolean selfCompany = !Boolean.FALSE.equals(bill.getIsSelfCompany());
        Timestamp nowTs = now();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal concessionTotal = BigDecimal.ZERO;
        BigDecimal selfTotal = BigDecimal.ZERO;
        BigDecimal nonSelfTotal = BigDecimal.ZERO;
        List<ChargeCalculation> calcs = new ArrayList<>();

        for (Charge c : charges) {
            ChargeCalculation calc = BillingCalculator.calculate(
                    c.getRate(), c.getQty(), c.getTaxPctA(), c.getTaxPctB(), c.getTaxPctC(),
                    Boolean.TRUE.equals(c.getApplyAfterConcession()),
                    c.getCoPayPct(), c.getConcessionPlanPct(), c.getConcessionBillPct(),
                    c.getDoctorSharePct(), Boolean.TRUE.equals(c.getIsOtProcedure()),
                    selfCompany);

            // snapshot columns (BR-BILL-008 restore contract)
            c.setTotalBeforeFinalization(calc.lineTotal());
            c.setSelfBeforeFinalization(calc.selfAmount());
            c.setNonselfBeforeFinalization(calc.nonSelfAmount());
            c.setUpdatedBy(AUDIT_ACTOR_ID);
            c.setUpdatedDatetime(nowTs);
            chargeRepository.save(c);

            total = total.add(calc.lineTotal());
            taxTotal = taxTotal.add(calc.taxTotal());
            concessionTotal = concessionTotal.add(calc.concession());
            selfTotal = selfTotal.add(calc.selfAmount());
            nonSelfTotal = nonSelfTotal.add(calc.nonSelfAmount());
            calcs.add(calc);

            if (calc.doctorShare() != null && bill.getPrescriberDoctorId() != null) {
                DoctorShare ds = new DoctorShare();
                ds.setShareId(nextId("t_doctor_share_seq"));
                ds.setBillId(bill.getBillId());
                ds.setChargeId(c.getChargeId());
                ds.setDoctorId(bill.getPrescriberDoctorId());
                ds.setShareAmount(calc.doctorShare());
                ds.setHospitalAmount(calc.hospitalShare());
                ds.setIsBilled(false);                       // CIMS_ADDDOCTORSHARE convention
                ds.setStatus(true);
                ds.setSynchronizedd(false);
                ds.setAddedBy(AUDIT_ACTOR_ID);
                ds.setAddedDatetime(nowTs);
                doctorShareRepository.save(ds);
            }
        }

        bill.setTotalAmount(total);
        bill.setTaxAmount(taxTotal);
        bill.setConcessionAmount(concessionTotal);
        bill.setSelfAmount(selfTotal);           // = sum(selfRate) + TaxAmount
        bill.setNonSelfAmount(nonSelfTotal);     // = sum(nonSelfRate)
        bill.setBalanceSelf(selfTotal);          // balances initialized to the amounts
        bill.setBalanceNonSelf(nonSelfTotal);
        bill.setChangeAmount(BigDecimal.ZERO);
        bill.setIsInterim(false);
        bill.setFinalizedAt(nowTs);
        bill.setBillNo(nextBillNo(admissionIdOf(bill)));   // BR-BILL-001 final number
        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(nowTs);
        bill = billRepository.save(bill);

        for (int i = 0; i < charges.size(); i++) {
            charges.get(i).setCalculations(List.of(calcs.get(i)));
        }
        bill.setCharges(charges);

        emitInvoiceGenerated(bill, charges, nowTs);
        log.info("Finalized bill {} as {} (total {}, tax {}, concession {}, self {}, nonSelf {})",
                billId, bill.getBillNo(), total, taxTotal, concessionTotal, selfTotal, nonSelfTotal);
        return get(billId);
    }

    private Long admissionIdOf(Bill bill) {
        return bill.getAdmissionId();
    }

    /** BR-BILL-001: 'OPDBILL-&lt;seq&gt;' when no admissionId, else 'IPDBILL-&lt;seq&gt;'. */
    private String nextBillNo(Long admissionId) {
        String seq = admissionId == null ? "t_opdbill_seq" : "t_ipdbill_seq";
        String prefix = admissionId == null ? "OPDBILL-" : "IPDBILL-";
        return prefix + nextVal(seq);
    }

    /**
     * E-07 InvoiceGenerated — payload keys EXACTLY {billId, billNo, patientId,
     * encounterId|null, admissionId|null, header{total,tax,concession,self,nonSelf},
     * lines[{chargeId,serviceName,rate,qty}], charges{taxTotal,concessionTotal,self,nonSelf},
     * doctorShare{doctorId,shareAmount,hospitalAmount}|null, isInterimOrFinal:'FINAL'};
     * partition key billId. Fired ONCE per bill (finalize guard makes re-entry illegal).
     */
    void emitInvoiceGenerated(Bill bill, List<Charge> charges, Timestamp finalizedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billId", String.valueOf(bill.getBillId()));
        payload.put("billNo", bill.getBillNo());
        payload.put("patientId", String.valueOf(bill.getPatientId()));
        payload.put("encounterId", bill.getEncounterId() == null
                ? null : String.valueOf(bill.getEncounterId()));
        payload.put("admissionId", bill.getAdmissionId() == null
                ? null : String.valueOf(bill.getAdmissionId()));

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("total", bill.getTotalAmount());
        header.put("tax", bill.getTaxAmount());
        header.put("concession", bill.getConcessionAmount());
        header.put("self", bill.getSelfAmount());
        header.put("nonSelf", bill.getNonSelfAmount());
        payload.put("header", header);

        List<Map<String, Object>> lines = new ArrayList<>();
        for (Charge c : charges) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("chargeId", String.valueOf(c.getChargeId()));
            line.put("serviceName", c.getServiceName());
            line.put("rate", c.getRate());
            line.put("qty", c.getQty());
            lines.add(line);
        }
        payload.put("lines", lines);

        Map<String, Object> chargesBlock = new LinkedHashMap<>();
        chargesBlock.put("taxTotal", bill.getTaxAmount());
        chargesBlock.put("concessionTotal", bill.getConcessionAmount());
        chargesBlock.put("self", bill.getSelfAmount());
        chargesBlock.put("nonSelf", bill.getNonSelfAmount());
        payload.put("charges", chargesBlock);

        // aggregate doctor share of the bill (all rows carry the prescriber doctor id)
        List<DoctorShare> shares = doctorShareRepository.findByBillIdOrderByShareIdAsc(bill.getBillId());
        if (shares.isEmpty()) {
            payload.put("doctorShare", null);
        } else {
            Map<String, Object> ds = new LinkedHashMap<>();
            ds.put("doctorId", String.valueOf(shares.get(0).getDoctorId()));
            ds.put("shareAmount", shares.stream()
                    .map(DoctorShare::getShareAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            ds.put("hospitalAmount", shares.stream()
                    .map(DoctorShare::getHospitalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            payload.put("doctorShare", ds);
        }

        payload.put("isInterimOrFinal", "FINAL");

        outboxService.append(OutboxPublisher.BillingEvents.INVOICE_GENERATED, "bill",
                bill.getBillId(), String.valueOf(bill.getBillId()), toJson(payload),
                "invoice-generated:" + bill.getBillId());
        log.info("Emitted {} for bill {} ({})", OutboxPublisher.BillingEvents.INVOICE_GENERATED,
                bill.getBillId(), bill.getBillNo());
    }

    // ------------------------------------------------------------------ concession

    /**
     * Apply plan/bill concession percentages to ALL charges of an INTERIM bill and
     * return it (amounts are computed at finalize). 400 BILL_ALREADY_FINALIZED after
     * finalize (pre-finalize only per register WP-11).
     * <p>
     * Emits {@code ConcessionApproved} (schema {@code concession-approved.v1}) to
     * {@code ihms.billing.events} with the applied percentages.
     */
    @Transactional
    public Bill applyConcession(long billId, BigDecimal planPct, BigDecimal billPct) {
        return applyConcession(billId, planPct, billPct, null, null, null);
    }

    /**
     * Apply concession with flat amount and per-item overrides (legacy BR-BILL-004).
     *
     * @param billId              bill to apply concession to
     * @param planPct             plan-level concession percentage (can be null)
     * @param billPct             bill-level concession percentage (can be null)
     * @param flatConcessionAmount flat amount to deduct (legacy concessionAmount, can be null)
     * @param chargeOverrides     per-item charge overrides (chargeId → concessionPct, can be null)
     * @param authorisedBy        user who authorised this concession (legacy concessionAuthorisedBy, can be null)
     */
    @Transactional
    public Bill applyConcession(long billId, BigDecimal planPct, BigDecimal billPct,
                                BigDecimal flatConcessionAmount,
                                Map<Long, BigDecimal> chargeOverrides,
                                Long authorisedBy) {
        Bill bill = load(billId);
        if (!Boolean.TRUE.equals(bill.getIsInterim())) {
            throw new InvalidBillingRequestException("BILL_ALREADY_FINALIZED",
                    "Concession can only be applied before finalization (bill " + billId + ")");
        }
        if ((planPct != null && planPct.signum() < 0) || (billPct != null && billPct.signum() < 0)) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "concessionPlanPct and concessionBillPct must be >= 0");
        }
        if (flatConcessionAmount != null && flatConcessionAmount.signum() < 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "flatConcessionAmount must be >= 0");
        }
        Timestamp nowTs = now();
        List<Charge> charges = chargeRepository.findByBillIdOrderByChargeIdAsc(billId);
        if (charges.isEmpty()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "Bill " + billId + " has no charges");
        }

        // Calculate total before concession for flat amount distribution
        BigDecimal totalBeforeConcession = charges.stream()
                .map(c -> c.getRate().multiply(BigDecimal.valueOf(c.getQty())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (Charge c : charges) {
            // Apply per-item override if present
            BigDecimal effectivePlanPct = planPct;
            BigDecimal effectiveBillPct = billPct;
            if (chargeOverrides != null && chargeOverrides.containsKey(c.getChargeId())) {
                effectivePlanPct = chargeOverrides.get(c.getChargeId());
                effectiveBillPct = BigDecimal.ZERO; // Override replaces billPct
            }
            c.setConcessionPlanPct(effectivePlanPct != null ? effectivePlanPct : BigDecimal.ZERO);
            c.setConcessionBillPct(effectiveBillPct != null ? effectiveBillPct : BigDecimal.ZERO);
            c.setUpdatedBy(AUDIT_ACTOR_ID);
            c.setUpdatedDatetime(nowTs);
            chargeRepository.save(c);
        }

        // Apply flat concession amount proportionally across charges
        if (flatConcessionAmount != null && flatConcessionAmount.compareTo(BigDecimal.ZERO) > 0
                && totalBeforeConcession.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal remainingFlat = flatConcessionAmount;
            for (int i = 0; i < charges.size(); i++) {
                Charge c = charges.get(i);
                BigDecimal lineTotal = c.getRate().multiply(BigDecimal.valueOf(c.getQty()));
                BigDecimal flatForLine;
                if (i == charges.size() - 1) {
                    // Last line gets remaining to avoid rounding errors
                    flatForLine = remainingFlat;
                } else {
                    flatForLine = flatConcessionAmount.multiply(lineTotal)
                            .divide(totalBeforeConcession, 2, java.math.RoundingMode.HALF_UP);
                }
                flatForLine = flatForLine.min(lineTotal); // Don't exceed line total
                c.setFlatConcessionAmount(flatForLine);
                remainingFlat = remainingFlat.subtract(flatForLine);
                chargeRepository.save(c);
            }
        }

        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(nowTs);
        bill.setConcessionAuthorisedBy(authorisedBy);
        billRepository.save(bill);

        emitConcessionApproved(bill, planPct, billPct);
        log.info("Applied concession plan%={} bill%={} flat={} to interim bill {} by user {}",
                planPct, billPct, flatConcessionAmount, billId, authorisedBy);
        return get(billId);
    }

    // ------------------------------------------------------------------ settlement

    /**
     * BR-BILL-006 — synchronous settlement called by payments-service with the
     * CUMULATIVE paid amounts. Balances are RECOMPUTED (self_amount - paidSelf,
     * non_self_amount - paidNonSelf), so replays are idempotent; settled &lt;=&gt; BOTH
     * balances &lt;= 0. Emits {@code BillSettled} (E-07 companion) ONCE on the first
     * transition to settled (replays of an already-settled bill emit nothing).
     * Returns the new state.
     */
    @Transactional
    public Bill settle(long billId, BigDecimal paidSelf, BigDecimal paidNonSelf) {
        Bill bill = load(billId);
        if (paidSelf == null || paidSelf.signum() < 0
                || paidNonSelf == null || paidNonSelf.signum() < 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "paidSelf and paidNonSelf must be non-negative cumulative amounts");
        }
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Cannot settle cancelled bill " + billId);
        }
        // capture the prior isSettled flag so we can detect the FIRST transition
        // (replay of an already-settled bill must NOT emit a duplicate event)
        boolean wasSettled = Boolean.TRUE.equals(bill.getIsSettled());

        bill.setBalanceSelf(bill.getSelfAmount().subtract(paidSelf));
        bill.setBalanceNonSelf(bill.getNonSelfAmount().subtract(paidNonSelf));
        boolean settled = bill.getBalanceSelf().signum() <= 0
                && bill.getBalanceNonSelf().signum() <= 0;   // BR-BILL-006
        bill.setIsSettled(settled);
        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(now());
        bill = billRepository.save(bill);

        // BillSettled emitted EXACTLY ONCE on the first transition to settled
        if (settled && !wasSettled) {
            emitBillSettled(bill, paidSelf, paidNonSelf);
        }
        log.info("Settlement recompute for bill {}: balanceSelf={}, balanceNonSelf={}, settled={}",
                billId, bill.getBalanceSelf(), bill.getBalanceNonSelf(), settled);
        return get(billId);
    }

    // ------------------------------------------------------------------ event emission helpers

    /**
     * Emit {@code BillSettled} (schema {@code bill-settled.v1}) — payload keys:
     * {billId, billNo, patientId, encounterId|null, admissionId|null,
     *  settledAt, paidSelf, paidNonSelf, balanceSelf, balanceNonSelf, changeAmount}.
     * Partition key: billId. Emitted ONCE on first transition to settled.
     */
    private void emitBillSettled(Bill bill, BigDecimal paidSelf, BigDecimal paidNonSelf) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billId", String.valueOf(bill.getBillId()));
        payload.put("billNo", bill.getBillNo());
        payload.put("patientId", String.valueOf(bill.getPatientId()));
        payload.put("encounterId", bill.getEncounterId() == null
                ? null : String.valueOf(bill.getEncounterId()));
        payload.put("admissionId", bill.getAdmissionId() == null
                ? null : String.valueOf(bill.getAdmissionId()));
        payload.put("settledAt", now().toString());
        payload.put("paidSelf", paidSelf);
        payload.put("paidNonSelf", paidNonSelf);
        payload.put("balanceSelf", bill.getBalanceSelf());
        payload.put("balanceNonSelf", bill.getBalanceNonSelf());
        payload.put("changeAmount", bill.getChangeAmount());

        outboxService.append(OutboxPublisher.BillingEvents.BILL_SETTLED, "bill",
                bill.getBillId(), String.valueOf(bill.getBillId()), toJson(payload),
                "bill-settled:" + bill.getBillId());
        log.info("Emitted {} for bill {} ({})", OutboxPublisher.BillingEvents.BILL_SETTLED,
                bill.getBillId(), bill.getBillNo());
    }

    /**
     * Emit {@code ConcessionApproved} (schema {@code concession-approved.v1}) — payload keys:
     * {billId, billNo, patientId, concessionPlanPct, concessionBillPct, appliedAt}.
     * Partition key: billId.
     */
    private void emitConcessionApproved(Bill bill, BigDecimal planPct, BigDecimal billPct) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billId", String.valueOf(bill.getBillId()));
        payload.put("billNo", bill.getBillNo());
        payload.put("patientId", String.valueOf(bill.getPatientId()));
        payload.put("concessionPlanPct", planPct);
        payload.put("concessionBillPct", billPct);
        payload.put("appliedAt", now().toString());

        outboxService.append(OutboxPublisher.BillingEvents.CONCESSION_APPROVED, "bill",
                bill.getBillId(), String.valueOf(bill.getBillId()), toJson(payload),
                "concession-approved:" + bill.getBillId());
        log.info("Emitted {} for bill {} ({}) plan%={} bill%={}",
                OutboxPublisher.BillingEvents.CONCESSION_APPROVED, bill.getBillId(), bill.getBillNo(), planPct, billPct);
    }

    // ------------------------------------------------------------------ BR-BILL-008: Settled bill cancellation with charge restoration

    /**
     * Cancel a SETTLED bill (BR-BILL-008) — restores charges from *BeforeFinalization
     * snapshots, marks bill as cancelled, restores advance balances via FIFO reversal,
     * sets doctor-share is_billed=false, resets T_ITEMSALE bill linkage.
     * <p>
     * This is the server-side implementation of the legacy
     * {@code CIMS_SETTLEDBILLCANCELLATION} procedure.
     * <p>
     * Returns the cancelled bill with restored charge snapshots for audit.
     */
    @Transactional
    public Bill cancelSettledBill(long billId, String reason) {
        Bill bill = load(billId);
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Bill " + billId + " is already cancelled");
        }
        if (!Boolean.TRUE.equals(bill.getIsSettled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "BR-BILL-008 applies only to SETTLED bills (bill " + billId + ")");
        }
        Timestamp nowTs = now();

        // 1. Restore charge snapshots (BR-BILL-008: *BeforeFinalization columns)
        List<Charge> charges = chargeRepository.findByBillIdOrderByChargeIdAsc(billId);
        for (Charge c : charges) {
            if (c.getTotalBeforeFinalization() != null) {
                // Restore the charge to pre-finalization state for refund/cancellation audit
                c.setIsCancelled(true);
                c.setUpdatedBy(AUDIT_ACTOR_ID);
                c.setUpdatedDatetime(nowTs);
                chargeRepository.save(c);
            }
        }

        // 2. Reset doctor share is_billed=false (BR-BILL-008)
        List<DoctorShare> shares = doctorShareRepository.findByBillIdOrderByShareIdAsc(billId);
        for (DoctorShare ds : shares) {
            ds.setIsBilled(false);
            ds.setUpdatedBy(AUDIT_ACTOR_ID);
            ds.setUpdatedDatetime(nowTs);
            doctorShareRepository.save(ds);
        }

        // 3. Mark bill cancelled (advance balance restoration is handled by payments-service
        //    via refund transactionType=3 which decrements advance.amount; here we only
        //    record the bill cancellation. The advance FIFO reversal is a cross-service
        //    saga step documented in GAP-REGISTER WP-13.)
        bill.setIsCancelled(true);
        bill.setIsSettled(false);   // cancelled bills are no longer settled
        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(nowTs);
        billRepository.save(bill);

        log.info("BR-BILL-008 cancelled settled bill {}: charges restored from snapshots, doctor shares unbilled, reason={}",
                billId, reason);
        return get(billId);
    }

    // --------------------------------------------------------------- credit notes

    /**
     * Issue a credit note against a finalized bill (M04 gap).
     * Ported from old IHMS: GetCreditNoteDetailsByBillId.
     * Credit note amount must not exceed the bill total minus existing credit notes.
     */
    @Transactional
    public CreditNote issueCreditNote(long billId, BigDecimal amount, String reason) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "Credit note amount must be > 0");
        }
        Bill bill = load(billId);
        if (Boolean.TRUE.equals(bill.getIsInterim())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Credit notes can only be issued against finalized bills (bill " + billId + ")");
        }
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Cannot issue credit note against cancelled bill " + billId);
        }
        List<CreditNote> existing = creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(billId);
        BigDecimal existingTotal = existing.stream()
                .map(CreditNote::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingTotal.add(amount).compareTo(bill.getTotalAmount()) > 0) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID",
                    "Credit note amount exceeds remaining bill balance (total=" + bill.getTotalAmount()
                            + ", existing credits=" + existingTotal + ", requested=" + amount + ")");
        }

        CreditNote cn = new CreditNote();
        cn.setCreditNoteId(nextId("t_credit_note_seq"));
        cn.setBillId(billId);
        cn.setPatientId(bill.getPatientId());
        cn.setCreditNoteNo("CN-" + nextVal("t_credit_note_no_seq"));
        cn.setReason(reason);
        cn.setAmount(amount);
        cn.setIsApplied(false);
        cn.setStatus(true);
        cn.setSynchronizedd(false);
        cn.setAddedBy(AUDIT_ACTOR_ID);
        cn.setAddedDatetime(now());
        cn = creditNoteRepository.save(cn);

        log.info("Issued credit note {} for bill {} amount={} reason={}",
                cn.getCreditNoteNo(), billId, amount, reason);
        return cn;
    }

    @Transactional(readOnly = true)
    public List<CreditNote> creditNotesForBill(long billId) {
        return creditNoteRepository.findByBillIdAndStatusTrueOrderByCreditNoteIdDesc(billId);
    }

    // --------------------------------------------------------------- estimation (M04 gap)

    /**
     * Bill estimation (M04 gap, minimal slice).
     * Stateless: does NOT persist charges/bill — calculates totals for proposed
     * service items using the same BillingCalculator as finalize (BR-BILL-004/005).
     * Ported from PatientBillEstimationNewDto / GetPatientBillEstimationDetailsResultDto:
     * sums line totals, taxes, concession, self/nonSelf. Caller supplies patientId
     * + isSelfCompany discriminator + proposed items.
     */
    public EstimateResult estimate(Long patientId, Boolean isSelfCompany, List<EstimateItem> items) {
        if (patientId == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "patientId is required for estimation");
        }
        if (items == null || items.isEmpty()) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "At least one item is required for estimation");
        }
        boolean selfCompany = isSelfCompany == null || isSelfCompany;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        BigDecimal concessionTotal = BigDecimal.ZERO;
        BigDecimal selfTotal = BigDecimal.ZERO;
        BigDecimal nonSelfTotal = BigDecimal.ZERO;
        List<EstimateLine> lines = new ArrayList<>();
        for (EstimateItem it : items) {
            if (it.rate() == null || it.rate().signum() < 0) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID", "rate must be >= 0 for estimation");
            }
            int qty = it.qty() == null ? 1 : it.qty();
            if (qty <= 0) {
                throw new InvalidBillingRequestException("BILL_REQ_INVALID", "qty must be > 0 for estimation");
            }
            ChargeCalculation calc = BillingCalculator.calculate(
                    it.rate(), qty, it.taxPctA(), it.taxPctB(), it.taxPctC(),
                    Boolean.TRUE.equals(it.applyAfterConcession()),
                    it.coPayPct(), it.concessionPlanPct(), it.concessionBillPct(),
                    it.doctorSharePct(), Boolean.TRUE.equals(it.isOtProcedure()),
                    selfCompany);
            total = total.add(calc.lineTotal());
            taxTotal = taxTotal.add(calc.taxTotal());
            concessionTotal = concessionTotal.add(calc.concession());
            selfTotal = selfTotal.add(calc.selfAmount());
            nonSelfTotal = nonSelfTotal.add(calc.nonSelfAmount());
            lines.add(new EstimateLine(it.serviceId(), it.serviceName(), it.rate(), qty,
                    calc.lineTotal(), calc.selfAmount(), calc.nonSelfAmount(), calc.taxTotal(), calc.concession()));
        }
        return new EstimateResult(patientId, selfCompany, total, taxTotal, concessionTotal, selfTotal, nonSelfTotal, lines);
    }

    // --------------------------------------------------------------- sponsor change stub (M04 gap)

    /**
     * Sponsor change mid-bill stub (M04 gap).
     * Ported from UpdatePatientSponsorNewDto: updates bill sponsorCompanyId/planId
     * and flips isSelfCompany (NULL/0 = self-pay). Allowed on interim or finalized
     * non-cancelled bills; emits SponsorChanged via outbox for audit.
     */
    @Transactional
    public Bill changeSponsor(long billId, Long companyId, Long planId) {
        Bill bill = load(billId);
        if (Boolean.TRUE.equals(bill.getIsCancelled())) {
            throw new InvalidBillingRequestException("BILL_ILLEGAL_TRANSITION",
                    "Cannot change sponsor on cancelled bill " + billId);
        }
        bill.setSponsorCompanyId(companyId);
        bill.setSponsorPlanId(planId);
        // sponsorship implies non-self when companyId present; clearing restores self
        bill.setIsSelfCompany(companyId == null || companyId == 0);
        bill.setUpdatedBy(AUDIT_ACTOR_ID);
        bill.setUpdatedDatetime(now());
        bill = billRepository.save(bill);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("billId", String.valueOf(billId));
        payload.put("patientId", String.valueOf(bill.getPatientId()));
        payload.put("sponsorCompanyId", companyId == null ? null : String.valueOf(companyId));
        payload.put("sponsorPlanId", planId == null ? null : String.valueOf(planId));
        payload.put("changedAt", now().toString());
        outboxService.append(OutboxPublisher.BillingEvents.SPONSOR_CHANGED, "bill",
                billId, String.valueOf(billId), toJson(payload), "sponsor-changed:" + billId);
        log.info("Sponsor changed for bill {} -> companyId={} planId={}", billId, companyId, planId);
        return get(billId);
    }

    // --------------------------------------------------------------- package subscription (M04 gap)

    /**
     * Package subscription (M04 gap, minimal slice ported from T_PATIENTPACKAGESUBSCRIPTION).
     * Creates ihms_billing.t_package_subscription row with subscription_no PS-<seq>
     * and emits PackageSubscribed via outbox. Optional billId ties consumption to bill.
     */
    @Transactional
    public PackageSubscription subscribePackage(Long patientId, Long packageId, Long billId,
                                               BigDecimal coveredAmount, Timestamp fromDate, Timestamp toDate) {
        if (patientId == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "patientId is required");
        }
        if (packageId == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "packageId is required");
        }
        if (billId != null) {
            load(billId); // validate bill exists
        }
        PackageSubscription ps = new PackageSubscription();
        ps.setSubscriptionId(nextId("t_package_subscription_seq"));
        ps.setPatientId(patientId);
        ps.setPackageId(packageId);
        ps.setBillId(billId);
        ps.setSubscriptionNo("PS-" + nextVal("t_package_subscription_no_seq"));
        ps.setCoveredAmount(coveredAmount == null ? BigDecimal.ZERO : coveredAmount);
        ps.setFromDate(fromDate);
        ps.setToDate(toDate);
        ps.setIsActive(true);
        ps.setStatus(true);
        ps.setSynchronizedd(false);
        ps.setAddedBy(AUDIT_ACTOR_ID);
        ps.setAddedDatetime(now());
        ps = packageSubscriptionRepository.save(ps);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("subscriptionId", String.valueOf(ps.getSubscriptionId()));
        payload.put("subscriptionNo", ps.getSubscriptionNo());
        payload.put("patientId", String.valueOf(patientId));
        payload.put("packageId", String.valueOf(packageId));
        payload.put("billId", billId == null ? null : String.valueOf(billId));
        payload.put("coveredAmount", ps.getCoveredAmount());
        outboxService.append(OutboxPublisher.BillingEvents.PACKAGE_SUBSCRIBED, "package_subscription",
                ps.getSubscriptionId(), String.valueOf(patientId), toJson(payload),
                "package-subscribed:" + ps.getSubscriptionId());
        log.info("Package subscription {} for patient {} package {} bill {}",
                ps.getSubscriptionNo(), patientId, packageId, billId);
        return ps;
    }

    @Transactional(readOnly = true)
    public List<PackageSubscription> packageSubscriptionsForPatient(Long patientId) {
        if (patientId == null) {
            throw new InvalidBillingRequestException("BILL_REQ_INVALID", "patientId is required");
        }
        return packageSubscriptionRepository.findByPatientIdOrderBySubscriptionIdDesc(patientId);
    }

    @Transactional(readOnly = true)
    public List<PackageSubscription> packageSubscriptionsForBill(Long billId) {
        return packageSubscriptionRepository.findByBillIdOrderBySubscriptionIdAsc(billId);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public Bill get(long billId) {
        Bill bill = load(billId);
        bill.setCharges(chargeRepository.findByBillIdOrderByChargeIdAsc(billId));
        return bill;
    }

    // ------------------------------------------------------------------ helpers

    private Bill load(long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new BillingEntityNotFoundException("NOT_FOUND",
                        "Bill not found: " + billId));
    }

    private static BigDecimal nzOrZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        return nextVal(sequenceName);
    }

    private long nextVal(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_billing." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested charge at POST /api/v1/charges. */
    public record NewCharge(Long serviceId, String serviceName, BigDecimal rate, Integer qty,
                            BigDecimal taxPctA, BigDecimal taxPctB, BigDecimal taxPctC,
                            Boolean applyAfterConcession, BigDecimal coPayPct,
                            BigDecimal concessionPlanPct, BigDecimal concessionBillPct,
                            BigDecimal doctorSharePct, Short specialization,
                            Boolean isOtProcedure) {}

    /** Estimation input item (same money fields as NewCharge minus service validation). */
    public record EstimateItem(Long serviceId, String serviceName, BigDecimal rate, Integer qty,
                               BigDecimal taxPctA, BigDecimal taxPctB, BigDecimal taxPctC,
                               Boolean applyAfterConcession, BigDecimal coPayPct,
                               BigDecimal concessionPlanPct, BigDecimal concessionBillPct,
                               BigDecimal doctorSharePct, Short specialization,
                               Boolean isOtProcedure) {}

    public record EstimateLine(Long serviceId, String serviceName, BigDecimal rate, Integer qty,
                               BigDecimal lineTotal, BigDecimal selfAmount, BigDecimal nonSelfAmount,
                               BigDecimal taxTotal, BigDecimal concession) {}

    public record EstimateResult(Long patientId, Boolean isSelfCompany, BigDecimal totalAmount,
                                 BigDecimal taxAmount, BigDecimal concessionAmount,
                                 BigDecimal selfAmount, BigDecimal nonSelfAmount,
                                 List<EstimateLine> lines) {}
}
