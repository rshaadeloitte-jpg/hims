package com.hims.ot.application;

import com.hims.ot.application.exceptions.InvalidOtRequestException;
import com.hims.ot.domain.model.OtSchedule;
import com.hims.ot.domain.model.OtScheduleDetail;
import com.hims.ot.domain.repository.OtScheduleDetailRepository;
import com.hims.ot.domain.repository.OtScheduleRepository;
import com.hims.ot.infrastructure.outbox.OutboxEvent;
import com.hims.ot.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L1 — BR-OT-001/BR-OT-002 rules and the lifecycle state machine, no Spring context.
 * Also verifies exact PROPOSED outbox contracts (ProcedureScheduled / SurgeryCompleted,
 * partition key patientId) and that cancel emits NO event.
 */
class OtScheduleServiceTest {

    private OtScheduleRepository scheduleRepository;
    private OtScheduleDetailRepository detailRepository;
    private OutboxService outboxService;
    private EntityManager em;
    private OtScheduleService service;
    private long nextSeq = 100;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(OtScheduleRepository.class);
        detailRepository = mock(OtScheduleDetailRepository.class);
        outboxService = mock(OutboxService.class);
        em = mock(EntityManager.class);
        service = new OtScheduleService(scheduleRepository, detailRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> nextSeq++);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Timestamp ts(String s) {
        return Timestamp.valueOf(s);
    }

    private OtScheduleService.ProcedureRequest proc(String code, String name, String rate,
                                                    Integer dependentOfIndex, String discountPct) {
        return new OtScheduleService.ProcedureRequest(code, name,
                rate == null ? null : new BigDecimal(rate),
                dependentOfIndex,
                discountPct == null ? null : new BigDecimal(discountPct));
    }

    // ------------------------------------------------------------------ BR-OT-001

    @Test
    void brOt001_neitherAdmissionNorEncounter_throwsBEOT004_noRowsSaved() {
        assertThatThrownBy(() -> service.create(501L, null, null, "OT-1",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, false,
                List.of(proc("P1", "Appendectomy", "5000", null, null))))
                .isInstanceOf(InvalidOtRequestException.class)
                .extracting(e -> ((InvalidOtRequestException) e).getErrorCode())
                .isEqualTo("BEOT004");
        verify(scheduleRepository, never()).save(any());
        verify(detailRepository, never()).save(any());
    }

