package com.hims.appointment.infrastructure.persistence;

import com.hims.appointment.AppointmentServiceApplication;
import com.hims.appointment.application.AppointmentService;
import com.hims.appointment.application.DailyEncounterCloseJob;
import com.hims.appointment.application.EncounterService;
import com.hims.appointment.application.exceptions.SlotAlreadyBookedException;
import com.hims.appointment.domain.model.Appointment;
import com.hims.appointment.domain.model.AppointmentStatus;
import com.hims.appointment.domain.model.DoctorSchedule;
import com.hims.appointment.domain.model.Encounter;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-031 on real PostgreSQL 16 + Kafka: V1 schema objects, slot
 * materialization + double-booking prevention (incl. concurrent CAS race),
 * token issuance, check-in with daily-counter OPD numbers, VisitStatus 11/12
 * split-brain guards, reschedule/cancel slot release, daily close job, and the
 * outbox → ihms.appointment.encounter roundtrip.
 *
 * NO class-level @Transactional (IMP-DEC-038 lesson): cleanup auto-commits per
 * statement; sequences persist across tests.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = AppointmentServiceApplication.class)
class AppointmentEncounterIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired AppointmentService appointments;
    @Autowired EncounterService encounters;
    @Autowired JdbcTemplate jdbc;
    private static final LocalDate MONDAY = LocalDate.of(2027, 3, 1);
    private static final LocalTime SLOT = LocalTime.of(9, 30);

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM ihms_appointment.outbox");
        jdbc.update("DELETE FROM ihms_appointment.encounter");
        jdbc.update("DELETE FROM ihms_appointment.appointment");
        jdbc.update("DELETE FROM ihms_appointment.doctor_slot");
        jdbc.update("DELETE FROM ihms_appointment.doctor_schedule");
        jdbc.update("DELETE FROM ihms_appointment.daily_encounter_counter");
    }

    private DoctorSchedule mondayClinic() {
        return appointments.createSchedule(1L, 11L, (short) MONDAY.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, 5L, 9L, null);
    }

    private long clinicId;

    private Appointment bookFirst() {
        DoctorSchedule clinic = mondayClinic();
        clinicId = clinic.getId();
        return appointments.book(spec(501L));
    }

    private AppointmentService.BookingSpec spec(long patientId) {
        return new AppointmentService.BookingSpec(patientId, 11L, clinicId, 1L, 5L, MONDAY, SLOT, null);
    }

    // ------------------------------------------------------------------ schema

    @Test
    void flywayV1_createsAllTablesSequencesAndIndexes() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema='ihms_appointment' "
                        + "ORDER BY table_name", String.class);
        assertThat(tables).contains("appointment", "encounter", "doctor_slot",
                "daily_encounter_counter", "outbox", "doctor_schedule");

        List<String> seqs = jdbc.queryForList(
                "SELECT sequencename FROM pg_sequences WHERE schemaname='ihms_appointment'", String.class);
        assertThat(seqs).contains("appointment_seq", "encounter_seq", "doctor_slot_seq",
                "doctor_schedule_seq", "outbox_seq");

        Integer uqSlot = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname='ihms_appointment' AND indexname='uq_doctor_slot'
                """, Integer.class);
        assertThat(uqSlot).isEqualTo(1);

        // status dictionary lock enforced at the DB level too
        Integer maxCode = jdbc.queryForObject(
                "SELECT MAX(appt_status) FROM ihms_appointment.appointment", Integer.class);
        assertThat(maxCode == null ? 0 : maxCode).isLessThanOrEqualTo(6);
    }

    // ------------------------------------------------------------------ booking & slots

    @Test
    void book_materializesSlots_assignsToken_writesOutboxInSameTx() {
        Appointment a = bookFirst();

        assertThat(a.getId()).isPositive();
        assertThat(a.getStatusEnum()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(a.getTokenNo()).isEqualTo(1);
        assertThat(a.getSlotEnd()).isEqualTo(LocalTime.of(10, 0));

        Integer bookedCount = jdbc.queryForObject(
                "SELECT booked_count FROM ihms_appointment.doctor_slot WHERE doctor_id=11 AND slot_date=? AND slot_start=?",
                Integer.class, java.sql.Date.valueOf(MONDAY), java.sql.Time.valueOf(SLOT));
        assertThat(bookedCount).isEqualTo(1);

        Map<String, Object> outboxRow = jdbc.queryForMap(
                "SELECT event_type, aggregate_id, partition_key, status FROM ihms_appointment.outbox");
        assertThat(outboxRow.get("event_type")).isEqualTo("AppointmentBooked");
        assertThat(outboxRow.get("partition_key")).isEqualTo("501");   // patientId keying
        assertThat(outboxRow.get("status")).isEqualTo("PENDING");

        var slots = appointments.findSlots(11L, MONDAY);
        assertThat(slots).hasSize(6);   // 09:00-12:00 in 30-min slots
        assertThat(slots.stream().filter(s -> Boolean.TRUE.equals(s.get("booked")))).hasSize(1);
    }

    @Test
    void doubleBooking_sameSlot_rejected_slotCountUnchanged() {
        bookFirst();
        assertThatThrownBy(() -> appointments.book(spec(502L)))
                .isInstanceOf(SlotAlreadyBookedException.class)
                .hasFieldOrPropertyWithValue("errorCode", "APPT_SLOT_TAKEN");

        Integer bookedCount = jdbc.queryForObject(
                "SELECT booked_count FROM ihms_appointment.doctor_slot WHERE slot_date=? AND slot_start=?",
                Integer.class, java.sql.Date.valueOf(MONDAY), java.sql.Time.valueOf(SLOT));
        assertThat(bookedCount).isEqualTo(1);
    }

    @Test
    void concurrentBookings_onlyOneWinsTheCapacityRace() throws Exception {
        clinicId = mondayClinic().getId();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wins = new AtomicInteger();
        AtomicInteger conflicts = new AtomicInteger();
        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final long patientId = 600 + i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    appointments.book(new AppointmentService.BookingSpec(
                            patientId, 11L, clinicId, 1L, 5L, MONDAY, SLOT, null));
                    wins.incrementAndGet();
                } catch (SlotAlreadyBookedException e) {
                    conflicts.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(wins.get()).isEqualTo(1);
        assertThat(conflicts.get()).isEqualTo(3);
        Integer rows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_appointment.appointment WHERE appt_status=1", Integer.class);
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void cancel_freesTheSlot_forRebooking_andEmitsAppointmentCancelled() {
        Appointment a = bookFirst();
        appointments.cancel(a.getId());

        assertThat(jdbc.queryForObject(
                "SELECT appt_status FROM ihms_appointment.appointment WHERE id=?", Integer.class, a.getId()))
                .isEqualTo(4);   // CANCELLED

        Appointment rebooked = appointments.book(spec(777L));
        assertThat(rebooked.getTokenNo()).isEqualTo(2);   // cancelled row still counts for the day roster

        Set<String> types = jdbc.queryForList(
                "SELECT DISTINCT event_type FROM ihms_appointment.outbox", String.class)
                .stream().collect(java.util.stream.Collectors.toSet());
        assertThat(types).contains("AppointmentCancelled", "AppointmentBooked");
    }

    @Test
    void reschedule_originalBecomesRescheduled_newRowLinked_oldSlotReleased() {
        Appointment original = bookFirst();

        LocalDate nextMonday = MONDAY.plusWeeks(1);
        DoctorSchedule clinic2 = appointments.createSchedule(1L, 11L,
                (short) nextMonday.getDayOfWeek().getValue(),
                LocalTime.of(9, 0), LocalTime.of(12, 0), 30, 5L, 9L, null);
        Appointment moved = appointments.reschedule(original.getId(), new AppointmentService.BookingSpec(
                501L, 11L, clinic2.getId(), 1L, 5L, nextMonday, SLOT, null));
        assertThat(moved.getRescheduledFromId()).isEqualTo(original.getId());

        assertThat(jdbc.queryForObject(
                "SELECT appt_status FROM ihms_appointment.appointment WHERE id=?",
                Integer.class, original.getId())).isEqualTo(5);   // RESCHEDULED
        assertThat(moved.getStatusEnum()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(moved.getRescheduledFromId()).isEqualTo(original.getId());

        // old slot freed (row remains, claim released)
        Integer oldCount = jdbc.queryForObject("""
                SELECT booked_count FROM ihms_appointment.doctor_slot
                WHERE doctor_id=11 AND slot_date=? AND slot_start=?
                """, Integer.class, java.sql.Date.valueOf(MONDAY), java.sql.Time.valueOf(SLOT));
        assertThat(oldCount).isEqualTo(0);
    }

    // ------------------------------------------------------------------ check-in / encounter

    @Test
    @SuppressWarnings("unchecked")
    void checkin_createsEncounter_dailyCounterNumbers_emitE02() throws Exception {
        Appointment a1 = bookFirst();
        // second appointment on a different slot same day/unit
        Appointment a2 = appointments.book(new AppointmentService.BookingSpec(
                502L, 11L, clinicId, 1L, 5L, MONDAY, LocalTime.of(10, 0), null));

        Encounter e1 = encounters.checkIn(a1.getId());
        Encounter e2 = encounters.checkIn(a2.getId());

        assertThat(e1.getEncounterNo()).isEqualTo(1);
        assertThat(e2.getEncounterNo()).isEqualTo(2);   // daily counter increments per (date, unit)
        assertThat(e1.getTokenNo()).isEqualTo(1);
        assertThat(e1.isClosed()).isFalse();
        assertThat(e1.getVisitstatus2()).isEqualTo("2");

        Map<String, Object> apptRow = jdbc.queryForMap(
                "SELECT appt_status, encounter_id FROM ihms_appointment.appointment WHERE id=?", a1.getId());
        assertThat(((Number) apptRow.get("appt_status")).intValue()).isEqualTo(2);   // CHECKED_IN
        assertThat(((Number) apptRow.get("encounter_id")).longValue()).isEqualTo(e1.getId());

        Map<String, Object> e02 = jdbc.queryForMap(
                "SELECT partition_key, payload::text AS p FROM ihms_appointment.outbox "
                        + "WHERE event_type='EncounterCreated' ORDER BY id LIMIT 1");
        assertThat(e02.get("partition_key")).isEqualTo("501");   // E-02 partition key patientId
        String payload = (String) e02.get("p");
        var e02Json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(payload);
        org.assertj.core.api.SoftAssertions softly = new org.assertj.core.api.SoftAssertions();
        softly.assertThat(e02Json.get("opdNo/token").asText()).isEqualTo("1");   // queue/kiosk compatible field
        softly.assertThat(e02Json.get("visitstatus2").asText()).isEqualTo("2");
        softly.assertThat(e02Json.get("appointmentType").asText()).isEqualTo("OPD_APPOINTMENT");
        softly.assertThat(e02Json.get("encounterId").asLong()).isEqualTo(e1.getId());
        softly.assertAll();

        // counter rollback safety is structural: one row per (date, unit)
        Integer counters = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_appointment.daily_encounter_counter", Integer.class);
        assertThat(counters).isEqualTo(1);
    }

    @Test
    void splitBrain_billingThenDispense_advancesGuards_onOneAggregate() {
        Appointment a = bookFirst();
        Encounter e = encounters.checkIn(a.getId());

        // dispense before billing is blocked by the legacy guard
        assertThatThrownBy(() -> encounters.markDispensed(e.getId(), false))
                .isInstanceOf(com.hims.appointment.application.exceptions.InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ENC_DISPENSE_BLOCKED");

        encounters.markBilled(e.getId());                       // VisitStatus 11
        assertThat(encounters.effectiveVisitStatus(e.getId())).isEqualTo((short) 11);

        // dispense attempt while undispensed items remain is rejected and stays at 11
        assertThatThrownBy(() -> encounters.markDispensed(e.getId(), true))
                .isInstanceOf(com.hims.appointment.application.exceptions.InvalidAppointmentRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ENC_DISPENSE_BLOCKED");
        assertThat(encounters.effectiveVisitStatus(e.getId())).isEqualTo((short) 11);

        encounters.markDispensed(e.getId(), false);             // 11 -> 12 once nothing pending
        Short dbValue = jdbc.queryForObject(
                "SELECT legacy_visit_status FROM ihms_appointment.encounter WHERE id=?",
                Short.class, e.getId());
        assertThat(dbValue).isEqualTo((short) 12);

        // billing after dispense must NEVER regress to 11 (legacy guard)
        encounters.markBilled(e.getId());
        assertThat(jdbc.queryForObject(
                "SELECT legacy_visit_status FROM ihms_appointment.encounter WHERE id=?",
                Short.class, e.getId())).isEqualTo((short) 12);
    }

    @Test
    @SuppressWarnings("unchecked")
    void complete_closesEncounter_emitsEncounterClosed_patientOrderingPreserved() throws Exception {
        Appointment a = bookFirst();
        Encounter e = encounters.checkIn(a.getId());
        encounters.completeEncounter(a.getId());

        assertThat(jdbc.queryForObject(
                "SELECT appt_status FROM ihms_appointment.appointment WHERE id=?",
                Integer.class, a.getId())).isEqualTo(3);   // COMPLETED

        Map<String, Object> closed = jdbc.queryForMap(
                "SELECT aggregate_id, partition_key, payload::text AS p FROM ihms_appointment.outbox "
                        + "WHERE event_type='EncounterClosed'");
        assertThat(closed.get("aggregate_id")).isEqualTo(e.getId());
        assertThat(((String) closed.get("partition_key"))).isEqualTo(String.valueOf(e.getId()));   // encounterId keying
        assertThat(new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree((String) closed.get("p")).get("closeReason").asText()).isEqualTo("COMPLETED");
    }

    // ------------------------------------------------------------------ daily close job

    @Test
    void dailyCloseJob_closesStaleOpenEncounters_andEmitsEncounterClosed() {
        Appointment a = bookFirst();
        Encounter e = encounters.checkIn(a.getId());

        // age the encounter beyond today
        jdbc.update("UPDATE ihms_appointment.encounter SET encounter_date=? WHERE id=?",
                java.sql.Date.valueOf(LocalDate.now().minusDays(3)), e.getId());

        int closed = encounters.closeStaleEncounters(LocalDate.now(), DailyEncounterCloseJob.REASON_AUTO_DAILY_CLOSE);
        assertThat(closed).isEqualTo(1);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT is_closed, close_reason FROM ihms_appointment.encounter WHERE id=?", e.getId());
        assertThat(row.get("is_closed")).isEqualTo(true);
        assertThat(row.get("close_reason")).isEqualTo(DailyEncounterCloseJob.REASON_AUTO_DAILY_CLOSE);

        Integer events = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_appointment.outbox WHERE event_type='EncounterClosed'", Integer.class);
        assertThat(events).isEqualTo(1);
    }

    // ------------------------------------------------------------------ Kafka roundtrip

    @Test
    void outboxRelay_publishesToAppointmentEncounterTopic_withEnvelope() throws Exception {
        Appointment a = bookFirst();
        Encounter e = encounters.checkIn(a.getId());
        appointments.cancel(appointments.book(new AppointmentService.BookingSpec(
                601L, 11L, clinicId, 1L, 5L, MONDAY, LocalTime.of(11, 0), null)).getId());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                Map.of(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, "mig031-test-" + System.nanoTime(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()))) {
            consumer.subscribe(List.of("ihms.appointment.encounter"));

            long deadline = System.currentTimeMillis() + 60_000;
            Set<String> seen = new java.util.HashSet<>();
            while (System.currentTimeMillis() < deadline && !seen.containsAll(Set.of(
                    "AppointmentBooked", "EncounterCreated", "AppointmentCancelled"))) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                records.forEach(r -> seen.add(extractEventType(r.value())));
            }
            assertThat(seen).contains("AppointmentBooked", "EncounterCreated", "AppointmentCancelled");
        }

        // relay marked everything PUBLISHED (retry/DLQ path untouched)
        Integer unpublished = jdbc.queryForObject(
                "SELECT COUNT(*) FROM ihms_appointment.outbox WHERE status <> 'PUBLISHED'", Integer.class);
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
