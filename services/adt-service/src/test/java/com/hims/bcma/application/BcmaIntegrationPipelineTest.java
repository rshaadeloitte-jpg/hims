package com.hims.bcma.application;

import com.hims.bcma.domain.model.*;
import com.hims.bcma.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests verifying the full verification-to-administration pipeline.
 * These tests simulate the end-to-end flow from medication scanning through
 * five rights verification to IVP administration and exception handling.
 */
class BcmaIntegrationPipelineTest {

    private BcmaModuleService bcmaModuleService;
    private FiveRightsVerificationService fiveRightsService;
    private IvpAdministrationService ivpService;
    private ExceptionHandlingService exceptionService;
    private StockViewService stockService;

    private MedicationScanRepository scanRepo;
    private IVPEventRepository ivpEventRepo;
    private BCMAExceptionRepository bcmaExceptionRepo;
    private MedicationStockRepository stockRepo;
    private BcmaVerificationRepository bcmaVerificationRepo;
    private VerificationLogRepository verificationLogRepo;
    private IvpAdministrationRepository ivpAdminRepo;
    private IvpDrugConfigRepository ivpDrugConfigRepo;
    private BCMAExceptionRepository exceptionRepo;
    private OverrideReasonRepository overrideReasonRepo;
    private StockAlertRepository stockAlertRepo;

