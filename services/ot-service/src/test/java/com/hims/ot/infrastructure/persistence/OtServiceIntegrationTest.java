package com.hims.ot.infrastructure.persistence;

import com.hims.ot.OtServiceApplication;
import com.hims.ot.application.OtScheduleService;
import com.hims.ot.domain.model.OtSchedule;
import com.hims.ot.infrastructure.outbox.OutboxEvent;
import com.hims.ot.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
 * L2 — WP-8 OT service on real PG16: V1 schema applies (ihms_ot), schedule create
 * (BR-OT-001 prerequisites + dependent-surgery discount), lifecycle
 * start/complete/cancel guards, BR-OT-002 pre_billing persistence, and EXACT outbox
 * rows for PROPOSED ProcedureScheduled / SurgeryCompleted (type strings, payload keys,
 * patientId partition keys; cancel emits nothing).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = OtServiceApplication.class)
@DirtiesContext
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OtServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private OtScheduleService service;

    @Autowired
    private OutboxRepository outboxRepository;

    private static final Timestamp START = Timestamp.valueOf("2026-09-01 08:00:00");
    private static final Timestamp END = Timestamp.valueOf("2026-09-01 10:00:00");

    private static List<OtScheduleService.ProcedureRequest> procedures() {
        return List.of(
                new OtScheduleService.ProcedureRequest("OT-Main", "Primary procedure",
                        new BigDecimal("10000.00"), null, null),
                new OtScheduleService.ProcedureRequest("OT-Assist", "Dependent procedure",
                        null, 0, new BigDecimal("25.00")));
    }

    @Test
    @Order(1)
    void fullOtFlow_withExactOutboxContracts() throws Exception {
        // ---- BR-OT-001: neither admissionId nor encounterId -> BEOT004, no rows ------
        long eventsBefore = outboxRepository.count();
        assertThatThrownBy(() -> service.create(501L, null, null, "OT-1", START, END,
                7L, 9L, false, procedures()))
                .isInstanceOf(com.hims.ot.application.exceptions.InvalidOtRequestException.class)
                .extracting(e -> ((com.hims.ot.application.exceptions.InvalidOtRequestException) e).getErrorCode())
                .isEqualTo("BEOT004");
        assertThat(outboxRepository.count()).isEqualTo(eventsBefore);   // no event on failure

        // ---- create with encounterId: dependent-surgery discount + ProcedureScheduled -
        OtSchedule created = service.create(501L, 900L, null, "OT-TABLE-1", START, END,
                7L, 9L, true, procedures());
        assertThat(created.getStatus()).isEqualTo(OtSchedule.STATUS_PLANNED);
        assertThat(created.getPreBilling()).isTrue();                    // BR-OT-002 persisted

        var details = created.getDetails();
        assertThat(details).hasSize(2);
        assertThat(details.get(0).getRate()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(details.get(0).getDiscountPct()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(details.get(1).getRate()).isEqualByComparingTo(BigDecimal.ZERO);          // default 0
        assertThat(details.get(1).getDiscountPct()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(details.get(1).getDependentOf()).isEqualTo(details.get(0).getDetailId()); // sibling FK
        assertThat(details.get(1).getNetRate()).isEqualByComparingTo(BigDecimal.ZERO);

        OutboxEvent scheduled = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("ProcedureScheduled"))
                .findFirst().orElseThrow();
        assertThat(scheduled.getVersion()).isEqualTo("v1");
        assertThat(scheduled.getAggregateType()).isEqualTo("schedule");
        assertThat(scheduled.getAggregateId()).isEqualTo(created.getScheduleId());
        assertThat(scheduled.getPartitionKey()).isEqualTo(String.valueOf(created.getPatientId()));  // patientId
        JsonNode ps = MAPPER.readTree(scheduled.getPayload());
        assertThat(ps.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("scheduleId", "patientId", "encounterId", "admissionId",
                        "otTable", "scheduledStart", "scheduledEnd", "surgeonId", "anesthetistId",
                        "preBilling", "procedures", "scheduledAt");
        assertThat(ps.get("preBilling").asBoolean()).isTrue();           // BR-OT-002 echoed in event
        assertThat(ps.get("procedures")).hasSize(2);
        assertThat(ps.get("procedures").get(1).get("dependentOf").asText())
                .isEqualTo(String.valueOf(details.get(0).getDetailId()));
        assertThat(ps.get("procedures").get(1).get("discountPct").decimalValue())
                .isEqualByComparingTo(new BigDecimal("25.00"));

        // ---- lifecycle guards --------------------------------------------------------
        // complete from PLANNED rejected
        assertThatThrownBy(() -> service.complete(created.getScheduleId()))
                .isInstanceOf(com.hims.ot.application.exceptions.InvalidOtRequestException.class);

        OtSchedule started = service.start(created.getScheduleId());
        assertThat(started.getStatus()).isEqualTo(OtSchedule.STATUS_IN_PROGRESS);
        // double start rejected
        assertThatThrownBy(() -> service.start(created.getScheduleId()))
                .isInstanceOf(com.hims.ot.application.exceptions.InvalidOtRequestException.class);

        // ---- complete -> DONE + SurgeryCompleted (PROPOSED) ---------------------------
        OtSchedule done = service.complete(created.getScheduleId());
        assertThat(done.getStatus()).isEqualTo(OtSchedule.STATUS_DONE);   // status=3

        OutboxEvent completed = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("SurgeryCompleted"))
                .findFirst().orElseThrow();
        assertThat(completed.getVersion()).isEqualTo("v1");
        assertThat(completed.getAggregateType()).isEqualTo("schedule");
        assertThat(completed.getAggregateId()).isEqualTo(created.getScheduleId());
        assertThat(completed.getPartitionKey()).isEqualTo(String.valueOf(created.getPatientId()));  // patientId
        JsonNode sc = MAPPER.readTree(completed.getPayload());
        assertThat(sc.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("scheduleId", "patientId", "encounterId", "admissionId",
                        "completedAt", "preBilling", "procedureCodes");
        assertThat(sc.get("procedureCodes").get(0).asText()).isEqualTo("OT-Main");

        // cancel after DONE rejected; DONE cannot be cancelled
        assertThatThrownBy(() -> service.cancel(created.getScheduleId()))
                .isInstanceOf(com.hims.ot.application.exceptions.InvalidOtRequestException.class);

        // ---- cancel path emits NO event ----------------------------------------------
        long beforeCancel = outboxRepository.count();
        OtSchedule cancelled = service.create(602L, null, 77L, "OT-TABLE-2",
                Timestamp.valueOf("2026-09-02 14:00:00"), null, 7L, 9L, false,
                List.of(new OtScheduleService.ProcedureRequest("OT-X", "X op",
                        new BigDecimal("500"), null, null)));
        service.cancel(cancelled.getScheduleId());
        assertThat(outboxRepository.count()).isEqualTo(beforeCancel + 1);  // only its ProcedureScheduled

        // ---- search worklist ?date=&status= ------------------------------------------
        assertThat(service.search(java.time.LocalDate.of(2026, 9, 1), null))
                .extracting(OtSchedule::getScheduleId)
                .containsExactly(created.getScheduleId());
        assertThat(service.search(null, (int) OtSchedule.STATUS_DONE))
                .extracting(OtSchedule::getScheduleId)
                .containsExactly(created.getScheduleId());
        assertThat(service.search(java.time.LocalDate.of(2026, 9, 2), (int) OtSchedule.STATUS_CANCELLED))
                .extracting(OtSchedule::getScheduleId)
                .containsExactly(cancelled.getScheduleId());

        // GET by id echoes pre_billing and details
        OtSchedule fetched = service.get(created.getScheduleId());
        assertThat(fetched.getPreBilling()).isTrue();
        assertThat(fetched.getDetails()).hasSize(2);

        // ---- producer stamped for relay topic routing (ihms.ot.events) ---------------
        assertThat(outboxRepository.findAll()).allSatisfy(e -> {
            assertThat(e.getProducerService()).isEqualTo("ot-service");
            assertThat(e.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        });
    }
}
