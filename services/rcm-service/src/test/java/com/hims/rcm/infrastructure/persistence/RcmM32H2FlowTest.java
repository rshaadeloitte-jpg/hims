package com.hims.rcm.infrastructure.persistence;

import com.hims.rcm.application.RcmM32Service;
import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmRuleException;
import com.hims.rcm.domain.model.ChiranjeeviCase;
import com.hims.rcm.domain.model.DischargeVerification;
import com.hims.rcm.domain.model.DmApproval;
import com.hims.rcm.domain.repository.ChiranjeeviCaseRepository;
import com.hims.rcm.domain.repository.DischargeVerificationRepository;
import com.hims.rcm.domain.repository.DmApprovalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * H2 flow tests for M32 RCM sub-features: DM approval, discharge verification, Chiranjeevi cases.
 * Uses in-memory H2 with Flyway migrations applied (V1 + V2).
 */
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {RcmM32Service.class}
        ),
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {com.hims.rcm.application.NhcxService.class,
                           com.hims.rcm.application.TidService.class}
        )
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.locations=classpath:db/migration",
        "spring.flyway.default-schema=ihms_rcm",
        "spring.flyway.create-schemas=true",
        "spring.jpa.properties.hibernate.default_schema=ihms_rcm"
})
class RcmM32H2FlowTest {

    @Autowired
    private RcmM32Service service;

    @Autowired
    private DmApprovalRepository dmRepository;

    @Autowired
    private DischargeVerificationRepository dvRepository;

    @Autowired
    private ChiranjeeviCaseRepository cjRepository;

    // ============================================================ DM approval flow

    @Test
    void dmApproval_create_persistsPending() {
        DmApproval a = service.requestDmApproval(1L, null, "RGHS", new BigDecimal("1000"));
        assertThat(a.getId()).isNotNull();
        assertThat(a.getApprovalNo()).startsWith("DM-");
        assertThat(a.getDocStatus()).isEqualTo(DmApproval.STATUS_PENDING);
        assertThat(a.getRequestedAmount()).isEqualByComparingTo("1000");

        DmApproval reloaded = dmRepository.findById(a.getId()).orElseThrow();
        assertThat(reloaded.getApprovalNo()).isEqualTo(a.getApprovalNo());
        assertThat(reloaded.getDocStatus()).isEqualTo(DmApproval.STATUS_PENDING);
    }

    @Test
    void dmApproval_approve_flow() {
        DmApproval a = service.requestDmApproval(1L, 100L, "RGHS", new BigDecimal("1000"));
        DmApproval approved = service.decide(a.getId(), true, new BigDecimal("800"), 99L, "ok");
        assertThat(approved.getDocStatus()).isEqualTo(DmApproval.STATUS_APPROVED);
        assertThat(approved.getApprovedAmount()).isEqualByComparingTo("800");
        assertThat(approved.getDmUserId()).isEqualTo(99L);
        assertThat(approved.getDecidedAt()).isNotNull();
    }

    @Test
    void dmApproval_reject_flow() {
        DmApproval a = service.requestDmApproval(1L, 100L, "RGHS", new BigDecimal("1000"));
        DmApproval rejected = service.decide(a.getId(), false, null, 99L, "not approved");
        assertThat(rejected.getDocStatus()).isEqualTo(DmApproval.STATUS_REJECTED);
        assertThat(rejected.getApprovedAmount()).isNull();
        assertThat(rejected.getDmUserId()).isEqualTo(99L);
        assertThat(rejected.getDecidedAt()).isNotNull();
    }

