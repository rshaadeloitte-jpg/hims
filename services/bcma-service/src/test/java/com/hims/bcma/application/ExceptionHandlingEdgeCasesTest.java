package com.hims.bcma.application;

import com.hims.bcma.domain.model.BCMAException;
import com.hims.bcma.domain.model.OverrideReason;
import com.hims.bcma.domain.repository.BCMAExceptionRepository;
import com.hims.bcma.domain.repository.OverrideReasonRepository;
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

class ExceptionHandlingEdgeCasesTest {

    private ExceptionHandlingService service;
    private BCMAExceptionRepository excRepo;
    private OverrideReasonRepository reasonRepo;

    @BeforeEach
    void setUp() {
        excRepo = mock(BCMAExceptionRepository.class);
        reasonRepo = mock(OverrideReasonRepository.class);
        service = new ExceptionHandlingService(excRepo, reasonRepo);
        when(excRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reasonRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Override authorization levels ---
    @Test
    void applyOverride_setsOverrideFields() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(1L);
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(1L)).thenReturn(Optional.of(exc));

        BCMAException result = service.applyOverride(1L, "Emergency situation", 101L);

        assertThat(result.getOverridden()).isTrue();
        assertThat(result.getOverrideNurse()).isEqualTo(101L);
        assertThat(result.getReason()).isEqualTo("Emergency situation");
    }

    @Test
    void applyOverride_whenExceptionNotFound_throwsIllegalArgumentException() {
        when(excRepo.findByExceptionId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.applyOverride(404L, "justification", 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exception not found");
    }

    @Test
    void applyOverride_updatesReasonField() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(2L);
        exc.setReason("Original reason");
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(2L)).thenReturn(Optional.of(exc));

        BCMAException result = service.applyOverride(2L, "New justification", 200L);

        assertThat(result.getReason()).isEqualTo("New justification");
    }

