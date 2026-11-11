package com.hims.nursing.application;

import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.application.exceptions.NursingResourceNotFoundException;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.repository.NurseNoteRepository;
import com.hims.nursing.domain.repository.NurseTaskRepository;
import com.hims.nursing.domain.repository.PatientVitalsRepository;
import com.hims.nursing.infrastructure.outbox.OutboxEvent;
import com.hims.nursing.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Optional;

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
 * L1 — nursing slice rules, no Spring context: validation (BEXA003), task
 * transition guard (PENDING->COMPLETED only), and outbox event emission
 * (PROPOSED events VitalsRecorded / NurseTaskCompleted).
 */
class NursingChartingServiceTest {

    private PatientVitalsRepository vitalsRepository;
    private NurseNoteRepository noteRepository;
    private NurseTaskRepository taskRepository;
    private OutboxService outboxService;
    private EntityManager em;
    private NursingChartingService service;
    private long nextSeq = 100;

    @BeforeEach
    void setUp() {
        vitalsRepository = mock(PatientVitalsRepository.class);
        noteRepository = mock(NurseNoteRepository.class);
        taskRepository = mock(NurseTaskRepository.class);
        outboxService = mock(OutboxService.class);
        em = mock(EntityManager.class);
        service = new NursingChartingService(vitalsRepository, noteRepository, taskRepository, outboxService);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> nextSeq++);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(vitalsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private NurseTask pendingTask(long id) {
        NurseTask t = new NurseTask();
        t.setId(id);
        t.setAdmissionId(9L);
        t.setTaskDesc("Administer IV antibiotic");
        t.setFrequency("TID");
        t.setTaskStatus(NurseTask.STATUS_PENDING);
        return t;
    }

    // ------------------------------------------------------------- vitals

    @Test
    void recordVital_emitsProposedVitalsRecordedToOutbox() {
        service.recordVital(501L, 9L, "Blood Pressure Systolic", new BigDecimal("120.00"), "mmHg", null, 42L);

        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(type.capture(), eq("vitals"), any(),
                eq("501"), payload.capture(), contains("vitals-recorded"));
        assertThat(type.getValue()).isEqualTo("VitalsRecorded");   // PROPOSED event string per register
        assertThat(payload.getValue()).contains("\"vitalsType\":\"Blood Pressure Systolic\"")
                .contains("\"patientId\":501").contains("\"admissionId\":9");
    }

    @Test
    void recordVital_missingValueRejectedBexa003() {
        assertThatThrownBy(() -> service.recordVital(501L, null, "Heart Rate", null, "bpm", null, null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .hasMessageContaining("value")
                .extracting(e -> ((InvalidNursingRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordVital_nonNumericValueRejectedBexa003() {
        assertThatThrownBy(() -> service.recordVital(501L, null, "Heart Rate",
                new BigDecimal("not-a-number"), null, null, null))
                .isInstanceOf(NumberFormatException.class);   // controller layer parses -> BEXA003 400
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void recordVital_negativeValueAndFutureTimestampRejected() {
        assertThatThrownBy(() -> service.recordVital(501L, null, "Temp", new BigDecimal("-1"), "C", null, null))
                .isInstanceOf(InvalidNursingRequestException.class);
        assertThatThrownBy(() -> service.recordVital(501L, null, "Temp", new BigDecimal("37"),
                "C", new Timestamp(System.currentTimeMillis() + 3_600_000), null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .hasMessageContaining("future");
    }

    // ------------------------------------------------------------- notes

    @Test
    void addNote_blankNoteRejectedBexa003() {
        assertThatThrownBy(() -> service.addNote(9L, "   ", null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .extracting(e -> ((InvalidNursingRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    // ------------------------------------------------------------- tasks

    @Test
    void completeTask_pendingToCompletedEmitsProposedNurseTaskCompleted() {
        when(taskRepository.findById(7L)).thenReturn(Optional.of(pendingTask(7L)));

        NurseTask done = service.completeTask(7L, 42L);

        assertThat(done.getTaskStatus()).isEqualTo(NurseTask.STATUS_COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();
        ArgumentCaptor<String> type = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).append(type.capture(), eq("task"), eq(7L),
                eq("9"), payload.capture(), contains("nurse-task-completed"));
        assertThat(type.getValue()).isEqualTo("NurseTaskCompleted");   // PROPOSED event string per register
        assertThat(payload.getValue()).contains("\"taskId\":7").contains("\"completedBy\":42");
    }

    @Test
    void completeTask_fromCompletedIsIllegalTransition400() {
        NurseTask completed = pendingTask(8L);
        completed.setTaskStatus(NurseTask.STATUS_COMPLETED);
        when(taskRepository.findById(8L)).thenReturn(Optional.of(completed));

        assertThatThrownBy(() -> service.completeTask(8L, null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .hasMessageContaining("PENDING")
                .extracting(e -> ((InvalidNursingRequestException) e).getErrorCode())
                .isEqualTo("NURSING_ILLEGAL_TRANSITION");
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void completeTask_fromCancelledIsIllegalTransition400() {
        NurseTask cancelled = pendingTask(9L);
        cancelled.setTaskStatus(NurseTask.STATUS_CANCELLED);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.completeTask(9L, null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .extracting(e -> ((InvalidNursingRequestException) e).getErrorCode())
                .isEqualTo("NURSING_ILLEGAL_TRANSITION");
    }

    @Test
    void completeTask_unknownTask404() {
        when(taskRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeTask(404L, null))
                .isInstanceOf(NursingResourceNotFoundException.class);
    }

    @Test
    void cancelTask_pendingToCancelled_noEvent() {
        when(taskRepository.findById(5L)).thenReturn(Optional.of(pendingTask(5L)));
        assertThat(service.cancelTask(5L).getTaskStatus()).isEqualTo(NurseTask.STATUS_CANCELLED);
        verify(outboxService, never()).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void listTasks_invalidStatusFilterRejectedBexa003() {
        assertThatThrownBy(() -> service.listTasks((short) 9, null))
                .isInstanceOf(InvalidNursingRequestException.class)
                .extracting(e -> ((InvalidNursingRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }
}