    @BeforeEach
    void setUp() {
        scanRepo = mock(MedicationScanRepository.class);
        ivpEventRepo = mock(IVPEventRepository.class);
        bcmaExceptionRepo = mock(BCMAExceptionRepository.class);
        stockRepo = mock(MedicationStockRepository.class);

        bcmaVerificationRepo = mock(BcmaVerificationRepository.class);
        verificationLogRepo = mock(VerificationLogRepository.class);

        ivpAdminRepo = mock(IvpAdministrationRepository.class);
        ivpDrugConfigRepo = mock(IvpDrugConfigRepository.class);

        exceptionRepo = mock(BCMAExceptionRepository.class);
        overrideReasonRepo = mock(OverrideReasonRepository.class);

        stockAlertRepo = mock(StockAlertRepository.class);

        bcmaModuleService = new BcmaModuleService(scanRepo, ivpEventRepo, bcmaExceptionRepo, stockRepo);
        fiveRightsService = new FiveRightsVerificationService(bcmaVerificationRepo, verificationLogRepo);
        ivpService = new IvpAdministrationService(ivpAdminRepo, ivpDrugConfigRepo);
        exceptionService = new ExceptionHandlingService(exceptionRepo, overrideReasonRepo);
        stockService = new StockViewService(stockRepo, stockAlertRepo);

        when(scanRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ivpEventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bcmaExceptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(bcmaVerificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(verificationLogRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ivpAdminRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ivpDrugConfigRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(exceptionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(overrideReasonRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(stockAlertRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Full happy path: scan -> verify 5 rights -> administer ---
    @Test
    void fullPipeline_scanToAdministration_success() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(100L, "DRUG-001", "Morphine", 50L, 5L);
        assertThat(scanResult.isMatch()).isTrue();
        assertThat(scanResult.patientId()).isEqualTo(100L);

        // Step 2: Verify all 5 rights
        BcmaVerification v = fiveRightsService.verifyRightPatient(1L, 100L, "WB-100", true);
        v = fiveRightsService.verifyRightDrug(1L, "NDC-MORPHINE", true);
        v = fiveRightsService.verifyRightDose(1L, true);
        v = fiveRightsService.verifyRightRoute(1L, true);
        v = fiveRightsService.verifyRightTime(1L, true);

        // Step 3: Finalize verification
        BcmaVerification existing = new BcmaVerification();
        existing.setBcmaId(1L);
        existing.setRightPatient(true);
        existing.setRightDrug(true);
        existing.setRightDose(true);
        existing.setRightRoute(true);
        existing.setRightTime(true);
        when(bcmaVerificationRepo.findByBcmaId(1L)).thenReturn(Optional.of(existing));
        BcmaVerification finalized = fiveRightsService.finalizeVerification(1L);
        assertThat(finalized.getAllRightsPassed()).isTrue();
        assertThat(finalized.getAdministered()).isTrue();

        // Step 4: Record IVP administration
        IvpAdministration ivp = ivpService.recordIVP(100L, "Morphine", BigDecimal.valueOf(5), "mg",
                BigDecimal.valueOf(2.0), false, null, 50L, 5L);
        assertThat(ivp.getAdministrationStatus()).isEqualTo("IN_PROGRESS");

        // Step 5: Complete IVP
        IvpAdministration ivpInProgress = new IvpAdministration();
        ivpInProgress.setIvpId(ivp.getIvpId());
        ivpInProgress.setAdministrationStatus("IN_PROGRESS");
        when(ivpAdminRepo.findByIvpId(ivp.getIvpId())).thenReturn(Optional.of(ivpInProgress));
        IvpAdministration completed = ivpService.completeIVP(ivp.getIvpId(), 50L);
        assertThat(completed.getAdministrationStatus()).isEqualTo("COMPLETED");
    }

    // --- Failed verification path: scan -> verify fails -> exception logged ---
    @Test
    void failedVerificationPipeline_logsException() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(200L, "DRUG-002", "Fentanyl", 60L, 6L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Verify rights - one fails
        BcmaVerification v = fiveRightsService.verifyRightPatient(2L, 200L, "WB-200", true);
        v = fiveRightsService.verifyRightDrug(2L, "NDC-FENTANYL", false); // Wrong drug scanned

        // Step 3: Finalize - should fail
        BcmaVerification existing = new BcmaVerification();
        existing.setBcmaId(2L);
        existing.setRightPatient(true);
        existing.setRightDrug(false);
        existing.setRightDose(false);
        existing.setRightRoute(false);
        existing.setRightTime(false);
        when(bcmaVerificationRepo.findByBcmaId(2L)).thenReturn(Optional.of(existing));
        BcmaVerification finalized = fiveRightsService.finalizeVerification(2L);
        assertThat(finalized.getAllRightsPassed()).isFalse();
        assertThat(finalized.getAdministered()).isFalse();

        // Step 4: Log exception
        BCMAException exception = bcmaModuleService.logException(200L, "DRUG-002",
                BCMAException.ExceptionType.MISMATCH, "Wrong drug scanned", null,
                Timestamp.from(Instant.now()), 6L, 60L);
        assertThat(exception.getExceptionType()).isEqualTo("MISMATCH");
    }

    // --- Override path: scan -> verify fails -> override -> administer ---
    @Test
    void overridePipeline_administerWithOverride() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(300L, "DRUG-003", "Dopamine", 70L, 7L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Verify rights - patient ID mismatch
        BcmaVerification v = fiveRightsService.verifyRightPatient(3L, 300L, "WRONG-WB", false);

        // Step 3: Log exception for mismatch
        BCMAException exception = exceptionService.createException(300L, "DRUG-003",
                "PATIENT_MISMATCH", "Wristband barcode does not match", 70L, 7L, 70L);
        assertThat(exception.getOverridden()).isFalse();

        // Step 4: Apply override
        BCMAException existing = new BCMAException();
        existing.setExceptionId(exception.getExceptionId());
        existing.setOverridden(false);
        when(exceptionRepo.findByExceptionId(exception.getExceptionId())).thenReturn(Optional.of(existing));
        BCMAException overridden = exceptionService.applyOverride(exception.getExceptionId(),
                "Patient confirmed verbally, wristband damaged", 71L);
        assertThat(overridden.getOverridden()).isTrue();
        assertThat(overridden.getOverrideNurse()).isEqualTo(71L);

        // Step 5: Proceed with administration
        IvpAdministration ivp = ivpService.recordIVP(300L, "Dopamine", BigDecimal.valueOf(400), "mg",
                BigDecimal.valueOf(5.0), true, BigDecimal.valueOf(250), 70L, 7L);
        assertThat(ivp.getAdministrationStatus()).isEqualTo("IN_PROGRESS");
    }

    // --- Stock decrement path: scan -> verify -> administer -> stock decremented ---
    @Test
    void stockDecrementPipeline_stockUpdatedAfterAdministration() {
        // Step 1: Check initial stock
        MedicationStock initialStock = new MedicationStock();
        initialStock.setStockId(1L);
        initialStock.setDrugNdc("NDC-MORPHINE");
        initialStock.setQuantityOnHand(BigDecimal.valueOf(50));
        initialStock.setBatchId("B001");
        initialStock.setExpiryDate(LocalDate.now().plusMonths(6));
        initialStock.setStorageLocation("FRIDGE-A");
        when(stockRepo.findByDrugNdc("NDC-MORPHINE")).thenReturn(List.of(initialStock));

        List<MedicationStock> beforeAdmin = stockService.findByDrug("NDC-MORPHINE");
        assertThat(beforeAdmin.get(0).getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(50));

        // Step 2: Administer medication
        IvpAdministration ivp = ivpService.recordIVP(400L, "Morphine", BigDecimal.valueOf(5), "mg",
                BigDecimal.valueOf(2.0), false, null, 80L, 8L);

        // Step 3: Decrement stock
        MedicationStock updatedStock = stockService.updateStock(1L, "NDC-MORPHINE", "B001",
                BigDecimal.valueOf(45), LocalDate.now().plusMonths(6), "FRIDGE-A");
        assertThat(updatedStock.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(45));
    }

    // --- Expiry alert path: scan -> detect expired -> alert created ---
    @Test
    void expiryAlertPipeline_alertCreatedForExpiredMedication() {
        // Step 1: Check stock with expired medication
        MedicationStock expiredStock = new MedicationStock();
        expiredStock.setStockId(2L);
        expiredStock.setDrugNdc("NDC-EXPIRED");
        expiredStock.setQuantityOnHand(BigDecimal.valueOf(10));
        expiredStock.setBatchId("B002");
        expiredStock.setExpiryDate(LocalDate.now().minusDays(5));
        expiredStock.setStorageLocation("CABINET-1");
        when(stockRepo.findByDrugNdc("NDC-EXPIRED")).thenReturn(List.of(expiredStock));

        List<MedicationStock> stocks = stockService.findByDrug("NDC-EXPIRED");
        assertThat(stocks.get(0).getExpiryDate()).isBefore(LocalDate.now());

        // Step 2: Create expiry alert
        StockAlert alert = stockService.createAlert("NDC-EXPIRED", "EXPIRED",
                "Batch B002 expired 5 days ago", "CRITICAL");
        assertThat(alert.getResolved()).isFalse();
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
    }

    // --- Low stock path: administer -> stock low -> alert created ---
    @Test
    void lowStockPipeline_alertCreatedWhenStockRunsLow() {
        // Step 1: Administer medication
        IvpAdministration ivp = ivpService.recordIVP(500L, "Amoxicillin", BigDecimal.valueOf(500), "mg",
                BigDecimal.valueOf(10.0), false, null, 90L, 9L);

        // Step 2: Update stock to low level
        MedicationStock lowStock = stockService.updateStock(3L, "NDC-AMOX", "B003",
                BigDecimal.valueOf(5), LocalDate.now().plusMonths(3), "DISPENSARY");
        assertThat(lowStock.getQuantityOnHand()).isEqualByComparingTo(BigDecimal.valueOf(5));

        // Step 3: Create low stock alert
        StockAlert alert = stockService.createAlert("NDC-AMOX", "LOW_STOCK",
                "Only 5 units remaining", "WARNING");
        assertThat(alert.getAlertType()).isEqualTo("LOW_STOCK");
    }

    // --- Batch recall path: stock updated -> batch recall alert ---
    @Test
    void batchRecallPipeline_alertCreatedForRecalledBatch() {
        // Step 1: Update stock for recalled batch
        MedicationStock recalledStock = stockService.updateStock(4L, "NDC-RECALL", "B999",
                BigDecimal.valueOf(100), LocalDate.now().plusMonths(6), "WAREHOUSE");

        // Step 2: Create batch recall alert
        StockAlert alert = stockService.createAlert("NDC-RECALL", "BATCH_RECALL",
                "Batch B999 recalled by manufacturer", "CRITICAL");
        assertThat(alert.getAlertType()).isEqualTo("BATCH_RECALL");
        assertThat(alert.getSeverity()).isEqualTo("CRITICAL");
    }

    // --- Multiple patients: concurrent administrations ---
    @Test
    void multiplePatientPipeline_concurrentAdministrations() {
        // Patient A
        PatientDrugMatch scanA = bcmaModuleService.scanMedication(600L, "DRUG-A", "DrugA", 100L, 10L);
        assertThat(scanA.isMatch()).isTrue();

        // Patient B
        PatientDrugMatch scanB = bcmaModuleService.scanMedication(601L, "DRUG-B", "DrugB", 101L, 11L);
        assertThat(scanB.isMatch()).isTrue();

        // Both get IVP
        IvpAdministration ivpA = ivpService.recordIVP(600L, "DrugA", BigDecimal.valueOf(10), "mg",
                BigDecimal.valueOf(5.0), false, null, 100L, 10L);
        IvpAdministration ivpB = ivpService.recordIVP(601L, "DrugB", BigDecimal.valueOf(20), "mg",
                BigDecimal.valueOf(3.0), false, null, 101L, 11L);

        assertThat(ivpA.getAdministrationStatus()).isEqualTo("IN_PROGRESS");
        assertThat(ivpB.getAdministrationStatus()).isEqualTo("IN_PROGRESS");
        assertThat(ivpA.getPatientId()).isEqualTo(600L);
        assertThat(ivpB.getPatientId()).isEqualTo(601L);
    }

    // --- Refused medication path: scan -> patient refuses -> exception logged ---
    @Test
    void refusedMedicationPipeline_exceptionLoggedForRefusedMedication() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(700L, "DRUG-REF", "RefusedDrug", 110L, 12L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Patient refuses - log exception
        BCMAException refusedException = bcmaModuleService.logException(700L, "DRUG-REF",
                BCMAException.ExceptionType.REFUSED, "Patient refused medication", null,
                Timestamp.from(Instant.now()), 12L, 110L);
        assertThat(refusedException.getExceptionType()).isEqualTo("REFUSED");
        assertThat(refusedException.getReason()).isEqualTo("Patient refused medication");
    }

    // --- Missed dose path: scan -> dose missed -> exception logged ---
    @Test
    void missedDosePipeline_exceptionLoggedForMissedDose() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(800L, "DRUG-MISS", "MissedDrug", 120L, 13L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Dose missed - log exception
        BCMAException missedException = bcmaModuleService.logException(800L, "DRUG-MISS",
                BCMAException.ExceptionType.MISSED, "Nurse missed scheduled dose", null,
                Timestamp.from(Instant.now()), 13L, 120L);
        assertThat(missedException.getExceptionType()).isEqualTo("MISSED");
    }

    // --- Delayed administration path: scan -> delayed -> exception logged ---
    @Test
    void delayedAdministrationPipeline_exceptionLoggedForDelayedAdministration() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(900L, "DRUG-DELAY", "DelayedDrug", 130L, 14L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Administration delayed - log exception
        BCMAException delayedException = bcmaModuleService.logException(900L, "DRUG-DELAY",
                BCMAException.ExceptionType.DELAYED, "Administration delayed due to patient unavailability", null,
                Timestamp.from(Instant.now()), 14L, 130L);
        assertThat(delayedException.getExceptionType()).isEqualTo("DELAYED");
    }

    // --- Dilution verification path: IVP with dilution required ---
    @Test
    void dilutionVerificationPipeline_dilutionVerifiedBeforeAdministration() {
        // Step 1: Create drug config requiring dilution
        IvpDrugConfig config = ivpService.createConfig("NDC-DIL", BigDecimal.valueOf(10.0),
                true, BigDecimal.valueOf(100));
        assertThat(config.getRequiresDilution()).isTrue();

        // Step 2: Record IVP
        IvpAdministration ivp = ivpService.recordIVP(1000L, "NDC-DIL", BigDecimal.valueOf(50), "mg",
                BigDecimal.valueOf(5.0), true, BigDecimal.valueOf(100), 140L, 15L);

        // Step 3: Verify dilution
        IvpDrugConfig drugConfig = new IvpDrugConfig();
        drugConfig.setRequiresDilution(true);
        drugConfig.setDrugNdc("NDC-DIL");
        when(ivpDrugConfigRepo.findByDrugNdc("NDC-DIL")).thenReturn(Optional.of(drugConfig));

        IvpAdministration ivpWithDilution = new IvpAdministration();
        ivpWithDilution.setIvpId(ivp.getIvpId());
        ivpWithDilution.setDrugName("NDC-DIL");
        when(ivpAdminRepo.findByIvpId(ivp.getIvpId())).thenReturn(Optional.of(ivpWithDilution));

        IvpAdministration verified = ivpService.verifyDilution(ivp.getIvpId(), true);
        assertThat(verified.getDilutionRequired()).isTrue();
    }

    // --- Override reason management path ---
    @Test
    void overrideReasonPipeline_reasonsManagedAndUsed() {
        // Step 1: Add override reasons
OverrideReason reason1 = exceptionService.addReason("ALLERGY", "Patient has known allergy", "CLINICAL");
        OverrideReason reason2 = exceptionService.addReason("NO-BARCODE", "No barcode available", "TECHNICAL");
        OverrideReason reason3 = exceptionService.addReason("EMERGENCY", "Emergency situation", "URGENT");

        assertThat(reason1.getCode()).isEqualTo("ALLERGY");
        assertThat(reason2.getCode()).isEqualTo("NO-BARCODE");
        assertThat(reason3.getCode()).isEqualTo("EMERGENCY");

        // Step 2: Create exception
        BCMAException exception = exceptionService.createException(1100L, "NDC-OVR",
                "ALLERGY_OVERRIDE", "Administered despite allergy warning", 150L, 16L, 150L);

        // Step 3: Apply override with reason
        BCMAException existing = new BCMAException();
        existing.setExceptionId(exception.getExceptionId());
        existing.setOverridden(false);
        when(exceptionRepo.findByExceptionId(exception.getExceptionId())).thenReturn(Optional.of(existing));
        BCMAException overridden = exceptionService.applyOverride(exception.getExceptionId(),
                "ALLERGY: Allergy was mild, administered with monitoring", 151L);
        assertThat(overridden.getOverridden()).isTrue();
    }

    // --- Alert resolution path: alert created -> resolved ---
    @Test
    void alertResolutionPipeline_alertCreatedThenResolved() {
        // Step 1: Create alert
        StockAlert alert = stockService.createAlert("NDC-RES", "LOW_STOCK",
                "Stock below threshold", "WARNING");
        assertThat(alert.getResolved()).isFalse();

        // Step 2: Resolve alert
        StockAlert existing = new StockAlert();
        existing.setAlertId(alert.getAlertId());
        existing.setResolved(false);
        when(stockAlertRepo.findByAlertId(alert.getAlertId())).thenReturn(Optional.of(existing));
        StockAlert resolved = stockService.resolveAlert(alert.getAlertId());
        assertThat(resolved.getResolved()).isTrue();
        assertThat(resolved.getResolvedAt()).isNotNull();
    }

    // --- Full audit trail path: scan -> verify -> exception -> override -> resolve ---
    @Test
    void fullAuditTrailPipeline_completeAuditTrail() {
        // Step 1: Scan medication
        PatientDrugMatch scanResult = bcmaModuleService.scanMedication(1200L, "DRUG-AUDIT", "AuditDrug", 160L, 17L);
        assertThat(scanResult.isMatch()).isTrue();

        // Step 2: Verify rights with one failure
        BcmaVerification v = fiveRightsService.verifyRightPatient(10L, 1200L, "WB-AUDIT", true);
        v = fiveRightsService.verifyRightDrug(10L, "NDC-AUDIT", false); // Mismatch

        // Step 3: Log exception
        BCMAException exception = exceptionService.createException(1200L, "NDC-AUDIT",
                "DRUG_MISMATCH", "Wrong drug scanned", 160L, 17L, 160L);
        verify(exceptionRepo).save(any());

        // Step 4: Apply override
        BCMAException existing = new BCMAException();
        existing.setExceptionId(exception.getExceptionId());
        existing.setOverridden(false);
        when(exceptionRepo.findByExceptionId(exception.getExceptionId())).thenReturn(Optional.of(existing));
        BCMAException overridden = exceptionService.applyOverride(exception.getExceptionId(),
                "Clinical override: correct drug confirmed", 161L);
        verify(exceptionRepo, times(2)).save(any());

        // Step 5: Create stock alert
        StockAlert alert = stockService.createAlert("NDC-AUDIT", "AUDIT_ALERT",
                "Override applied for drug mismatch", "WARNING");
        verify(stockAlertRepo).save(any());

        // Step 6: Resolve alert
        StockAlert alertExisting = new StockAlert();
        alertExisting.setAlertId(alert.getAlertId());
        alertExisting.setResolved(false);
        when(stockAlertRepo.findByAlertId(alert.getAlertId())).thenReturn(Optional.of(alertExisting));
        StockAlert resolved = stockService.resolveAlert(alert.getAlertId());
        assertThat(resolved.getResolved()).isTrue();
    }

    // --- IVP status transitions ---
    @Test
    void ivpStatusPipeline_pendingToInProgressToCompleted() {
        // Record creates IN_PROGRESS
        IvpAdministration ivp = ivpService.recordIVP(1300L, "StatusDrug", BigDecimal.valueOf(10), "mg",
                BigDecimal.valueOf(5.0), false, null, 170L, 18L);
        assertThat(ivp.getAdministrationStatus()).isEqualTo("IN_PROGRESS");

        // Complete transitions to COMPLETED
        IvpAdministration existing = new IvpAdministration();
        existing.setIvpId(ivp.getIvpId());
        existing.setAdministrationStatus("IN_PROGRESS");
        when(ivpAdminRepo.findByIvpId(ivp.getIvpId())).thenReturn(Optional.of(existing));
        IvpAdministration completed = ivpService.completeIVP(ivp.getIvpId(), 170L);
        assertThat(completed.getAdministrationStatus()).isEqualTo("COMPLETED");
    }

    // --- Stock query by location ---
    @Test
    void stockQueryByLocationPipeline_locationBasedQueries() {
        // Add stock in different locations
        MedicationStock ward1Stock = new MedicationStock();
        ward1Stock.setStockId(10L);
        ward1Stock.setDrugNdc("NDC-LOC");
        ward1Stock.setStorageLocation("WARD-1");
        MedicationStock ward2Stock = new MedicationStock();
        ward2Stock.setStockId(11L);
        ward2Stock.setDrugNdc("NDC-LOC");
        ward2Stock.setStorageLocation("WARD-2");
        when(stockRepo.findByDrugNdc("NDC-LOC")).thenReturn(List.of(ward1Stock, ward2Stock));

        List<MedicationStock> results = stockService.findByDrug("NDC-LOC");
        assertThat(results).hasSize(2);
    }

    // --- Multiple verifications for same patient ---
    @Test
    void multipleVerificationsForSamePatient_pipeline() {
        // First verification
        BcmaVerification v1 = fiveRightsService.verifyRightPatient(20L, 1400L, "WB-1400", true);
        when(bcmaVerificationRepo.findByBcmaId(20L)).thenReturn(Optional.of(v1));
        v1 = fiveRightsService.verifyRightDrug(20L, "NDC-1400-A", true);

        // Second verification (different medication)
        BcmaVerification v2 = fiveRightsService.verifyRightPatient(21L, 1400L, "WB-1400", true);
        when(bcmaVerificationRepo.findByBcmaId(21L)).thenReturn(Optional.of(v2));
        v2 = fiveRightsService.verifyRightDrug(21L, "NDC-1400-B", true);

        assertThat(v1.getBcmaId()).isEqualTo(20L);
        assertThat(v2.getBcmaId()).isEqualTo(21L);
        assertThat(v1.getPatientId()).isEqualTo(1400L);
        assertThat(v2.getPatientId()).isEqualTo(1400L);
    }
}
