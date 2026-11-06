package com.hims.rmrs.application;

import com.hims.rmrs.application.exceptions.DuplicateRmrsResourceException;
import com.hims.rmrs.application.exceptions.InvalidRmrsRequestException;
import com.hims.rmrs.application.exceptions.RmrsNotFoundException;
import com.hims.rmrs.domain.model.BankStatement;
import com.hims.rmrs.domain.model.JournalVoucher;
import com.hims.rmrs.domain.model.JournalVoucherLeg;
import com.hims.rmrs.domain.model.LedgerMaster;
import com.hims.rmrs.domain.model.PoRequest;
import com.hims.rmrs.domain.repository.BankStatementRepository;
import com.hims.rmrs.domain.repository.HeadMasterRepository;
import com.hims.rmrs.domain.repository.JournalVoucherLegRepository;
import com.hims.rmrs.domain.repository.JournalVoucherRepository;
import com.hims.rmrs.domain.repository.LedgerMasterRepository;
import com.hims.rmrs.domain.repository.PoRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RmrsM23ServiceTest {

    @Mock private JournalVoucherRepository voucherRepository;
    @Mock private JournalVoucherLegRepository legRepository;
    @Mock private LedgerMasterRepository ledgerRepository;
    @Mock private HeadMasterRepository headRepository;
    @Mock private BankStatementRepository bankStatementRepository;
    @Mock private PoRequestRepository poRequestRepository;
    private RmrsM23Service service;

    @BeforeEach void setUp() {
        service = new RmrsM23Service(voucherRepository, legRepository, ledgerRepository,
                headRepository, bankStatementRepository, poRequestRepository);
    }

    @Test void createVoucher_balanced_ok() {
        when(ledgerRepository.existsById(1L)).thenReturn(true);
        when(ledgerRepository.existsById(2L)).thenReturn(true);
        when(voucherRepository.nextId()).thenReturn(100L);
        when(legRepository.nextId()).thenReturn(1000L, 1001L);
        when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(legRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        JournalVoucher v = service.createVoucher("opening", "2026-01-01", List.of(
                new RmrsM23Service.Leg(1L, new BigDecimal("100"), null),
                new RmrsM23Service.Leg(2L, null, new BigDecimal("100"))));
        assertThat(v.getVoucherNo()).isEqualTo("JV-100");
        assertThat(v.getDocStatus()).isEqualTo(JournalVoucher.STATUS_DRAFT);
    }

    @Test void createVoucher_unbalanced_throws() {
        when(ledgerRepository.existsById(1L)).thenReturn(true);
        when(ledgerRepository.existsById(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.createVoucher("n", "2026-01-01", List.of(
                new RmrsM23Service.Leg(1L, new BigDecimal("100"), null),
                new RmrsM23Service.Leg(2L, null, new BigDecimal("99")))))
                .isInstanceOf(DuplicateRmrsResourceException.class);
    }

    @Test void createVoucher_tooFewLegs_throws() {
        assertThatThrownBy(() -> service.createVoucher("n", "2026-01-01",
                List.of(new RmrsM23Service.Leg(1L, new BigDecimal("10"), null))))
                .isInstanceOf(InvalidRmrsRequestException.class);
    }

    @Test void postVoucher_onlyFromDraft() {
        JournalVoucher v = new JournalVoucher(); v.setId(1L); v.setDocStatus(JournalVoucher.STATUS_DRAFT);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(v));
        when(voucherRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.postVoucher(1L, null).getDocStatus()).isEqualTo(JournalVoucher.STATUS_POSTED);
    }

    @Test void markSuspense_roundTrip() {
        LedgerMaster l = new LedgerMaster(); l.setLedgerId(7L); l.setSuspense(false);
        when(ledgerRepository.findById(7L)).thenReturn(Optional.of(l));
        when(ledgerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        LedgerMaster after = service.markSuspense(7L, "pending clearance");
        assertThat(after.getSuspense()).isTrue();
        assertThat(after.getSuspenseReason()).isEqualTo("pending clearance");
        LedgerMaster cleared = service.clearSuspense(7L);
        assertThat(cleared.getSuspense()).isFalse();
        assertThat(cleared.getSuspenseReason()).isNull();
    }

    @Test void importBankStatement_validatesDirection() {
        when(ledgerRepository.existsById(1L)).thenReturn(true);
        when(bankStatementRepository.nextId()).thenReturn(50L);
        when(bankStatementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThatThrownBy(() -> service.importBankStatement(1L, "2026-01-01",
                new BigDecimal("10"), "BOGUS", null, null))
                .isInstanceOf(InvalidRmrsRequestException.class);
    }

    @Test void reconcile_requiresMatch() {
        BankStatement s = new BankStatement(); s.setId(1L); s.setStatementLineId(1L);
        s.setDocStatus(BankStatement.STATUS_IMPORTED);
        when(bankStatementRepository.findById(1L)).thenReturn(Optional.of(s));
        assertThatThrownBy(() -> service.reconcile(1L, null, null))
                .isInstanceOf(InvalidRmrsRequestException.class);
    }

    @Test void reconcile_setsMatchedAndStatus() {
        BankStatement s = new BankStatement(); s.setId(1L); s.setStatementLineId(1L);
        s.setDocStatus(BankStatement.STATUS_IMPORTED);
        when(bankStatementRepository.findById(1L)).thenReturn(Optional.of(s));
        when(bankStatementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BankStatement out = service.reconcile(1L, 99L, null);
        assertThat(out.getDocStatus()).isEqualTo(BankStatement.STATUS_RECONCILED);
        assertThat(out.getMatchedEntryId()).isEqualTo(99L);
    }

    @Test void poRequest_fullFlow() {
        when(headRepository.existsById(1L)).thenReturn(true);
        when(poRequestRepository.nextId()).thenReturn(200L);
        when(poRequestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PoRequest p = service.createPoRequest(null, 1L, null, new BigDecimal("500"), "");
        assertThat(p.getRequestNo()).isEqualTo("POQ-200");
        when(poRequestRepository.findById(200L)).thenReturn(Optional.of(p));
        assertThat(service.approvePoRequest(200L, null).getDocStatus())
                .isEqualTo(PoRequest.STATUS_APPROVED);
        assertThat(service.markIssued(200L, 12345L).getDocStatus())
                .isEqualTo(PoRequest.STATUS_ISSUED);
    }

    @Test void ledgerVoucherLeg_findByVoucher() {
        JournalVoucher v = new JournalVoucher(); v.setId(1L); v.setVoucherId(1L);
        when(voucherRepository.findById(1L)).thenReturn(Optional.of(v));
        when(legRepository.findByVoucherId(1L)).thenReturn(List.of(new JournalVoucherLeg()));
        assertThat(service.voucherLegs(1L)).hasSize(1);
    }

    @Test void markSuspense_unknownLedger_throws() {
        when(ledgerRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markSuspense(99L, "x"))
                .isInstanceOf(RmrsNotFoundException.class);
    }
}