    @Test
    void brOt001_encounterIdOnly_isAccepted() {
        OtSchedule s = service.create(501L, 900L, null, "OT-1",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, false,
                List.of(proc("P1", "Appendectomy", null, null, null)));
        assertThat(s.getEncounterId()).isEqualTo(900L);
        // rate defaults to 0 (tariff lookup deferred to Reference integration)
        assertThat(s.getDetails().get(0).getRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void brOt001_admissionIdOnly_isAccepted() {
        OtSchedule s = service.create(501L, null, 77L, "OT-2",
                ts("2026-09-01 10:00:00"), null, 7L, 9L, false,
                List.of(proc("P1", "Hernia repair", "8000", null, null)));
        assertThat(s.getAdmissionId()).isEqualTo(77L);
    }

    @Test
    void brOt001_dependentSurgery_discountApplied_rateDefaulted() {
        OtSchedule s = service.create(501L, 900L, null, "OT-1",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, true,
                List.of(
                        proc("MAIN", "Primary procedure", "10000", null, null),
                        proc("DEP", "Dependent procedure", null, 0, "25.00")));

        OtScheduleDetail dep = s.getDetails().get(1);
        assertThat(dep.getDependentOf()).isNotNull();
        assertThat(dep.getDiscountPct()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(dep.getRate()).isEqualByComparingTo(BigDecimal.ZERO);          // default 0
        assertThat(dep.getNetRate()).isEqualByComparingTo(new BigDecimal("0.00"));

        OtScheduleDetail main = s.getDetails().get(0);
        assertThat(main.getDependentOf()).isNull();
        assertThat(main.getDiscountPct()).isEqualByComparingTo(BigDecimal.ZERO);  // non-dependent rows carry no discount

        // discount on a NON-dependent row is rejected
        assertThatThrownBy(() -> service.create(501L, 900L, null, "OT-1",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, false,
                List.of(proc("X", "X", "100", null, "50"))))
                .isInstanceOf(InvalidOtRequestException.class);
    }

    // ------------------------------------------------------------------ BR-OT-002

    @Test
    void brOt002_preBillingFlagPersistedAndEchoed() {
        OtSchedule on = service.create(501L, 900L, null, "OT-1",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, true,
                List.of(proc("P1", "P1", "1000", null, null)));
        assertThat(on.getPreBilling()).isTrue();

        OtSchedule off = service.create(502L, 901L, null, "OT-2",
                ts("2026-09-01 08:00:00"), null, 7L, 9L, null,
                List.of(proc("P1", "P1", "1000", null, null)));
        assertThat(off.getPreBilling()).isFalse();   // defaults false

        // flag echoed in ProcedureScheduled payload; charge creation itself is deferred to Billing Wave 4
        ArgumentCaptor<String> payloadCap = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(eq(OtScheduleService.OutboxEvents.PROCEDURE_SCHEDULED),
                eq("schedule"), eq(on.getScheduleId()), eq("501"), payloadCap.capture(), any());
        assertThat(payloadCap.getValue()).contains("\"preBilling\":true");
    }

    // ------------------------------------------------------------------ lifecycle guards

    private OtSchedule persisted(short status) {
        OtSchedule s = new OtSchedule();
        s.setScheduleId(10L);
        s.setPatientId(501L);
        s.setEncounterId(900L);
        s.setOtTable("OT-1");
        s.setScheduledStart(ts("2026-09-01 08:00:00"));
        s.setStatus(status);
        s.setPreBilling(false);
        when(scheduleRepository.findById(10L)).thenReturn(java.util.Optional.of(s));
        return s;
    }

    @Test
    void start_onlyFromPlanned() {
        assertThat(persisted(OtSchedule.STATUS_PLANNED));
        OtSchedule started = service.start(10L);
        assertThat(started.getStatus()).isEqualTo(OtSchedule.STATUS_IN_PROGRESS);

        persisted(OtSchedule.STATUS_CANCELLED);
        assertThatThrownBy(() -> service.start(10L))
                .isInstanceOf(InvalidOtRequestException.class)
                .extracting(e -> ((InvalidOtRequestException) e).getErrorCode())
                .isEqualTo("OT_ILLEGAL_TRANSITION");

        persisted(OtSchedule.STATUS_DONE);
        assertThatThrownBy(() -> service.start(10L))
                .isInstanceOf(InvalidOtRequestException.class);

        // no events from start
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void complete_onlyFromInProgress_emitsSurgeryCompleted() {
        persisted(OtSchedule.STATUS_PLANNED);
        assertThatThrownBy(() -> service.complete(10L))
                .isInstanceOf(InvalidOtRequestException.class);
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());

        when(detailRepository.findByScheduleIdOrderByDetailIdAsc(10L)).thenReturn(List.of());
        OtSchedule done = completeFromInProgress();
        assertThat(done.getStatus()).isEqualTo(OtSchedule.STATUS_DONE);

        // exact SurgeryCompleted contract — PROPOSED event, partition key patientId
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(type.capture(), eq("schedule"), eq(10L), partitionKey.capture(),
                any(), eq("surgery-completed:10"));
        assertThat(type.getValue()).isEqualTo("SurgeryCompleted");       // PROPOSED per §3
        assertThat(partitionKey.getValue()).isEqualTo("501");            // patientId
    }

    private OtSchedule completeFromInProgress() {
        persisted(OtSchedule.STATUS_IN_PROGRESS);
        when(detailRepository.findByScheduleIdOrderByDetailIdAsc(10L)).thenReturn(List.of());
        return service.complete(10L);
    }

    @Test
    void cancel_fromPlannedOrInProgress_only_neverFromDone() {
        persisted(OtSchedule.STATUS_PLANNED);
        assertThat(service.cancel(10L).getStatus()).isEqualTo(OtSchedule.STATUS_CANCELLED);

        persisted(OtSchedule.STATUS_IN_PROGRESS);
        assertThat(service.cancel(10L).getStatus()).isEqualTo(OtSchedule.STATUS_CANCELLED);

        persisted(OtSchedule.STATUS_DONE);
        assertThatThrownBy(() -> service.cancel(10L))
                .isInstanceOf(InvalidOtRequestException.class)
                .extracting(e -> ((InvalidOtRequestException) e).getErrorCode())
                .isEqualTo("OT_ILLEGAL_TRANSITION");

        // double-cancel rejected
        persisted(OtSchedule.STATUS_CANCELLED);
        assertThatThrownBy(() -> service.cancel(10L))
                .isInstanceOf(InvalidOtRequestException.class);

        // cancel emits NO outbox event (only create/complete do)
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    // ------------------------------------------------------------------ ProcedureScheduled contract

    @Test
    void create_emitsProcedureScheduled_withPatientIdPartitionKey() {
        OtSchedule s = service.create(501L, 900L, null, "OT-1",
                ts("2026-09-01 08:00:00"), ts("2026-09-01 10:00:00"), 7L, 9L, false,
                List.of(proc("MAIN", "Main op", "10000", null, null)));

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> aggregateType = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> aggregateId = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> partitionKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> idem = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(type.capture(), aggregateType.capture(), aggregateId.capture(),
                partitionKey.capture(), any(), idem.capture());
        assertThat(type.getValue()).isEqualTo("ProcedureScheduled");   // PROPOSED per §3
        assertThat(partitionKey.getValue()).isEqualTo("501");          // patientId
        assertThat(aggregateType.getValue()).isEqualTo("schedule");
        assertThat(aggregateId.getValue()).isEqualTo(s.getScheduleId());
        assertThat(idem.getValue()).isEqualTo("procedure-scheduled:" + s.getScheduleId());
    }
}