    @Test
    void applyOverride_withNullJustification_setsNullReason() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(3L);
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(3L)).thenReturn(Optional.of(exc));

        BCMAException result = service.applyOverride(3L, null, 300L);

        assertThat(result.getReason()).isNull();
        assertThat(result.getOverridden()).isTrue();
    }

    @Test
    void applyOverride_withNullOverrideNurse_setsNull() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(4L);
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(4L)).thenReturn(Optional.of(exc));

        BCMAException result = service.applyOverride(4L, "reason", null);

        assertThat(result.getOverrideNurse()).isNull();
    }

    // --- Mandatory reason codes ---
    @Test
    void addReason_setsCodeDescriptionCategoryAndActive() {
        OverrideReason r = service.addReason("ALLERGY", "Patient has known allergy", "CLINICAL");

        assertThat(r.getCode()).isEqualTo("ALLERGY");
        assertThat(r.getDescription()).isEqualTo("Patient has known allergy");
        assertThat(r.getCategory()).isEqualTo("CLINICAL");
        assertThat(r.getIsActive()).isTrue();
    }

    @Test
    void addReason_withEmptyCategory_succeeds() {
        OverrideReason r = service.addReason("NO-NDC", "No barcode found", "");

        assertThat(r.getCode()).isEqualTo("NO-NDC");
        assertThat(r.getCategory()).isEmpty();
        assertThat(r.getIsActive()).isTrue();
    }

    @Test
    void addReason_withNullCode_setsNull() {
        OverrideReason r = service.addReason(null, "description", "CAT");

        assertThat(r.getCode()).isNull();
        assertThat(r.getIsActive()).isTrue();
    }

    @Test
    void addReason_withMaxLengthDescription_succeeds() {
        String longDesc = "A".repeat(500);
        OverrideReason r = service.addReason("LONG", longDesc, "OTHER");

        assertThat(r.getDescription()).hasSize(500);
    }

    @Test
    void addReason_generatesUniqueIds() {
        OverrideReason r1 = service.addReason("CODE-1", "First reason", "A");
        try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        OverrideReason r2 = service.addReason("CODE-1", "Second reason", "B");

        assertThat(r1.getReasonId()).isNotEqualTo(r2.getReasonId());
    }

    // --- Clinical judgment trails ---
    @Test
    void createException_tracksFullContext() {
        BCMAException exc = service.createException(1L, "NDC-123", "CLINICAL_JUDGMENT",
                "Administered without wristband due to emergency", 101L, 5L, 201L);

        assertThat(exc.getPatientId()).isEqualTo(1L);
        assertThat(exc.getDrugIen()).isEqualTo("NDC-123");
        assertThat(exc.getExceptionType()).isEqualTo("CLINICAL_JUDGMENT");
        assertThat(exc.getReason()).isEqualTo("Administered without wristband due to emergency");
        assertThat(exc.getOverrideNurse()).isEqualTo(101L);
        assertThat(exc.getWardId()).isEqualTo(5L);
        assertThat(exc.getScannedBy()).isEqualTo(201L);
        assertThat(exc.getOverridden()).isFalse();
    }

    @Test
    void createException_setsExceptionTimeToNow() {
        Timestamp before = Timestamp.from(Instant.now());
        BCMAException exc = service.createException(2L, "NDC-456", "EMERGENCY", "Urgent",
                null, 1L, null);
        Timestamp after = Timestamp.from(Instant.now());

        assertThat(exc.getExceptionTime()).isBetween(before, after);
    }

    @Test
    void createException_setsOverriddenToFalse() {
        BCMAException exc = service.createException(3L, "NDC-789", "REASON", "desc",
                null, 2L, null);

        assertThat(exc.getOverridden()).isFalse();
    }

    @Test
    void applyOverride_createsAuditTrailOfJustification() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(5L);
        exc.setOverridden(false);
        exc.setReason("Initial: No barcode");
        when(excRepo.findByExceptionId(5L)).thenReturn(Optional.of(exc));

        BCMAException result = service.applyOverride(5L, "Override: Emergency protocol followed", 301L);

        // Audit trail: reason updated to justification, overridden set, nurse recorded
        assertThat(result.getReason()).isEqualTo("Override: Emergency protocol followed");
        assertThat(result.getOverridden()).isTrue();
        assertThat(result.getOverrideNurse()).isEqualTo(301L);
    }

    // --- Audit logging ---
    @Test
    void createException_savesToRepository() {
        service.createException(4L, "NDC-AUDIT", "AUDIT_TYPE", "audit log test",
                50L, 3L, 60L);

        verify(excRepo).save(argThat(exc ->
                exc.getPatientId().equals(4L) &&
                exc.getDrugIen().equals("NDC-AUDIT") &&
                exc.getExceptionType().equals("AUDIT_TYPE")));
    }

    @Test
    void applyOverride_savesToRepository() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(6L);
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(6L)).thenReturn(Optional.of(exc));

        service.applyOverride(6L, "saved justification", 401L);

        verify(excRepo).save(argThat(e ->
                e.getOverridden().equals(true) &&
                e.getReason().equals("saved justification")));
    }

    @Test
    void addReason_savesToRepository() {
        service.addReason("AUDIT-CODE", "Audit reason description", "AUDIT");

        verify(reasonRepo).save(argThat(r ->
                r.getCode().equals("AUDIT-CODE") &&
                r.getIsActive().equals(true)));
    }

    @Test
    void createException_withNullPatientId_succeeds() {
        BCMAException exc = service.createException(null, "NDC-X", "TYPE", "reason",
                null, null, null);

        assertThat(exc.getPatientId()).isNull();
        assertThat(exc.getExceptionType()).isEqualTo("TYPE");
    }

    @Test
    void createException_withAllFields() {
        BCMAException exc = service.createException(5L, "NDC-FULL", "FULL_TYPE",
                "Full exception record", 500L, 10L, 510L);

        assertThat(exc.getExceptionId()).isNotNull();
        assertThat(exc.getPatientId()).isEqualTo(5L);
        assertThat(exc.getDrugIen()).isEqualTo("NDC-FULL");
        assertThat(exc.getExceptionType()).isEqualTo("FULL_TYPE");
        assertThat(exc.getReason()).isEqualTo("Full exception record");
        assertThat(exc.getOverrideNurse()).isEqualTo(500L);
        assertThat(exc.getWardId()).isEqualTo(10L);
        assertThat(exc.getScannedBy()).isEqualTo(510L);
        assertThat(exc.getExceptionTime()).isNotNull();
        assertThat(exc.getOverridden()).isFalse();
    }

    @Test
    void addReason_withNullDescription_setsNull() {
        OverrideReason r = service.addReason("CODE-NULL", null, "CAT");

        assertThat(r.getDescription()).isNull();
    }

    @Test
    void findByPatient_returnsExceptions() {
        BCMAException exc1 = new BCMAException();
        exc1.setExceptionId(10L);
        BCMAException exc2 = new BCMAException();
        exc2.setExceptionId(11L);
        when(excRepo.findByPatientId(77L)).thenReturn(List.of(exc1, exc2));

        List<BCMAException> result = service.findByPatient(77L);

        assertThat(result).hasSize(2);
    }

    @Test
    void findByPatient_returnsEmptyWhenNoExceptions() {
        when(excRepo.findByPatientId(88L)).thenReturn(List.of());

        List<BCMAException> result = service.findByPatient(88L);

        assertThat(result).isEmpty();
    }

    @Test
    void applyOverride_canBeCalledMultipleTimes() {
        BCMAException exc = new BCMAException();
        exc.setExceptionId(7L);
        exc.setOverridden(false);
        when(excRepo.findByExceptionId(7L)).thenReturn(Optional.of(exc));
        when(excRepo.save(any())).thenAnswer(inv -> {
            BCMAException arg = inv.getArgument(0);
            BCMAException copy = new BCMAException();
            copy.setExceptionId(arg.getExceptionId());
            copy.setReason(arg.getReason());
            copy.setOverrideNurse(arg.getOverrideNurse());
            copy.setOverridden(arg.getOverridden());
            copy.setOverrideReason(arg.getOverrideReason());
            copy.setPatientId(arg.getPatientId());
            copy.setDrugIen(arg.getDrugIen());
            copy.setExceptionType(arg.getExceptionType());
            copy.setExceptionTime(arg.getExceptionTime());
            copy.setWardId(arg.getWardId());
            copy.setScannedBy(arg.getScannedBy());
            return copy;
        });

        BCMAException first = service.applyOverride(7L, "First override", 501L);
        BCMAException second = service.applyOverride(7L, "Second override", 502L);

        assertThat(first.getReason()).isEqualTo("First override");
        assertThat(second.getReason()).isEqualTo("Second override");
        assertThat(second.getOverrideNurse()).isEqualTo(502L);
    }

    @Test
    void createException_withNullDrugNdc_createsException() {
        BCMAException exc = service.createException(6L, null, "TYPE", "reason",
                null, null, null);

        assertThat(exc.getDrugIen()).isNull();
        assertThat(exc.getExceptionType()).isEqualTo("TYPE");
    }
}