    @Test
    void dmApproval_decide_nonPending_throws() {
        DmApproval a = service.requestDmApproval(1L, 100L, "RGHS", new BigDecimal("1000"));
        service.decide(a.getId(), true, new BigDecimal("800"), 99L, "ok");
        assertThatThrownBy(() -> service.decide(a.getId(), false, null, 99L, "again"))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void dmApproval_approve_missingAmount_throws() {
        DmApproval a = service.requestDmApproval(1L, 100L, "RGHS", new BigDecimal("1000"));
        assertThatThrownBy(() -> service.decide(a.getId(), true, null, null, null))
                .isInstanceOf(InvalidRcmRequestException.class);
    }

    @Test
    void dmApproval_list_byStatus() {
        DmApproval a1 = service.requestDmApproval(1L, null, "RGHS", new BigDecimal("1000"));
        DmApproval a2 = service.requestDmApproval(2L, null, "RGHS", new BigDecimal("2000"));
        service.decide(a1.getId(), true, new BigDecimal("1000"), 99L, "ok");

        assertThat(service.listDm(DmApproval.STATUS_APPROVED)).hasSize(1);
        assertThat(service.listDm(DmApproval.STATUS_PENDING)).hasSize(1);
        assertThat(service.listDm(null)).hasSize(2);
    }

    // ============================================================ Discharge verification flow

    @Test
    void discharge_start_persistsPending() {
        DischargeVerification v = service.startVerification(100L, 1L);
        assertThat(v.getId()).isNotNull();
        assertThat(v.getDocStatus()).isEqualTo(DischargeVerification.STATUS_PENDING);
        assertThat(v.getBillSettled()).isFalse();
        assertThat(v.getClearanceDone()).isFalse();

        DischargeVerification reloaded = dvRepository.findById(v.getId()).orElseThrow();
        assertThat(reloaded.getAdmissionId()).isEqualTo(100L);
        assertThat(reloaded.getDocStatus()).isEqualTo(DischargeVerification.STATUS_PENDING);
    }

    @Test
    void discharge_fullFlow_verified() {
        DischargeVerification v = service.startVerification(100L, 1L);
        service.setBillSettled(v.getId(), true);
        service.setClearanceDone(v.getId(), true);
        DischargeVerification verified = service.complete(v.getId(), true, 7L, "ok");
        assertThat(verified.getDocStatus()).isEqualTo(DischargeVerification.STATUS_VERIFIED);
        assertThat(verified.getVerifiedAt()).isNotNull();
        assertThat(verified.getVerifierUserId()).isEqualTo(7L);
    }

    @Test
    void discharge_verifyBeforeBillSettled_throws() {
        DischargeVerification v = service.startVerification(100L, 1L);
        service.setClearanceDone(v.getId(), true);
        assertThatThrownBy(() -> service.complete(v.getId(), true, null, null))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void discharge_verifyBeforeClearanceDone_throws() {
        DischargeVerification v = service.startVerification(100L, 1L);
        service.setBillSettled(v.getId(), true);
        assertThatThrownBy(() -> service.complete(v.getId(), true, null, null))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void discharge_reject_flow() {
        DischargeVerification v = service.startVerification(100L, 1L);
        DischargeVerification rejected = service.complete(v.getId(), false, 7L, "rejected");
        assertThat(rejected.getDocStatus()).isEqualTo(DischargeVerification.STATUS_REJECTED);
        assertThat(rejected.getVerifiedAt()).isNotNull();
    }

    @Test
    void discharge_complete_nonPending_throws() {
        DischargeVerification v = service.startVerification(100L, 1L);
        service.setBillSettled(v.getId(), true);
        service.setClearanceDone(v.getId(), true);
        service.complete(v.getId(), true, 7L, "ok");
        assertThatThrownBy(() -> service.complete(v.getId(), true, 7L, "again"))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void discharge_list_byAdmissionId() {
        DischargeVerification v1 = service.startVerification(100L, 1L);
        DischargeVerification v2 = service.startVerification(200L, 2L);
        assertThat(service.listDv(100L, null)).hasSize(1);
        assertThat(service.listDv(null, DischargeVerification.STATUS_PENDING)).hasSize(2);
    }

    // ============================================================ Chiranjeevi case flow

    @Test
    void chiranjeevi_open_persistsNew() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        assertThat(c.getId()).isNotNull();
        assertThat(c.getCaseNo()).startsWith("CJ-");
        assertThat(c.getDocStatus()).isEqualTo(ChiranjeeviCase.STATUS_NEW);
        assertThat(c.getAmount()).isEqualByComparingTo("5000");

        ChiranjeeviCase reloaded = cjRepository.findById(c.getId()).orElseThrow();
        assertThat(reloaded.getCaseNo()).isEqualTo(c.getCaseNo());
        assertThat(reloaded.getDocStatus()).isEqualTo(ChiranjeeviCase.STATUS_NEW);
    }

    @Test
    void chiranjeevi_approve_flow() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        ChiranjeeviCase approved = service.decideCase(c.getId(), true, "approved");
        assertThat(approved.getDocStatus()).isEqualTo(ChiranjeeviCase.STATUS_APPROVED);
        assertThat(approved.getDecidedAt()).isNotNull();
    }

    @Test
    void chiranjeevi_reject_flow() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        ChiranjeeviCase rejected = service.decideCase(c.getId(), false, "rejected");
        assertThat(rejected.getDocStatus()).isEqualTo(ChiranjeeviCase.STATUS_REJECTED);
        assertThat(rejected.getDecidedAt()).isNotNull();
    }

    @Test
    void chiranjeevi_close_flow() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        service.decideCase(c.getId(), true, "approved");
        ChiranjeeviCase closed = service.close(c.getId(), "closed");
        assertThat(closed.getDocStatus()).isEqualTo(ChiranjeeviCase.STATUS_CLOSED);
        assertThat(closed.getClosedAt()).isNotNull();
    }

    @Test
    void chiranjeevi_close_unapproved_throws() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        assertThatThrownBy(() -> service.close(c.getId(), null))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void chiranjeevi_decide_nonNew_throws() {
        ChiranjeeviCase c = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        service.decideCase(c.getId(), true, "approved");
        assertThatThrownBy(() -> service.decideCase(c.getId(), false, "again"))
                .isInstanceOf(RcmRuleException.class);
    }

    @Test
    void chiranjeevi_list_byStatus() {
        ChiranjeeviCase c1 = service.openCase(1L, "CJ-100", "PKG-A", new BigDecimal("5000"));
        ChiranjeeviCase c2 = service.openCase(2L, "CJ-200", "PKG-B", new BigDecimal("6000"));
        service.decideCase(c1.getId(), true, "approved");

        assertThat(service.listCj(ChiranjeeviCase.STATUS_APPROVED)).hasSize(1);
        assertThat(service.listCj(ChiranjeeviCase.STATUS_NEW)).hasSize(1);
        assertThat(service.listCj(null)).hasSize(2);
    }
}
