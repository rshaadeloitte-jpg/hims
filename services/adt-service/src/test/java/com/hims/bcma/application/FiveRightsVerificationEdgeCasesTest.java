package com.hims.bcma.application;

import com.hims.bcma.domain.model.BcmaVerification;
import com.hims.bcma.domain.model.VerificationLog;
import com.hims.bcma.domain.repository.BcmaVerificationRepository;
import com.hims.bcma.domain.repository.VerificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FiveRightsVerificationEdgeCasesTest {

    private FiveRightsVerificationService service;
    private BcmaVerificationRepository bcmaRepo;
    private VerificationLogRepository logRepo;

    @BeforeEach
    void setUp() {
        bcmaRepo = mock(BcmaVerificationRepository.class);
        logRepo = mock(VerificationLogRepository.class);
        service = new FiveRightsVerificationService(bcmaRepo, logRepo);
        when(bcmaRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(logRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- All 5 rights pass ---
    @Test
    void finalizeAllFiveRightsPass_setsAllRightsPassedTrue() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(1L);
        v.setPatientId(100L);
        v.setDrugNdc("NDC-001");
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(1L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(1L);

        assertThat(result.getAllRightsPassed()).isTrue();
        assertThat(result.getAdministered()).isTrue();
    }

    @Test
    void finalizeAllFiveRightsPass_setsAdministeredAt() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(2L);
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(2L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(2L);

        assertThat(result.getAdministeredAt()).isNotNull();
    }

    // --- Any single right fails ---
    @Test
    void finalizeRightPatientFails_setsAllRightsPassedFalse() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(3L);
        v.setRightPatient(false);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(3L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(3L);

        assertThat(result.getAllRightsPassed()).isFalse();
        assertThat(result.getAdministered()).isFalse();
    }

    @Test
    void finalizeRightDrugFails_setsAllRightsPassedFalse() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(4L);
        v.setRightPatient(true);
        v.setRightDrug(false);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(4L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(4L);

        assertThat(result.getAllRightsPassed()).isFalse();
    }

    @Test
    void finalizeRightDoseFails_setsAllRightsPassedFalse() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(5L);
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(false);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(5L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(5L);

        assertThat(result.getAllRightsPassed()).isFalse();
    }

    @Test
    void finalizeRightRouteFails_setsAllRightsPassedFalse() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(6L);
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(false);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(6L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(6L);

        assertThat(result.getAllRightsPassed()).isFalse();
    }

    @Test
    void finalizeRightTimeFails_setsAllRightsPassedFalse() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(7L);
        v.setRightPatient(true);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(false);
        when(bcmaRepo.findByBcmaId(7L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(7L);

        assertThat(result.getAllRightsPassed()).isFalse();
    }

    // --- Override scenarios ---
    @Test
    void updateOverrideFields_setsOverrideByAndReason() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(8L);
        v.setRightPatient(false);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(8L)).thenReturn(Optional.of(v));

        // Simulate override scenario
        v.setOverridden(true);
        v.setOverrideReason("Clinical justification");
        v.setOverrideBy(101L);
        BcmaVerification result = service.finalizeVerification(8L);

        // After override, allRightsPassed reflects the actual boolean values,
        // but the overridden flag is tracked separately
        assertThat(result.getOverridden()).isTrue();
        assertThat(result.getOverrideReason()).isEqualTo("Clinical justification");
        assertThat(result.getOverrideBy()).isEqualTo(101L);
    }

    @Test
    void finalizeWithOverrideStillChecksActualBooleans() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(9L);
        v.setRightPatient(false); // One right fails
        v.setOverridden(true); // But overridden
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(9L)).thenReturn(Optional.of(v));

        BcmaVerification result = service.finalizeVerification(9L);

        // allRightsPassed is based on actual values, not override
        assertThat(result.getAllRightsPassed()).isFalse();
    }

    // --- Barcode mismatch ---
    @Test
    void verifyRightPatientWithMismatchBarcode_stillSavesAndLogs() {
        BcmaVerification v = service.verifyRightPatient(10L, 200L, "EXPECTED-BARCODE", false);

        assertThat(v.getWristbandBarcode()).isEqualTo("EXPECTED-BARCODE");
        assertThat(v.getRightPatient()).isFalse();
        verify(logRepo).save(any(VerificationLog.class));
    }

    @Test
    void verifyRightPatientMatchLogsResultFalse() {
        BcmaVerification v = service.verifyRightPatient(11L, 201L, "WB-001", false);

        assertThat(v.getRightPatient()).isFalse();
        verify(logRepo).save(argThat(log ->
                log.getResult() != null && !log.getResult()));
    }

    // --- Expired medication ---
    @Test
    void verifyRightDrugWithExpiredMedication_stillVerifiesDrugNdc() {
        BcmaVerification v = service.verifyRightDrug(12L, "EXPIRED-NDC-999", true);

        assertThat(v.getDrugNdc()).isEqualTo("EXPIRED-NDC-999");
        assertThat(v.getRightDrug()).isTrue();
    }

    // --- Expired wristband ---
    @Test
    void verifyRightPatientWithExpiredWristband_stillRecordsBarcode() {
        BcmaVerification v = service.verifyRightPatient(13L, 202L, "EXPIRED-WBAND", true);

        assertThat(v.getWristbandBarcode()).isEqualTo("EXPIRED-WBAND");
    }

    @Test
    void verifyRightPatientWithMismatchedWristbandAndDrugLogsBothEvents() {
        service.verifyRightPatient(14L, 203L, "WB-MISMATCH", false);
        service.verifyRightDrug(14L, "NDC-MISMATCH", false);

        // Two logs should be saved
        verify(logRepo, times(2)).save(any(VerificationLog.class));
    }

    // --- Concurrent verification ---
    @Test
    void concurrentVerification_multipleRightsSavedWithUpdatedState() {
        BcmaVerification existing = new BcmaVerification();
        existing.setBcmaId(15L);
        existing.setRightDrug(true);
        existing.setRightDose(true);
        existing.setRightRoute(true);
        existing.setRightTime(true);
        when(bcmaRepo.findByBcmaId(15L)).thenReturn(Optional.of(existing));

        BcmaVerification result = service.verifyRightPatient(15L, 204L, "WB-CONCURRENT", true);

        // The existing verification should be updated with the new right patient value
        assertThat(result.getRightPatient()).isTrue();
        // Previous rights should be preserved
        assertThat(result.getRightDrug()).isTrue();
        assertThat(result.getRightDose()).isTrue();
        assertThat(result.getRightRoute()).isTrue();
        assertThat(result.getRightTime()).isTrue();
    }

    @Test
    void verifyRightDose_whenNoExistingRecord_createsNewRecord() {
        when(bcmaRepo.findByBcmaId(999L)).thenReturn(Optional.empty());

        BcmaVerification v = service.verifyRightDose(999L, true);

        assertThat(v.getBcmaId()).isEqualTo(999L);
        assertThat(v.getRightDose()).isTrue();
    }

    @Test
    void verifyRightRoute_whenNoExistingRecord_createsNewRecord() {
        BcmaVerification v = service.verifyRightRoute(998L, true);

        assertThat(v.getBcmaId()).isEqualTo(998L);
        assertThat(v.getRightRoute()).isTrue();
    }

    @Test
    void verifyRightTime_whenNoExistingRecord_createsNewRecord() {
        BcmaVerification v = service.verifyRightTime(997L, true);

        assertThat(v.getBcmaId()).isEqualTo(997L);
        assertThat(v.getRightTime()).isTrue();
    }

    // --- Null match defaults to true ---
    @Test
    void verifyRightPatientWithNullMatch_defaultsToTrue() {
        BcmaVerification v = service.verifyRightPatient(16L, 205L, "WB-002", null);

        assertThat(v.getRightPatient()).isTrue();
    }

    @Test
    void verifyRightDrugWithNullMatch_defaultsToTrue() {
        BcmaVerification v = service.verifyRightDrug(17L, "NDC-002", null);

        assertThat(v.getRightDrug()).isTrue();
    }

    @Test
    void verifyRightDoseWithNullMatch_defaultsToTrue() {
        BcmaVerification v = service.verifyRightDose(18L, null);

        assertThat(v.getRightDose()).isTrue();
    }

    @Test
    void verifyRightRouteWithNullMatch_defaultsToTrue() {
        BcmaVerification v = service.verifyRightRoute(19L, null);

        assertThat(v.getRightRoute()).isTrue();
    }

    @Test
    void verifyRightTimeWithNullMatch_defaultsToTrue() {
        BcmaVerification v = service.verifyRightTime(20L, null);

        assertThat(v.getRightTime()).isTrue();
    }

    // --- Verification logging ---
    @Test
    void verifyRightPatientLogsRightPatientEvent() {
        service.verifyRightPatient(21L, 210L, "WB-LOG", true);

        verify(logRepo).save(argThat(log ->
                "RIGHT_PATIENT".equals(log.getVerificationEvent())));
    }

    @Test
    void verifyRightDrugLogsRightDrugEvent() {
        service.verifyRightDrug(22L, "NDC-LOG", true);

        verify(logRepo).save(argThat(log ->
                "RIGHT_DRUG".equals(log.getVerificationEvent())));
    }

    @Test
    void verifyRightDoseLogsRightDoseEvent() {
        service.verifyRightDose(23L, true);

        verify(logRepo).save(argThat(log ->
                "RIGHT_DOSE".equals(log.getVerificationEvent())));
    }

    @Test
    void verifyRightRouteLogsRightRouteEvent() {
        service.verifyRightRoute(24L, true);

        verify(logRepo).save(argThat(log ->
                "RIGHT_ROUTE".equals(log.getVerificationEvent())));
    }

    @Test
    void verifyRightTimeLogsRightTimeEvent() {
        service.verifyRightTime(25L, true);

        verify(logRepo).save(argThat(log ->
                "RIGHT_TIME".equals(log.getVerificationEvent())));
    }

    // --- Finalize edge cases ---
    @Test
    void finalizeVerification_notFound_throwsIllegalArgumentException() {
        when(bcmaRepo.findByBcmaId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalizeVerification(404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not found");
    }

    @Test
    void finalizeVerification_withNullRightPatient_throwsNullPointerException() {
        BcmaVerification v = new BcmaVerification();
        v.setBcmaId(26L);
        v.setRightPatient(null);
        v.setRightDrug(true);
        v.setRightDose(true);
        v.setRightRoute(true);
        v.setRightTime(true);
        when(bcmaRepo.findByBcmaId(26L)).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.finalizeVerification(26L))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void findByPatient_returnsNonEmptyList() {
        BcmaVerification v1 = new BcmaVerification();
        v1.setBcmaId(27L);
        BcmaVerification v2 = new BcmaVerification();
        v2.setBcmaId(28L);
        when(bcmaRepo.findByPatientId(500L)).thenReturn(List.of(v1, v2));

        List<BcmaVerification> result = service.findByPatient(500L);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByPatient_returnsEmptyList_whenNoResults() {
        when(bcmaRepo.findByPatientId(999L)).thenReturn(List.of());

        List<BcmaVerification> result = service.findByPatient(999L);

        assertThat(result).isEmpty();
    }

    // --- Scan time set ---
    @Test
    void verifyRightPatient_setsScanTime() {
        Timestamp before = Timestamp.from(Instant.now());
        BcmaVerification v = service.verifyRightPatient(29L, 300L, "WB-003", true);
        Timestamp after = Timestamp.from(Instant.now());

        assertThat(v.getScanTime()).isBetween(before, after);
    }

    @Test
    void verifyRightDose_setsScanTime() {
        Timestamp before = Timestamp.from(Instant.now());
        BcmaVerification v = service.verifyRightDose(30L, true);
        Timestamp after = Timestamp.from(Instant.now());

        assertThat(v.getScanTime()).isBetween(before, after);
    }
}
