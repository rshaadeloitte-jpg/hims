package com.hims.adt.infrastructure.persistence;

import com.hims.adt.AdtServiceApplication;
import com.hims.adt.application.AdmissionService;
import com.hims.adt.application.exceptions.ActiveAdmissionExistsException;
import com.hims.adt.application.exceptions.BedUnavailableException;
import com.hims.adt.domain.model.Admission;
import com.hims.adt.domain.model.Bed;
import com.hims.adt.infrastructure.outbox.OutboxEvent;
import com.hims.adt.infrastructure.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-032 on real PostgreSQL 16 + Kafka: V1 schema applies (ihms_adt incl.
 * §A5 seeds + CLEANING row), BR-IPD-001 ipdNo template, BR-IPD-002 CAS claims
 * under parallel contention (exactly-one-winner per bed, exactly-one ACTIVE
 * admission per patient via uq_t_admission_active), transfer CLEANING +
 * transfer log, multi-department clearance gate (BR-DIS-001), abscond sweep
 * (BR-IPD-005), bed reservations, and the outbox → ihms.adt.lifecycle
 * roundtrip with all four lifecycle event types.
 *
 * NO class-level @Transactional (IMP-DEC-038 lesson): cleanup auto-commits per
 * statement; sequences persist across tests.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AdtServiceApplication.class)
class AdtServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired private AdmissionService admissionService;
    @Autowired private com.hims.adt.application.MasterDataService masterDataService;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM ihms_adt.outbox");
        jdbc.update("DELETE FROM ihms_adt.r_clearance_checklist");
        jdbc.update("DELETE FROM ihms_adt.t_bed_reservation");
        jdbc.update("DELETE FROM ihms_adt.t_bed_transfer_log");
        jdbc.update("DELETE FROM ihms_adt.t_admission_detail");
        jdbc.update("DELETE FROM ihms_adt.t_admission");
        jdbc.update("DELETE FROM ihms_adt.m_bed");
        jdbc.update("DELETE FROM ihms_adt.m_ward");
    }

    // ------------------------------------------------------------------ helpers

    private long newWard(String name) {
        return masterDataService.createWard(1L, name, "GENERAL", true).getWardId();
    }

    private Bed freeBed(long wardId, String bedNo) {
        return masterDataService.createBed(bedNo, wardId, "GENERAL", "B1", null,
                false, false, false, false, 1L);
    }

    /** Clear all four checklist rows so discharge may proceed. */
    private void clearAllDepartments(long admissionId) {
        for (String dept : List.of("NURSING", "PHARMACY", "BILLING", "LAB")) {
            admissionService.clearDepartment(admissionId, dept, 42L, null);
        }
    }

    // ------------------------------------------------------------------ flow

    @Test
    void fullAdtFlow_withExactOutboxContracts() throws Exception {
        // ---- masters + §A5 seeds --------------------------------------------------
        long wardA = newWard("General Medicine");
        long wardB = newWard("ICU");
        assertThat(masterDataService.availableBeds(null)).isEmpty();

        Bed g1 = freeBed(wardA, "G-101");
        Bed g2 = freeBed(wardA, "G-102");
        Bed icu = freeBed(wardB, "I-201");

        var statusMaster = jdbc.queryForMap(
                "SELECT isrelease FROM ihms_adt.m_bed_status_master WHERE bed_status_id=6");
        assertThat(statusMaster.get("isrelease")).isEqualTo(true);   // BR-DIS-003 release allocatable again

        // ---- admission with AUTO allocation ---------------------------------------
        Admission admitted = admissionService.admit(501L, 900L, 11L, "ROUTINE", 1L, null,
                false, null, null, null, null);
        assertThat(admitted.getBedId()).isNotNull();
        int yy = java.time.Year.now().getValue() % 100;
        assertThat(admitted.getIpdNo())   // BR-IPD-001 template
                .isEqualTo("A/IHMS/%d/%05d".formatted(yy, admitted.getAdmissionId()));
        assertThat(jdbc.queryForObject(
                "SELECT occupied FROM ihms_adt.m_bed WHERE bed_id=?", Boolean.class,
                admitted.getBedId())).isTrue();

        // duplicate active admission rejected (BERB013)
        long eventsBefore = outboxRepository.count();
        assertThatThrownBy(() -> admissionService.admit(501L, 900L, 11L, "ROUTINE", 1L, null,
                false, null, null, null, null))
                .isInstanceOf(ActiveAdmissionExistsException.class);
        assertThat(outboxRepository.count()).isEqualTo(eventsBefore);   // no events on failure

        // ---- exact E-03 PatientAdmitted row ---------------------------------------
        OutboxEvent e03 = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("PatientAdmitted")).findFirst().orElseThrow();
        assertThat(e03.getVersion()).isEqualTo("v1");
        assertThat(e03.getAggregateType()).isEqualTo("admission");
        assertThat(e03.getPartitionKey()).isEqualTo(String.valueOf(admitted.getPatientId()));
        JsonNode p03 = MAPPER.readTree(e03.getPayload());
        assertThat(p03.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "admissionId", "patientId", "encounterId", "bedId", "wardId", "unitId",
                "admissionType", "ipdNo", "doctorId", "admittedAt");
        assertThat(p03.get("ipdNo").asText()).isEqualTo(admitted.getIpdNo());
        assertThat(p03.get("encounterId").asText()).isEqualTo("900");

        // ---- explicit-bed admission onto the OCCUPIED bed -> BERB005 ---------------
        assertThatThrownBy(() -> admissionService.admit(601L, null, 12L, "EMERGENCY", 1L,
                admitted.getBedId(), false, null, null, null, null))
                .isInstanceOf(com.hims.adt.application.exceptions.InvalidAdtRequestException.class)
                .extracting(e -> ((com.hims.adt.application.exceptions.InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("BERB005");   // status 5 -> isrelease=false

        Admission admitted2 = admissionService.admit(601L, null, 12L, "EMERGENCY", 1L, null,
                false, null, null, null, null);
        assertThat(admitted2.getBedId()).isNotEqualTo(admitted.getBedId());

        // ---- transfer to the ICU bed -----------------------------------------------
        Admission transferred = admissionService.transfer(admitted.getId(), icu.getBedId(), "step-up care");
        assertThat(transferred.getBedId()).isEqualTo(icu.getBedId());
        assertThat(transferred.getWardId()).isEqualTo(wardB);

        Bed vacated = jdbc.queryForObject(
                "SELECT bed_id, occupied, bed_status_id FROM ihms_adt.m_bed WHERE bed_id=?",
                (rs, i) -> { Bed b = new Bed(); b.setBedId(rs.getLong(1));
                    b.setOccupied(rs.getBoolean(2)); b.setBedStatusId(rs.getShort(3)); return b; },
                g1.getBedId());
        assertThat(vacated.getOccupied()).isFalse();
        assertThat(vacated.getBedStatusId()).isEqualTo((short) 2);   // CLEANING, NOT claimable

        Map<String, Object> logRow = jdbc.queryForMap(
                "SELECT from_bed_id, to_bed_id, from_ward_id, to_ward_id, reason "
                        + "FROM ihms_adt.t_bed_transfer_log WHERE admission_id=?", admitted.getId());
        assertThat(logRow.get("from_bed_id")).isEqualTo(g1.getBedId());
        assertThat(logRow.get("to_bed_id")).isEqualTo(icu.getBedId());
        assertThat(logRow.get("reason")).isEqualTo("step-up care");

        // cleaning-complete flips CLEANING back to FREE (claimable again)
        admissionService.markCleaningComplete(g1.getBedId());
        assertThat(jdbc.queryForObject(
                "SELECT bed_status_id FROM ihms_adt.m_bed WHERE bed_id=?", Short.class,
                g1.getBedId())).isEqualTo((short) 1);

        // ---- clearance gate (BR-DIS-001) -------------------------------------------
        assertThatThrownBy(() -> admissionService.discharge(transferred.getId(), "NORMAL", false, null, null))
                .isInstanceOf(com.hims.adt.application.exceptions.InvalidAdtRequestException.class)
                .extracting(e -> ((com.hims.adt.application.exceptions.InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_CLEARANCE_PENDING");

        var rows = admissionService.clearances(transferred.getId());
        assertThat(rows).hasSize(4);
        assertThat(rows).allMatch(com.hims.adt.domain.model.ClearanceChecklistItem::pending);
        clearAllDepartments(transferred.getId());

        // ---- discharge (BR-DIS-003) -------------------------------------------------
        Admission discharged = admissionService.discharge(transferred.getId(), "NORMAL", false, null, null);
        assertThat(discharged.getDischargeDate()).isNotNull();

        Map<String, Object> released = jdbc.queryForMap(
                "SELECT occupied, bed_status_id FROM ihms_adt.m_bed WHERE bed_id=?", icu.getBedId());
        assertThat(released.get("occupied")).isEqualTo(false);
        assertThat(((Number) released.get("bed_status_id")).shortValue()).isEqualTo((short) 6);   // pinned literal

        // re-admission onto the released bed works (status 6 isrelease=true)
        Admission readmitted = admissionService.admit(701L, null, 13L, "ROUTINE", 2L, icu.getBedId(),
                false, null, null, null, null);
        assertThat(readmitted.getBedId()).isEqualTo(icu.getBedId());

        // double discharge rejected
        assertThatThrownBy(() -> admissionService.discharge(transferred.getId(), "NORMAL", false, null, null))
                .isInstanceOf(com.hims.adt.application.exceptions.InvalidAdtRequestException.class);

        // ---- exact E-04 PatientDischarged row --------------------------------------
        OutboxEvent e04 = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("PatientDischarged"))
                .filter(e -> e.getPartitionKey().equals(String.valueOf(discharged.getPatientId())))
                .findFirst().orElseThrow();   // aggregateId is the dischargeId, not the admissionId
        assertThat(e04.getPartitionKey()).isEqualTo(String.valueOf(discharged.getPatientId()));
        JsonNode p04 = MAPPER.readTree(e04.getPayload());
        assertThat(p04.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "dischargeId", "patientId", "admissionId", "encounterId",
                "dischargeType", "isExpired", "deathDate", "date", "balanceDue");
        assertThat(p04.get("balanceDue").isNull()).isTrue();
        assertThat(p04.get("encounterId").asText()).isEqualTo("900");

        // ---- final inventory: all four lifecycle types present ----------------------
        var allEvents = outboxRepository.findAll();
        assertThat(allEvents).extracting(OutboxEvent::getEventType)
                .contains("PatientAdmitted", "PatientDischarged", "BedTransferred", "BedStatusChanged");

        // BedTransferred keyed by PATIENT id; both BedStatusChanged keyed by BED id
        assertThat(allEvents.stream().filter(e -> e.getEventType().equals("BedTransferred")))
                .allSatisfy(e -> assertThat(e.getPartitionKey()).isEqualTo("501"));
        var statusChangedKeys = allEvents.stream()
                .filter(e -> e.getEventType().equals("BedStatusChanged"))
                .map(OutboxEvent::getPartitionKey).collect(Collectors.toSet());
        assertThat(statusChangedKeys).containsExactlyInAnyOrder(
                String.valueOf(icu.getBedId()), String.valueOf(g1.getBedId()));

        assertThat(allEvents).allSatisfy(e -> {
            assertThat(e.getProducerService()).isEqualTo("adt-service");
            assertThat(e.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        });

        assertThat(admissionService.search(501L, true)).isEmpty();   // discharged -> inactive
        assertThat(admissionService.search(501L, false))
                .extracting(Admission::getAdmissionId)
                .containsExactly(admitted.getAdmissionId());
    }

    // ------------------------------------------------------------------ BR-IPD-002 concurrency

    @Test
    void parallelBedClaim_exactlyOneWinnerPerBed_BR_IPD_002() throws Exception {
        long wardId = newWard("Race Ward");
        Bed b1 = freeBed(wardId, "R-1");
        Bed b2 = freeBed(wardId, "R-2");

        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<java.util.concurrent.Future<Object>> futures = new ArrayList<>();
            for (int p = 0; p < 6; p++) {
                final long patientId = 700L + p;
                futures.add(pool.submit(() ->
                        (Object) admissionService.admit(patientId, null, 11L, "ROUTINE", 1L, null,
                                false, null, null, null, null)));
            }
            int ok = 0;
            int failed = 0;
            Set<Long> claimedBeds = new java.util.HashSet<>();
            for (var f : futures) {
                try {
                    Admission a = (Admission) f.get(30, TimeUnit.SECONDS);
                    ok++;
                    assertThat(claimedBeds.add(a.getBedId()))
                            .as("no double occupancy of bed %s", a.getBedId()).isTrue();
                } catch (java.util.concurrent.ExecutionException e) {
                    failed++;
                    assertThat(e.getCause()).isInstanceOf(BedUnavailableException.class);   // BEXB001
                }
            }
            assertThat(ok).isEqualTo(2);      // only as many as free beds
            assertThat(failed).isEqualTo(4);

            Integer occupied = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ihms_adt.m_bed WHERE occupied", Integer.class);
            assertThat(occupied).isEqualTo(2);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentAdmitSamePatient_exactlyOneActiveAdmission_uqConstraint() throws Exception {
        long wardId = newWard("Dup Ward");
        freeBed(wardId, "D-1");
        freeBed(wardId, "D-2");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(() -> admissionService.admit(777L, null, 11L, "ROUTINE", 1L, null,
                    false, null, null, null, null));
            var f2 = pool.submit(() -> admissionService.admit(777L, null, 11L, "ROUTINE", 1L, null,
                    false, null, null, null, null));

            int okCount = 0;
            for (var f : List.of(f1, f2)) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    okCount++;
                } catch (java.util.concurrent.ExecutionException expected) {
                    // loser: BERB013 pre-check or the DB unique index — either way rejected
                }
            }
            assertThat(okCount).isEqualTo(1);
            Integer active = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ihms_adt.t_admission WHERE patient_id=777 AND is_cancel=false "
                            + "AND discharge_date IS NULL", Integer.class);
            assertThat(active).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ reservations

    @Test
    void reservation_blocksOtherPatients_isConsumedByOwner() {
        long wardId = newWard("Res Ward");
        Bed reserved = freeBed(wardId, "RES-1");
        Bed open = freeBed(wardId, "OPEN-1");

        Timestamp expires = Timestamp.from(java.time.Instant.now().plusSeconds(3600));
        var r = admissionService.reserve(reserved.getBedId(), 501L, expires);
        assertThat(r.getResStatus()).isEqualTo(com.hims.adt.domain.model.BedReservation.ResStatus.ACTIVE);

        // second ACTIVE reservation on the same bed -> ADT_BED_RESERVED (DB partial index too)
        assertThatThrownBy(() -> admissionService.reserve(reserved.getBedId(), 602L, expires))
                .isInstanceOf(com.hims.adt.application.exceptions.InvalidAdtRequestException.class)
                .extracting(e -> ((com.hims.adt.application.exceptions.InvalidAdtRequestException) e).getErrorCode())
                .isEqualTo("ADT_BED_RESERVED");

        // another patient's auto-allocation skips the reserved bed
        Admission other = admissionService.admit(602L, null, 11L, "ROUTINE", 1L, null,
                false, null, null, null, null);
        assertThat(other.getBedId()).isEqualTo(open.getBedId());

        // owner's auto-allocation lands on her reserved bed and CONSUMES it
        Admission owner = admissionService.admit(501L, null, 11L, "ROUTINE", 1L, null,
                false, null, null, null, null);
        assertThat(owner.getBedId()).isEqualTo(reserved.getBedId());
        assertThat(jdbc.queryForObject(
                "SELECT res_status FROM ihms_adt.t_bed_reservation WHERE reservation_id=?", String.class,
                r.getReservationId())).isEqualTo("CONSUMED");
    }

    // ------------------------------------------------------------------ abscond sweep

    @Test
    void abscondSweep_autoAbscondsStaleInterimAdmissions_BR_IPD_005() {
        Timestamp old = Timestamp.from(java.time.Instant.now().minus(Duration.ofDays(4)));
        jdbc.update("INSERT INTO ihms_adt.t_admission (admission_id, patient_id, doctor_id, "
                        + "ipd_no, admitted_at, is_cancel) VALUES (9001, 888, 11, ?, ?, false)",
                "A/IHMS/" + (java.time.Year.now().getValue() % 100) + "/90001", old);

        Timestamp cutoff = Timestamp.from(java.time.Instant.now().minus(Duration.ofDays(3)));
        List<Admission> swept = admissionService.sweepAbsconds(cutoff);

        assertThat(swept).extracting(Admission::getAdmissionId).containsExactly(9001L);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT abscond_datetime, discharge_type, is_cancel FROM ihms_adt.t_admission WHERE admission_id=9001");
        assertThat(row.get("abscond_datetime")).isNotNull();
        assertThat(row.get("discharge_type")).isEqualTo("AUTO_ABSCONDED");
        assertThat(row.get("is_cancel")).isEqualTo(true);

        // sweep emitted PatientDischarged keyed by patientId
        Integer events = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_adt.outbox WHERE event_type='PatientDischarged' "
                        + "AND partition_key='888'", Integer.class);
        assertThat(events).isEqualTo(1);
    }

    // ------------------------------------------------------------------ Kafka roundtrip

    @Test
    void outboxRelay_publishesAllFourLifecycleEventsToIhmsAdtLifecycleTopic() throws Exception {
        long wardA = newWard("Kafka General");
        long wardB = newWard("Kafka ICU");
        Bed g1 = freeBed(wardA, "KG-1");
        Bed icu = freeBed(wardB, "KI-1");

        Admission admitted = admissionService.admit(909L, 9010L, 11L, "ROUTINE", 1L, g1.getBedId(),
                false, null, null, null, null);
        admissionService.transfer(admitted.getId(), icu.getBedId(), null);
        clearAllDepartments(admitted.getId());
        admissionService.discharge(admitted.getId(), "NORMAL", false, null, null);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "mig032-test-" + System.nanoTime(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()))) {
            consumer.subscribe(List.of("ihms.adt.lifecycle"));

            long deadline = System.currentTimeMillis() + 60_000;
            Set<String> seen = new java.util.HashSet<>();
            JsonNode admittedEnvelope = null;
            while (System.currentTimeMillis() < deadline && !seen.containsAll(Set.of(
                    "PatientAdmitted", "PatientDischarged", "BedTransferred", "BedStatusChanged"))) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (var r : records) {
                    seen.add(extractEventType(r.value()));
                    if ("PatientAdmitted".equals(extractEventType(r.value()))) {
                        admittedEnvelope = MAPPER.readTree(r.value());
                    }
                }
            }
            assertThat(seen).contains("PatientAdmitted", "PatientDischarged",
                    "BedTransferred", "BedStatusChanged");

            // envelope contract: partition key = record key = patientId for PatientAdmitted
            assertThat(admittedEnvelope).isNotNull();
            assertThat(admittedEnvelope.get("partitionKey").asText()).isEqualTo("909");
            assertThat(admittedEnvelope.get("schemaName").asText()).isEqualTo("patient-admitted.v1");
            assertThat(admittedEnvelope.get("payload").get("ipdNo").asText())
                    .isEqualTo(admitted.getIpdNo());
        }

        Integer unpublished = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_adt.outbox WHERE status <> 'PUBLISHED'", Integer.class);
        assertThat(unpublished).isEqualTo(0);
    }

    private static String extractEventType(String envelopeJson) {
        try {
            int idx = envelopeJson.indexOf("\"eventType\"");
            if (idx < 0) return "";
            int colon = envelopeJson.indexOf(':', idx);
            int openQuote = envelopeJson.indexOf('"', colon);
            int closeQuote = envelopeJson.indexOf('"', openQuote + 1);
            return envelopeJson.substring(openQuote + 1, closeQuote);
        } catch (Exception e) {
            return "";
        }
    }
}
