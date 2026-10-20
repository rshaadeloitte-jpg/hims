package com.hims.billing.application;

import com.hims.billing.application.BillingCalculator.ChargeCalculation;
import com.hims.billing.application.exceptions.InvalidBillingRequestException;
import com.hims.billing.domain.model.Bill;
import com.hims.billing.domain.model.Charge;
import com.hims.billing.domain.model.DoctorShare;
import com.hims.billing.domain.repository.BillRepository;
import com.hims.billing.domain.repository.ChargeRepository;
import com.hims.billing.domain.repository.CreditNoteRepository;
import com.hims.billing.domain.repository.DoctorShareRepository;
import com.hims.billing.domain.repository.PackageSubscriptionRepository;
import com.hims.billing.infrastructure.outbox.OutboxEvent;
import com.hims.billing.infrastructure.outbox.OutboxPublisher.BillingEvents;
import com.hims.billing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — service-rule pins (GAP-REGISTER WP-11):
 *   BR-BILL-001 placeholder '000000000000000' -> final OPDBILL-/IPDBILL-&lt;seq&gt;,
 *   BR-BILL-006 settlement settled &lt;=&gt; BOTH balances &lt;= 0, idempotent replay,
 *   E-07 InvoiceGenerated appended EXACTLY ONCE per bill,
 *   *BeforeFinalization snapshots (BR-BILL-008 restore contract).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingServiceTest {

    @Mock private BillRepository billRepository;
    @Mock private ChargeRepository chargeRepository;
    @Mock private DoctorShareRepository doctorShareRepository;
    @Mock private CreditNoteRepository creditNoteRepository;
    @Mock private PackageSubscriptionRepository packageSubscriptionRepository;
    @Mock private OutboxService outboxService;
    @Mock private EntityManager em;
    @Mock private Query query;

    private BillingService service;

    private final AtomicLong seq = new AtomicLong(100);
    private final List<String> issuedSequences = new java.util.ArrayList<>();

    @BeforeEach
    void wireServiceAndStubSequences() {
        // manual wiring: Mockito constructor injection would skip the @PersistenceContext field
        service = new BillingService(billRepository, chargeRepository, doctorShareRepository,
                creditNoteRepository, packageSubscriptionRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);
        // SELECT nextval('ihms_billing.<seq>') -> monotonically increasing ids
        when(em.createNativeQuery(anyString())).thenAnswer(inv -> {
            issuedSequences.add((String) inv.getArgument(0));
            return query;
        });
        when(query.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        lenient().when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(doctorShareRepository.save(any())).thenAnswer(inv -> {
            DoctorShare ds = inv.getArgument(0);
            if (ds.getShareId() == null) ds.setShareId(900L);
            return ds;
        });
    }

    // ------------------------------------------------------------ fixtures

    /** rate 100 x1, coPay 40, plan 10 + bill 5, doctor share 10% — SELF path money:
     *  selfBase 40, conPct 15 -> selfRate 34, nonSelf 60, share CEILING(10% x 100)=10.00. */
    private static Charge charge() {
        Charge c = new Charge();
        c.setChargeId(7L);
        c.setServiceId(900L);
        c.setServiceName("X-Ray Chest");
        c.setRate(new BigDecimal("100"));
        c.setQty(1);
        c.setCoPayPct(new BigDecimal("40"));
        c.setConcessionPlanPct(new BigDecimal("10"));
        c.setConcessionBillPct(new BigDecimal("5"));
        c.setDoctorSharePct(new BigDecimal("10"));
        return c;
    }

    private Bill bill(Long admissionId) {
        Bill b = new Bill();
        b.setBillId(3L);
        b.setPatientId(505L);
        b.setEncounterId(admissionId == null ? 7777L : null);
        b.setAdmissionId(admissionId);
        b.setPrescriberDoctorId(42L);
        b.setIsSelfCompany(true);
        b.setIsInterim(true);
        b.setAddedDatetime(new Timestamp(System.currentTimeMillis()));
        return b;
    }

    private void loadStubs(Bill bill, List<Charge> charges) {
        // single-bill-per-test: id is drawn from the mocked sequence, so match any id
        when(billRepository.findById(anyLong())).thenAnswer(inv -> Optional.of(bill));
        if (!charges.isEmpty()) {
            when(chargeRepository.findById(charges.get(0).getChargeId()))
                    .thenReturn(Optional.of(charges.get(0)));   // POST /bills attach lookup
        }
        when(chargeRepository.findByBillIdOrderByChargeIdAsc(bill.getBillId()))
                .thenReturn(charges);   // used by finalize AND trailing get()
        when(doctorShareRepository.findByBillIdOrderByShareIdAsc(bill.getBillId()))
                .thenAnswer(inv -> List.of());   // replaced below when shares are pinned
    }

    // ---------------------------------------------------------- BR-BILL-001 numbering

    @Test
    void brBill001_placeholderAtCreation_thenOpdbillFinalNumber_withoutAdmission() {
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));

        Bill created = service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        assertThat(created.getBillNo()).isEqualTo("000000000000000");      // placeholder per rule
        assertThat(created.getIsSettled()).isFalse();
        assertThat(created.getCashCounterId()).isEqualTo(1L);              // default cash counter

        // re-point the persisted bill for finalize (createBill returned a detached copy)
        b.setBillNo("000000000000000");
        Bill finalized = service.finalizeBill(3L);
        assertThat(finalized.getBillNo()).startsWith("OPDBILL-");          // OPD variant: no admissionId
        assertThat(finalized.getIsInterim()).isFalse();
        assertThat(finalized.getBillNo()).isNotEqualTo("000000000000000");
        assertThat(issuedSequences.stream().anyMatch(s -> s.contains("t_opdbill_seq"))).isTrue();
    }

    @Test
    void brBill001_ipdVariant_usesIpdbillSequence_whenAdmissionPresent() {
        Bill b = bill(8888L);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));
        service.createBill(505L, null, 8888L, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");

        Bill finalized = service.finalizeBill(3L);
        assertThat(finalized.getBillNo()).startsWith("IPDBILL-");          // IPD variant
        assertThat(issuedSequences.stream().anyMatch(s -> s.contains("t_ipdbill_seq"))).isTrue();
        assertThat(issuedSequences.stream().noneMatch(s -> s.contains("t_opdbill_seq"))).isTrue();
    }

    @Test
    void brBill001_finalizeTwice_rejected_illegalTransition() {
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));
        service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");
        service.finalizeBill(3L);

        assertThatThrownBy(() -> service.finalizeBill(3L))
                .isInstanceOf(InvalidBillingRequestException.class)
                .extracting(e -> ((InvalidBillingRequestException) e).getErrorCode())
                .isEqualTo("BILL_ILLEGAL_TRANSITION");
        // the second attempt must NOT have drawn another bill-number sequence value
        assertThat(issuedSequences.stream().filter(s -> s.contains("_bill_seq")).count()).isEqualTo(1);
    }

    // ---------------------------------------------------------- snapshots (BR-BILL-008 contract)

    @Test
    void finalize_snapshotsBeforeFinalizationColumns_onEveryCharge() {
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));
        service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");
        service.finalizeBill(3L);

        ArgumentCaptor<Charge> captor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, atLeastOnce()).save(captor.capture());
        Charge snapshotted = captor.getAllValues().stream()
                .filter(c -> c.getTotalBeforeFinalization() != null).findFirst().orElseThrow();
        assertThat(snapshotted.getTotalBeforeFinalization()).isEqualByComparingTo("100.00");
        assertThat(snapshotted.getSelfBeforeFinalization()).isEqualByComparingTo("34.00");
        assertThat(snapshotted.getNonselfBeforeFinalization()).isEqualByComparingTo("60.00");
    }

    // ---------------------------------------------------------- E-07 emitted ONCE

    @Test
    void finalize_emitsInvoiceGenerated_exactlyOnce_withExactContractFields() {
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));
        when(doctorShareRepository.findByBillIdOrderByShareIdAsc(3L)).thenReturn(List.of());
        service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");

        service.finalizeBill(3L);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> pkCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idemCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService, times(1)).append(typeCaptor.capture(), eq("bill"), eq(3L),
                pkCaptor.capture(), anyString(), idemCaptor.capture());
        assertThat(typeCaptor.getValue()).isEqualTo(BillingEvents.INVOICE_GENERATED);  // 'InvoiceGenerated'
        assertThat(pkCaptor.getValue()).isEqualTo("3");                                // partition key billId
        assertThat(idemCaptor.getValue()).isEqualTo("invoice-generated:3");

        // rejected second finalize emits nothing further
        assertThatThrownBy(() -> service.finalizeBill(3L))
                .isInstanceOf(InvalidBillingRequestException.class);
        verify(outboxService, times(1)).append(any(), anyString(), any(), anyString(), anyString(), anyString());

        // interim operations never emit: create-only paths above already verified count==1
        assertThat(typeCaptor.getValue()).isEqualTo("InvoiceGenerated");
    }

    @Test
    void finalize_persistsDoctorShareRow_isBilledFalse() {
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(charge())));
        service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");
        service.finalizeBill(3L);

        ArgumentCaptor<DoctorShare> captor = ArgumentCaptor.forClass(DoctorShare.class);
        verify(doctorShareRepository).save(captor.capture());
        DoctorShare ds = captor.getValue();
        assertThat(ds.getBillId()).isEqualTo(3L);
        assertThat(ds.getChargeId()).isEqualTo(7L);
        assertThat(ds.getDoctorId()).isEqualTo(42L);                 // prescriberDoctorId from request header
        assertThat(ds.getShareAmount()).isEqualByComparingTo("10.00");     // CEILING scale 2
        assertThat(ds.getHospitalAmount()).isEqualByComparingTo("90.00");
        assertThat(ds.getIsBilled()).isFalse();
    }

    @Test
    void finalize_skipsDoctorShare_whenOtProcedure_evenWithPctAboveZero() {
        Charge ot = charge();
        ot.setIsOtProcedure(true);
        Bill b = bill(null);
        loadStubs(b, new java.util.ArrayList<>(List.of(ot)));
        service.createBill(505L, 7777L, null, null, 42L, true, List.of(7L));
        b.setBillNo("000000000000000");
        service.finalizeBill(3L);

        verify(doctorShareRepository, never()).save(any());
        verify(outboxService, times(1)).append(eq("InvoiceGenerated"), eq("bill"), eq(3L),
                anyString(), anyString(), anyString());   // E-07 still fires, doctorShare=null in payload
    }

    // ---------------------------------------------------------- BR-BILL-006 settlement

    private Bill finalizedBill() {
        Bill b = bill(null);
        b.setIsInterim(false);
        b.setBillNo("OPDBILL-1");
        b.setSelfAmount(new BigDecimal("500.00"));
        b.setNonSelfAmount(new BigDecimal("400.00"));
        b.setBalanceSelf(new BigDecimal("500.00"));
        b.setBalanceNonSelf(new BigDecimal("400.00"));
        when(billRepository.findById(3L)).thenReturn(Optional.of(b));
        when(chargeRepository.findByBillIdOrderByChargeIdAsc(3L)).thenReturn(List.of(charge()));
        return b;
    }

    @Test
    void brBill006_partialPayment_notSettled_balancesRecomputed() {
        finalizedBill();
        Bill result = service.settle(3L, new BigDecimal("300"), new BigDecimal("200"));
        assertThat(result.getBalanceSelf()).isEqualByComparingTo("200.00");
        assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("200.00");
        assertThat(result.getIsSettled()).isFalse();       // one balance > 0 => NOT settled
    }

    @Test
    void brBill006_fullPayment_settled_bothBalancesAtZero() {
        finalizedBill();
        Bill result = service.settle(3L, new BigDecimal("500"), new BigDecimal("400"));
        assertThat(result.getBalanceSelf()).isEqualByComparingTo("0.00");
        assertThat(result.getBalanceNonSelf()).isEqualByComparingTo("0.00");
        assertThat(result.getIsSettled()).isTrue();        // BOTH <= 0 => settled
    }

    @Test
    void brBill006_settlementReplay_isIdempotent_sameStateOnRepeat() {
        Bill b = finalizedBill();
        service.settle(3L, new BigDecimal("500"), new BigDecimal("400"));
        // payments-service replays the SAME cumulative request (network retry): state identical
        Bill replay = service.settle(3L, new BigDecimal("500"), new BigDecimal("400"));
        assertThat(replay.getIsSettled()).isTrue();
        assertThat(replay.getBalanceSelf()).isEqualByComparingTo("0.00");
        assertThat(replay.getBalanceNonSelf()).isEqualByComparingTo("0.00");
        // overpayment stays settled too (balances <= 0, BR-BILL-006)
        b.setBalanceSelf(new BigDecimal("500.00"));
        b.setBalanceNonSelf(new BigDecimal("400.00"));
        Bill overpay = service.settle(3L, new BigDecimal("600"), new BigDecimal("450"));
        assertThat(overpay.getIsSettled()).isTrue();
        assertThat(overpay.getBalanceSelf()).isEqualByComparingTo("-100.00");
    }

    @Test
    void brBill006_negativePaidAmount_rejected() {
        finalizedBill();
        assertThatThrownBy(() -> service.settle(3L, new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(InvalidBillingRequestException.class)
                .hasMessageContaining("non-negative");
    }

    // ---------------------------------------------------------- concession pre-finalize only

    @Test
    void concession_afterFinalize_rejected400() {
        finalizedBill();   // isInterim=false
        assertThatThrownBy(() -> service.applyConcession(3L, BigDecimal.TEN, BigDecimal.ONE))
                .isInstanceOf(InvalidBillingRequestException.class)
                .extracting(e -> ((InvalidBillingRequestException) e).getErrorCode())
                .isEqualTo("BILL_ALREADY_FINALIZED");
    }
}
