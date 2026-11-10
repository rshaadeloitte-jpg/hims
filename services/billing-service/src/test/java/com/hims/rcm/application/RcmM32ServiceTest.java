package com.hims.rcm.application;

import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmRuleException;
import com.hims.rcm.domain.model.ChiranjeeviCase;
import com.hims.rcm.domain.model.DischargeVerification;
import com.hims.rcm.domain.model.DmApproval;
import com.hims.rcm.domain.repository.ChiranjeeviCaseRepository;
import com.hims.rcm.domain.repository.DischargeVerificationRepository;
import com.hims.rcm.domain.repository.DmApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RcmM32ServiceTest {

    @Mock private DmApprovalRepository dmRepository;
    @Mock private DischargeVerificationRepository dvRepository;
    @Mock private ChiranjeeviCaseRepository cjRepository;
    private RcmM32Service service;

    @BeforeEach void setUp() {
        service = new RcmM32Service(dmRepository, dvRepository, cjRepository);
    }

    @Test void dm_approve_ok() {
        when(dmRepository.nextId()).thenReturn(50L);
        when(dmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DmApproval a = service.requestDmApproval(1L, null, "RGHS", new BigDecimal("1000"));
        assertThat(a.getApprovalNo()).isEqualTo("DM-50");
        assertThat(a.getDocStatus()).isEqualTo(DmApproval.STATUS_PENDING);
    }

    @Test void dm_decide_pending_ok() {
        DmApproval a = new DmApproval(); a.setId(1L); a.setDocStatus(DmApproval.STATUS_PENDING);
        when(dmRepository.findById(1L)).thenReturn(Optional.of(a));
        when(dmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DmApproval out = service.decide(1L, true, new BigDecimal("800"), 99L, "ok");
        assertThat(out.getDocStatus()).isEqualTo(DmApproval.STATUS_APPROVED);
        assertThat(out.getApprovedAmount()).isEqualByComparingTo("800");
    }

    @Test void dm_decide_rejected_noApprovedAmount() {
        DmApproval a = new DmApproval(); a.setId(1L); a.setDocStatus(DmApproval.STATUS_PENDING);
        when(dmRepository.findById(1L)).thenReturn(Optional.of(a));
        when(dmRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.decide(1L, false, null, null, "no").getDocStatus())
                .isEqualTo(DmApproval.STATUS_REJECTED);
    }

    @Test void dm_approve_missingAmount_throws() {
        DmApproval a = new DmApproval(); a.setId(1L); a.setDocStatus(DmApproval.STATUS_PENDING);
        when(dmRepository.findById(1L)).thenReturn(Optional.of(a));
        assertThatThrownBy(() -> service.decide(1L, true, null, null, null))
                .isInstanceOf(InvalidRcmRequestException.class);
    }

    @Test void discharge_fullFlow_verified() {
        Map<Long, DischargeVerification> dvStore = new HashMap<>();
        when(dvRepository.nextId()).thenReturn(70L);
        when(dvRepository.save(any())).thenAnswer(inv -> { DischargeVerification d = inv.getArgument(0); dvStore.put(d.getId(), d); return d; });
        when(dvRepository.findById(any(Long.class))).thenAnswer(inv -> Optional.ofNullable(dvStore.get(inv.getArgument(0))));
        DischargeVerification v = service.startVerification(100L, 1L);
        assertThat(v.getDocStatus()).isEqualTo(DischargeVerification.STATUS_PENDING);
        service.setBillSettled(v.getId(), true);
        service.setClearanceDone(v.getId(), true);
        DischargeVerification verified = service.complete(v.getId(), true, 7L, "ok");
        assertThat(verified.getDocStatus()).isEqualTo(DischargeVerification.STATUS_VERIFIED);
        assertThat(verified.getVerifiedAt()).isNotNull();
    }

    @Test void discharge_verifyBeforeClearance_throws() {
        Map<Long, DischargeVerification> dvStore2 = new HashMap<>();
        when(dvRepository.nextId()).thenReturn(71L);
        when(dvRepository.save(any())).thenAnswer(inv -> { DischargeVerification d = inv.getArgument(0); dvStore2.put(d.getId(), d); return d; });
        when(dvRepository.findById(any(Long.class))).thenAnswer(inv -> Optional.ofNullable(dvStore2.get(inv.getArgument(0))));
        DischargeVerification v = service.startVerification(100L, 1L);
        service.setBillSettled(v.getId(), true);
        assertThatThrownBy(() -> service.complete(v.getId(), true, null, null))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test void chiranjeevi_lifecycle() {
        when(cjRepository.nextId()).thenReturn(700L);
        when(cjRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        assertThat(c.getCaseNo()).isEqualTo("CJ-700");
        when(cjRepository.findById(700L)).thenReturn(Optional.of(c));
        assertThat(service.decideCase(700L, true, "ok").getDocStatus())
                .isEqualTo(ChiranjeeviCase.STATUS_APPROVED);
        assertThat(service.close(700L, "done").getDocStatus())
                .isEqualTo(ChiranjeeviCase.STATUS_CLOSED);
    }

    @Test void chiranjeevi_close_unapproved_throws() {
        ChiranjeeviCase c = new ChiranjeeviCase(); c.setId(7L); c.setDocStatus(ChiranjeeviCase.STATUS_NEW);
        when(cjRepository.findById(7L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> service.close(7L, null)).isInstanceOf(RcmRuleException.class);
    }
}
