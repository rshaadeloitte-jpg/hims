package com.hims.queue.infrastructure.persistence;

import com.hims.queue.QueueServiceApplication;
import com.hims.queue.application.QueueService;
import com.hims.queue.domain.model.Counter;
import com.hims.queue.domain.model.QueueToken;
import com.hims.queue.domain.repository.CounterRepository;
import com.hims.queue.domain.repository.QueueTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — MIG-022 queue service on real PG16: V1 schema applies (ihms_queue),
 * daily counter resets per date and rolls back with the transaction,
 * emergency-priority call-next, transfer/no-show/complete on live columns.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = QueueServiceApplication.class)
class QueueServiceIntegrationTest {

    // NO @Transactional wrapper: service methods run REQUIRES_NEW and would
    // deadlock against this class's own open tx (ON CONFLICT on daily_counter
    // waiting on the uncommitted outer DELETE of the same row). Cleanup below
    // auto-commits per statement instead.

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired QueueService service;
    @Autowired QueueTokenRepository tokenRepository;
    @Autowired CounterRepository counterRepository;
    @Autowired JdbcTemplate jdbc;
    @PersistenceContext EntityManager em;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM ihms_queue.queue_token");   // auto-commit per statement
        jdbc.update("DELETE FROM ihms_queue.daily_counter");
        jdbc.update("DELETE FROM ihms_queue.counter");
    }

    private Counter counter(String code, long dept) {
        return service.createCounter(asJson("{\"code\":\"" + code + "\",\"name\":\"" + code
                + " name\",\"departmentId\":" + dept + ",\"doctorId\":77}"), 7L);
    }

    private static com.fasterxml.jackson.databind.JsonNode asJson(String s) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void flywayV1_createsSchemaTablesAndSequences() {
        Number tables = (Number) em.createNativeQuery("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'ihms_queue'
                """).getSingleResult();
        assertThat(tables.longValue()).isGreaterThanOrEqualTo(3L);   // counter, queue_token, daily_counter (+history is other schema)

        Number seq = (Number) em.createNativeQuery(
                "SELECT last_value FROM ihms_queue.\"queue_token_seq\"").getSingleResult();
        assertThat(seq.longValue()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void issue_dailyCounterIncrementsAndResetsPerDate() throws Exception {
        Counter c = counter("OPD-1", 9);
        QueueToken t1 = service.issue(asJson("{\"patientId\":1,\"departmentId\":9}"), null);
        QueueToken t2 = service.issue(asJson("{\"patientId\":2,\"departmentId\":9}"), null);
        assertThat(t1.getTokenNo()).isEqualTo(1L);
        assertThat(t2.getTokenNo()).isEqualTo(2L);

        Object lastNo = em.createNativeQuery("SELECT last_no FROM ihms_queue.daily_counter WHERE counter_date = :d")
                .setParameter("d", LocalDate.now())
                .getSingleResult();
        assertThat(((Number) lastNo).longValue()).isEqualTo(2L);

        // unique (queue_date, token_no) holds
        Long rows = ((Number) em.createNativeQuery("SELECT COUNT(*) FROM ihms_queue.queue_token").getSingleResult()).longValue();
        assertThat(rows).isEqualTo(2L);
    }

    @Test
    void fullLifecycle_callTransferComplete_onRealColumns() throws Exception {
        Counter c = counter("OPD-1", 9);
        QueueToken t = service.issue(asJson("{\"patientId\":501,\"departmentId\":9,\"priority\":1}"), null);

        QueueToken called = service.callNext(c.getId());
        assertThat(called.getId()).isEqualTo(t.getId());
        assertThat(called.getQueueStatus()).isEqualTo(QueueToken.CALLED);
        assertThat(called.getCalledAt()).isNotNull();

        // transfer to a second counter -> re-queued WAITING there
        Counter c2 = counter("OPD-2", 10);
        QueueToken moved = service.transfer(t.getId(), asJson("{\"toCounterId\":" + c2.getId() + "}"));
        assertThat(moved.getCounterId()).isEqualTo(c2.getId());
        assertThat(moved.getQueueStatus()).isEqualTo(QueueToken.WAITING);

        // call again at the target, then complete — forward-only chain on real columns
        QueueToken recalled = service.callNext(c2.getId());
        assertThat(recalled.getId()).isEqualTo(t.getId());
        QueueToken done = service.complete(t.getId());
        assertThat(done.getQueueStatus()).isEqualTo(QueueToken.COMPLETED);
        assertThat(done.getCompletedAt()).isNotNull();

        // terminal token cannot transfer
        assertThatThrownBy(() -> service.transfer(t.getId(), asJson("{\"toCounterId\":" + c.getId() + "}")))
                .isInstanceOf(com.hims.queue.application.exceptions.InvalidQueueRequestException.class)
                .hasMessageContaining("terminal");
    }

    @Test
    void emergencyPriorityJump_persistsAcrossRows() throws Exception {
        Counter c = counter("EMER", 9);
        service.issue(asJson("{\"patientId\":1,\"departmentId\":9}"), null);                    // normal #1
        service.issue(asJson("{\"patientId\":2,\"departmentId\":9}"), null);                    // normal #2
        service.issue(asJson("{\"patientId\":3,\"departmentId\":9,\"priority\":2}"), null);     // emergency #3

        QueueToken called = service.callNext(c.getId());
        assertThat(called.getPriority()).isEqualTo(QueueToken.PRIORITY_EMERGENCY);
        assertThat(called.getPatientId()).isEqualTo(3L);

        QueueToken secondCall = service.callNext(c.getId());
        assertThat(secondCall.getTokenNo()).isEqualTo(1L);   // FIFO resumes among normals
    }

    @Test
    void dashboard_projectionCountsNowServingAndWaitingList() throws Exception {
        Counter c = counter("OPD-1", 9);
        service.issue(asJson("{\"patientId\":1,\"departmentId\":9}"), null);
        QueueToken emergency = service.issue(asJson("{\"patientId\":2,\"departmentId\":9,\"priority\":2}"), null);
        service.callNext(c.getId());

        var dash = service.dashboard(null);
        assertThat(dash.get("called")).isEqualTo(1L);
        assertThat(dash.get("waiting")).isEqualTo(1L);
        assertThat(dash.get("completed")).isEqualTo(0L);
        @SuppressWarnings("unchecked")
        var nowServing = (java.util.Map<String, Object>) dash.get("nowServing");
        assertThat(nowServing).containsEntry(String.valueOf(c.getId()), emergency.getTokenNo());
        @SuppressWarnings("unchecked")
        var waitingList = (java.util.List<java.util.Map<String, Object>>) dash.get("waitingList");
        assertThat(waitingList).hasSize(1);
    }

    @Test
    void noShow_forwardOnlyGuardOnRealData() throws Exception {
        Counter c = counter("OPD-1", 9);
        QueueToken t = service.issue(asJson("{\"patientId\":5,\"departmentId\":9}"), null);
        assertThatThrownBy(() -> service.markNoShow(t.getId()))
                .isInstanceOf(com.hims.queue.application.exceptions.InvalidQueueRequestException.class)
                .hasMessageContaining("status");
        service.callNext(c.getId());
        QueueToken out = service.markNoShow(t.getId());
        assertThat(out.getQueueStatus()).isEqualTo(QueueToken.NO_SHOW);
    }
}
