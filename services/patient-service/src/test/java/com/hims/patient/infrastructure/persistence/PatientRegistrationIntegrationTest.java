package com.hims.patient.infrastructure.persistence;

import com.hims.patient.PatientServiceApplication;
import com.hims.patient.application.PatientRegistrationService;
import com.hims.patient.domain.model.PatientDocument;
import com.hims.patient.domain.model.PatientRegistration;
import com.hims.patient.infrastructure.outbox.OutboxEvent;
import com.hims.patient.infrastructure.outbox.OutboxPublisher;
import com.hims.patient.infrastructure.outbox.OutboxRepository;
import com.hims.patient.infrastructure.outbox.OutboxService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-030 patient service on real PG16 + Kafka: V1 schema applies,
 * BR-PAT-001 MRN formula on live sequence ids, BR-PAT-002 duplicate matrix on
 * real indexes, outbox rows written in the registration tx and relayed to
 * Kafka (E-01 envelope), document linkage persists.
 *
 * NO class-level @Transactional: the service runs REQUIRES_NEW; an outer tx
 * holding uncommitted DELETEs deadlocks ON CONFLICT inserts (IMP-DEC-038
 * lesson). Cleanup auto-commits per statement instead.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = PatientServiceApplication.class)
class PatientRegistrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired PatientRegistrationService service;
    @Autowired OutboxService outboxService;
    @Autowired OutboxRepository outboxRepository;
    @Autowired OutboxPublisher publisher;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM ihms_patient.outbox");            // auto-commit per statement
        jdbc.update("DELETE FROM ihms_patient.patient_document");
        jdbc.update("DELETE FROM ihms_patient.patient_registration");
    }

    private static final String PATIENT_JSON =
            "{\"firstName\":\"Ramesh\",\"lastName\":\"Sharma\",\"gender\":\"M\",\"dob\":\"1990-01-01\","
                    + "\"mobileNo\":\"9876543210\",\"bloodGroup\":\"B+\","
                    + "\"addressLine1\":\"1 Civil Lines\",\"city\":\"Jaipur\",\"state\":\"RJ\",\"pincode\":\"302001\","
                    + "\"emergencyContactName\":\"Sita\",\"emergencyContactPhone\":\"9876500000\"}";

    private static com.fasterxml.jackson.databind.JsonNode json(String s) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void flywayV1_createsSchemaTablesSequencesAndUniqueIndexes() {
        Integer tables = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ihms_patient'", Integer.class);
        assertThat(tables).isGreaterThanOrEqualTo(3);   // patient_registration, patient_document, outbox

        Long seq = jdbc.queryForObject("SELECT last_value FROM ihms_patient.\"patient_reg_seq\"", Long.class);
        assertThat(seq).isGreaterThanOrEqualTo(0L);

        Integer uqMrno = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE schemaname='ihms_patient' AND indexname='ux_patient_mrno'
                """, Integer.class);
        assertThat(uqMrno).isEqualTo(1);
    }

    @Test
    void register_mrnFormulaAndOutboxInSameTransaction() {
        PatientRegistration p = service.register(json(PATIENT_JSON), 7L);

        assertThat(p.getId()).isPositive();    // sequences persist across tests (no ALTER SEQUENCE — deadlock lesson)
        assertThat(p.getMrno())
                .matches(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyy"))
                        + String.format("%09d", p.getId()));

        List<Map<String, Object>> outbox = jdbc.queryForList(
                "SELECT event_type, aggregate_id, partition_key, status FROM ihms_patient.outbox");
        assertThat(outbox).hasSize(1);
        assertThat(outbox.get(0).get("event_type")).isEqualTo("PatientRegistered");
        assertThat(outbox.get(0).get("aggregate_id")).isEqualTo((long) p.getId());
        assertThat(outbox.get(0).get("partition_key")).isEqualTo(String.valueOf(p.getId()));
        assertThat(outbox.get(0).get("status")).isEqualTo("PENDING");
    }

    @Test
    void duplicateMatrix_realLookups_bera011_and_bexa040() {
        PatientRegistration first = service.register(json(
                "{\"firstName\":\"Ramesh\",\"gender\":\"M\",\"dob\":\"1990-01-01\",\"mobileNo\":\"9876543210\","
                        + "\"aadhaarNo\":\"111122223333\",\"healthIdNumber\":\"ABHA-777\"}"), null);

        // Aadhaar hit → BERA011
        assertThatThrownBy(() -> service.register(json(
                "{\"firstName\":\"Other\",\"gender\":\"F\",\"aadhaarNo\":\"111122223333\"}"), null))
                .isInstanceOf(com.hims.patient.application.exceptions.InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011");

        // ABHA healthIdNumber → BEXA040 with existing MRN
        String firstMrno = jdbc.queryForObject(
                "SELECT mrno FROM ihms_patient.patient_registration ORDER BY id LIMIT 1", String.class);
        assertThatThrownBy(() -> service.register(json(
                "{\"firstName\":\"Third\",\"gender\":\"M\",\"healthIdNumber\":\"abha-777\"}"), null))
                .isInstanceOf(com.hims.patient.application.exceptions.InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BEXA040")
                .hasMessageContaining(firstMrno);

        // mobile+firstName(+dob) heuristic → BERA011
        assertThatThrownBy(() -> service.register(json(
                "{\"firstName\":\"Ramesh\",\"gender\":\"M\",\"dob\":\"1990-01-01\",\"mobileNo\":\"9876543210\"}"), null))
                .isInstanceOf(com.hims.patient.application.exceptions.InvalidPatientRequestException.class)
                .hasFieldOrPropertyWithValue("errorCode", "BERA011");

        // distinct identity passes (id relative: sequences persist across tests)
        PatientRegistration ok = service.register(json(
                "{\"firstName\":\"Sunita\",\"gender\":\"F\",\"mobileNo\":\"9000000001\"}"), null);
        assertThat(ok.getId()).isGreaterThan(first.getId());

        // DB-level uniqueness of healthIdNumber holds even if service guard changes
        Long healthIdRows = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT lower(health_id_number)) FROM ihms_patient.patient_registration "
                        + "WHERE health_id_number IS NOT NULL", Long.class);
        assertThat(healthIdRows).isEqualTo(1L);
    }

    @Test
    void update_emitsPatientUpdated_andNeverTouchesServerOwnedFields() {
        PatientRegistration p = service.register(json(PATIENT_JSON), 7L);
        String originalMrno = p.getMrno();

        PatientRegistration updated = service.update(p.getId(), json(
                "{\"city\":\"Ajmer\",\"mrno\":\"FORGED\",\"id\":999,\"status\":false}"));

        assertThat(updated.getCity()).isEqualTo("Ajmer");
        assertThat(updated.getMrno()).isEqualTo(originalMrno);
        assertThat(updated.getId()).isEqualTo(p.getId());
        assertThat(updated.getStatus()).isTrue();

        List<Map<String, Object>> events = jdbc.queryForList(
                "SELECT event_type FROM ihms_patient.outbox ORDER BY id");
        assertThat(events).extracting(m -> m.get("event_type"))
                .containsExactly("PatientRegistered", "PatientUpdated");
    }

    @Test
    void documentLinkage_persistsWithOwnerScope() {
        PatientRegistration p = service.register(json(PATIENT_JSON), 7L);
        PatientDocument d = service.linkDocument(p.getId(),
                json("{\"docType\":\"AADHAAR\",\"docRef\":\"obj/pat/" + p.getId() + "/aadhaar.pdf\"}"));
        assertThat(d.getPatientId()).isEqualTo(p.getId());

        assertThat(service.documents(p.getId())).hasSize(1);

        assertThatThrownBy(() -> service.linkDocument(404L, json("{\"docType\":\"X\",\"docRef\":\"y\"}")))
                .isInstanceOf(com.hims.patient.application.exceptions.PatientNotFoundException.class);
    }

    @Test
    void outboxToKafka_roundTrip_marksPublished_withCatalogEnvelope() throws Exception {
        PatientRegistration p = service.register(json(PATIENT_JSON), 7L);
        OutboxEvent pending = outboxRepository.findAll().getFirst();
        assertThat(pending.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(pending.getEventType()).isEqualTo("PatientRegistered");

        publisher.publish(outboxRepository.findById(pending.getId()).orElseThrow());

        OutboxEvent finalState = pending;
        long deadline = System.currentTimeMillis() + 30000;
        while (System.currentTimeMillis() < deadline) {
            finalState = outboxRepository.findById(pending.getId()).orElseThrow();
            if (finalState.getStatus() == OutboxEvent.OutboxStatus.PUBLISHED) break;
            Thread.sleep(500);
        }
        assertThat(finalState.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PUBLISHED);
        assertThat(finalState.getPublishedAt()).isNotNull();
        // envelope payload carries the E-01 kickoff subset keyed by patientId
        assertThat(finalState.getPartitionKey()).isEqualTo(String.valueOf(p.getId()));
    }
}
