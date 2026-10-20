package com.hims.lab.infrastructure.persistence;

import com.hims.lab.LabServiceApplication;
import com.hims.lab.application.LabOrderService;
import com.hims.lab.application.MachineService;
import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.domain.model.MachineConfiguration;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
import com.hims.lab.infrastructure.outbox.OutboxEvent;
import com.hims.lab.infrastructure.outbox.OutboxRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-7 Lab service on real PG16: V1 schema applies (ihms_lab), machine config CRUD,
 * booking+details -> collect -> start -> upsert results -> finalize-per-detail, and the
 * EXACT outbox row for E-05 LabResultFinalized (type string 'LabResultFinalized',
 * schema token lab-result-finalized.v1 mapping, partition key encounterId,
 * payload keys exactly {bookingId, detailIds[], patientId, encounterId,
 * results[{parameterName,value,unit}], status, finalizedAt}) — emitted ONLY when the
 * WHOLE booking finalizes.
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = LabServiceApplication.class)
@DirtiesContext
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LabServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private LabOrderService labOrderService;

    @Autowired
    private MachineService machineService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @Order(1)
    void fullLabFlow_withExactOutboxContracts() throws Exception {
        // ---- machine config CRUD -------------------------------------------------
        MachineConfiguration machine = machineService.create("Sysmex XN-1000", "HL7", true);
        assertThat(machine.getMachineId()).isNotNull();
        assertThat(machineService.list(true)).extracting(MachineConfiguration::getMachineId)
                .contains(machine.getMachineId());
        MachineConfiguration updated = machineService.update(machine.getMachineId(),
                "Sysmex XN-1000 (lab-1)", "ASTM", null);
        assertThat(updated.getProtocol()).isEqualTo("ASTM");

        // ---- create booking with two test lines -----------------------------------
        PathOrderBooking booking = labOrderService.create(505L, 7777L, false, List.of(
                new LabOrderService.TestLine("CBC", "Complete Blood Count"),
                new LabOrderService.TestLine("LFT", "Liver Function Test")));
        long bookingId = booking.getBookingId();
        assertThat(booking.getBookingNo()).isEqualTo("LAB-" + bookingId);
        assertThat(booking.getBookingStatus()).isEqualTo(PathOrderBooking.STATUS_ORDERED);
        assertThat(booking.getDetails()).extracting(PathOrderDetail::getDetailStatus)
                .containsOnly(PathOrderBooking.STATUS_ORDERED);

        // ---- status machine: collect -> start --------------------------------------
        labOrderService.collect(bookingId);
        assertThat(labOrderService.get(bookingId).getBookingStatus())
                .isEqualTo(PathOrderBooking.STATUS_SAMPLE_COLLECTED);
        labOrderService.start(bookingId);
        assertThat(labOrderService.get(bookingId).getBookingStatus())
                .isEqualTo(PathOrderBooking.STATUS_IN_PROGRESS);

        // illegal: collect again -> 400 LAB_ILLEGAL_TRANSITION
        assertThatThrownBy(() -> labOrderService.collect(bookingId))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");

        // ---- result upsert on detail 1 ---------------------------------------------
        PathOrderDetail detail1 = booking.getDetails().get(0);
        PathOrderDetail detail2 = booking.getDetails().get(1);
        labOrderService.upsertResults(bookingId, detail1.getDetailId(), List.of(
                new LabOrderService.ResultRow("Hb", "13.5", "g/dL", "12-16"),
                new LabOrderService.ResultRow("WBC", "7.2", "10^3/uL", "4-11")));
        // re-entry updates the existing row in place (upsert keyed parameter_name)
        labOrderService.upsertResults(bookingId, detail1.getDetailId(), List.of(
                new LabOrderService.ResultRow("Hb", "13.9", "g/dL", "12-16"),
                new LabOrderService.ResultRow("WBC", "7.2", "10^3/uL", "4-11")));
        var params1 = labOrderService.get(bookingId).getDetails().get(0).getParameters();
        assertThat(params1).hasSize(2);
        assertThat(params1.stream().filter(p -> p.getParameterName().equalsIgnoreCase("hb"))
                .findFirst().orElseThrow().getValue()).isEqualTo("13.9");

        // ---- finalize detail 1: NOT all details final -> NO E-05 yet ---------------
        long outboxBefore = outboxRepository.count();
        labOrderService.finalizeDetail(bookingId, detail1.getDetailId());
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore);   // no event per-detail
        assertThat(labOrderService.get(bookingId).getBookingStatus())
                .isEqualTo(PathOrderBooking.STATUS_IN_PROGRESS);        // header not yet final

        // results of a finalized detail are locked
        assertThatThrownBy(() -> labOrderService.upsertResults(bookingId, detail1.getDetailId(),
                List.of(new LabOrderService.ResultRow("Hb", "1", "g/dL", null))))
                .isInstanceOf(InvalidLabRequestException.class)
                .hasMessageContaining("locked");

        // ---- finalize detail 2: whole booking finalizes -> EXACTLY ONE E-05 --------
        labOrderService.upsertResults(bookingId, detail2.getDetailId(), List.of(
                new LabOrderService.ResultRow("Bilirubin", "0.8", "mg/dL", "0.2-1.2")));
        PathOrderBooking finalized = labOrderService.finalizeDetail(bookingId, detail2.getDetailId());
        assertThat(finalized.getBookingStatus()).isEqualTo(PathOrderBooking.STATUS_FINALIZED);
        assertThat(finalized.getFinalizedAt()).isNotNull();

        var rows = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("LabResultFinalized")).toList();
        assertThat(rows).hasSize(1);   // emitted exactly once for the whole booking

        OutboxEvent e05 = rows.get(0);
        assertThat(e05.getVersion()).isEqualTo("v1");
        assertThat(e05.getAggregateType()).isEqualTo("labOrder");
        assertThat(e05.getAggregateId()).isEqualTo(bookingId);
        assertThat(e05.getPartitionKey()).isEqualTo("7777");   // partition key encounterId
        assertThat(e05.getProducerService()).isEqualTo("lab-service");
        assertThat(e05.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(e05.getIdempotencyKey()).isEqualTo("lab-result-finalized:" + bookingId);

        JsonNode payload = MAPPER.readTree(e05.getPayload());
        assertThat(payload.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("bookingId", "detailIds", "patientId",
                        "encounterId", "results", "status", "finalizedAt");
        assertThat(payload.get("bookingId").asText()).isEqualTo(String.valueOf(bookingId));
        assertThat(payload.get("patientId").asText()).isEqualTo("505");
        assertThat(payload.get("encounterId").asText()).isEqualTo("7777");
        assertThat(payload.get("detailIds"))
                .extracting(JsonNode::asText)
                .containsExactly(String.valueOf(detail1.getDetailId()), String.valueOf(detail2.getDetailId()));
        assertThat(payload.get("results")).hasSize(3);
        assertThat(payload.get("results").get(0).fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("parameterName", "value", "unit");
        assertThat(payload.get("status").asText()).isEqualTo("FINALIZED");
        assertThat(payload.get("finalizedAt").asText()).isNotBlank();

        // all parameter rows of the booking carry is_final=true + finalized_at
        var allParams = labOrderService.get(bookingId).getDetails().stream()
                .flatMap(d -> d.getParameters().stream()).toList();
        assertThat(allParams).allSatisfy(p -> {
            assertThat(p.getIsFinal()).isTrue();
            assertThat(p.getFinalizedAt()).isNotNull();
        });

        // ---- post-finalize guards ---------------------------------------------------
        assertThatThrownBy(() -> labOrderService.finalizeDetail(bookingId, detail2.getDetailId()))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
        assertThatThrownBy(() -> labOrderService.cancel(bookingId))
                .isInstanceOf(InvalidLabRequestException.class)
                .extracting(e -> ((InvalidLabRequestException) e).getErrorCode())
                .isEqualTo("LAB_ILLEGAL_TRANSITION");
        assertThat(outboxRepository.count()).isEqualTo(outboxBefore + 1);   // guards emitted nothing

        // ---- worklist ---------------------------------------------------------------
        assertThat(labOrderService.worklist((short) PathOrderBooking.STATUS_FINALIZED, null))
                .extracting(PathOrderBooking::getBookingId)
                .containsExactly(bookingId);
        assertThat(labOrderService.worklist(null, 999L)).isEmpty();
        assertThat(labOrderService.worklist((short) PathOrderBooking.STATUS_FINALIZED, 505L))
                .extracting(PathOrderBooking::getBookingId)
                .containsExactly(bookingId);
    }
}
