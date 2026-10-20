package com.hims.nursing.infrastructure.persistence;

import com.hims.nursing.NursingServiceApplication;
import com.hims.nursing.application.NursingChartingService;
import com.hims.nursing.application.exceptions.InvalidNursingRequestException;
import com.hims.nursing.domain.model.NurseNote;
import com.hims.nursing.domain.model.NurseTask;
import com.hims.nursing.domain.model.PatientVitals;
import com.hims.nursing.infrastructure.outbox.OutboxEvent;
import com.hims.nursing.infrastructure.outbox.OutboxRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-6 nursing service on real PG16: V1 schema applies (ihms_nursing),
 * vitals -> notes -> task create/complete flow with the transition guard,
 * and the outbox rows for the PROPOSED events VitalsRecorded + NurseTaskCompleted.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = NursingServiceApplication.class)
@DirtiesContext
@Transactional
class NursingServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private NursingChartingService chartingService;

    @Autowired
    private OutboxRepository outboxRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void schemaAppliesAndVitalsNotesTasksFlowWritesOutboxRows() {
        // V1 applied against PG16: all tables exist in ihms_nursing
        Long tables = (Long) em.createNativeQuery("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'ihms_nursing'
                  AND table_name IN ('t_patientvitals','t_nursenote','t_nursetaskdetails','outbox')
                """).getSingleResult();
        assertThat(tables).isEqualTo(4L);

        // ---- vitals: record + range listing + PROPOSED VitalsRecorded outbox row
        PatientVitals vital = chartingService.recordVital(501L, 9L, "Blood Pressure Systolic",
                new BigDecimal("120.00"), "mmHg", null, 42L);
        assertThat(vital.getId()).isNotNull();
        assertThat(chartingService.listVitals(501L, null, null)).hasSize(1);
        assertThat(chartingService.listVitals(502L, null, null)).isEmpty();

        List<OutboxEvent> vitalsEvents = outboxRepository.findAll();
        assertThat(vitalsEvents).hasSize(1);
        OutboxEvent ev = vitalsEvents.get(0);
        assertThat(ev.getEventType()).isEqualTo("VitalsRecorded");   // PROPOSED event type string per register
        assertThat(ev.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(ev.getAggregateType()).isEqualTo("vitals");
        assertThat(ev.getPartitionKey()).isEqualTo("501");
        assertThat(ev.getPayload()).contains("\"patientId\":501").contains("\"vitalsType\":\"Blood Pressure Systolic\"");
        assertThat(em.createNativeQuery(
                "SELECT COUNT(*) FROM ihms_nursing.t_patientvitals WHERE id = ?")
                .setParameter(1, vital.getId()).getSingleResult()).isEqualTo(1L);

        // ---- notes
        NurseNote note = chartingService.addNote(9L, "Patient stable after rounds.", 7L);
        assertThat(note.getId()).isNotNull();
        assertThat(chartingService.listNotes(9L)).hasSize(1);

        // ---- tasks: create PENDING -> complete emits PROPOSED NurseTaskCompleted
        NurseTask task = chartingService.createTask(9L, "Administer IV antibiotic", "TID",
                Timestamp.valueOf("2026-08-24 08:00:00"));
        assertThat(task.getId()).isNotNull();
        assertThat(task.getTaskStatus()).isEqualTo(NurseTask.STATUS_PENDING);

        NurseTask completed = chartingService.completeTask(task.getId(), 42L);
        assertThat(completed.getTaskStatus()).isEqualTo(NurseTask.STATUS_COMPLETED);

        // transition guard at service level AND persisted status in DB
        assertThatThrownBy(() -> chartingService.completeTask(task.getId(), 42L))
                .isInstanceOf(InvalidNursingRequestException.class);
        Short dbStatus = (Short) em.createNativeQuery(
                        "SELECT task_status FROM ihms_nursing.t_nursetaskdetails WHERE id = ?")
                .setParameter(1, task.getId()).getSingleResult();
        assertThat(dbStatus.shortValue()).isEqualTo(NurseTask.STATUS_COMPLETED);

        // cancel path + illegal complete-from-cancelled
        NurseTask other = chartingService.createTask(9L, "Reposition q2h", "Q2H", null);
        assertThat(chartingService.cancelTask(other.getId()).getTaskStatus()).isEqualTo(NurseTask.STATUS_CANCELLED);
        assertThatThrownBy(() -> chartingService.completeTask(other.getId(), null))
                .isInstanceOf(InvalidNursingRequestException.class);

        // ---- outbox: VitalsRecorded x1 + NurseTaskCompleted x1 (PROPOSED types verbatim)
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).extracting(OutboxEvent::getEventType)
                .containsExactlyInAnyOrder("VitalsRecorded", "NurseTaskCompleted");
        OutboxEvent taskEvent = events.stream()
                .filter(e -> "NurseTaskCompleted".equals(e.getEventType()))
                .findFirst().orElseThrow();
        assertThat(taskEvent.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(taskEvent.getAggregateType()).isEqualTo("task");
        assertThat(taskEvent.getPartitionKey()).isEqualTo("9");   // admissionId partition key for task completion
        assertThat(taskEvent.getPayload()).contains("\"taskId\":" + task.getId())
                .contains("\"completedBy\":42");
    }
}
