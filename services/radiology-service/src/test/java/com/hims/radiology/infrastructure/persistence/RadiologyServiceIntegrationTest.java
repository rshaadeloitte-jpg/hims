package com.hims.radiology.infrastructure.persistence;

import com.hims.radiology.RadiologyServiceApplication;
import com.hims.radiology.application.RadOrderService;
import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.domain.model.RadOrderBooking;
import com.hims.radiology.domain.model.RadOrderDetail;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L2 — WP-R1 Radiology service on real PG16: V1 schema applies (ihms_rad),
 * booking -> SCHEDULED header + numbered lines, full lifecycle end-to-end
 * (start -> capture-image -> report -> COMPLETED with report persisted,
 * cancel guard), DB-level CHECK constraints (encounter XOR admission, qty > 0)
 * — and NO outbox rows / no outbox table (RadOrderCreated DEFERRED per
 * KAFKA-EVENT-CATALOG §3; explicit negative assertion below).
 * NOTE: requires Docker; skipped automatically when Docker is absent
 * (@Testcontainers(disabledWithoutDocker = true)) — do not remove.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = RadiologyServiceApplication.class)
@DirtiesContext
@Transactional
class RadiologyServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RadOrderService radOrderService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager em;

    /** Negative event assertion: RadOrderCreated DEFERRED -> nothing in any outbox. */
    private void assertNoOutboxRows() {
        // (a) no outbox table exists in ihms_rad at all (bloodbank WP-9 pattern)
        Number outboxTables = (Number) em.createNativeQuery(
                "SELECT count(*) FROM information_schema.tables "
                + "WHERE table_schema='ihms_rad' AND table_name='outbox'").getSingleResult();
        assertThat(outboxTables.longValue()).isZero();
        // (b) belt-and-braces: zero outbox rows anywhere in the schema, whatever the table state
        Number outboxRowsAnywhere = (Number) em.createNativeQuery(
                "SELECT count(*) FROM information_schema.tables t "
                + "JOIN pg_class c ON c.relname = t.table_name "
                + "WHERE t.table_schema='ihms_rad' AND c.relkind='r' "
                + "AND t.table_name LIKE '%outbox%'").getSingleResult();
        assertThat(outboxRowsAnywhere.longValue()).isZero();
    }

    @Test
    void fullRadOrderFlow_guardsAndNegativeOutbox_onRealPg() throws Exception {
        // ---- schema sanity: flyway applied, explicit sequence live ------------------
        Number seq = (Number) em.createNativeQuery("SELECT nextval('ihms_rad.t_radorderbooking_seq')")
                .getSingleResult();
        assertThat(seq.longValue()).isPositive();

        assertNoOutboxRows();   // nothing emitted even before any write

        // ---- guards before anything is written --------------------------------------
        assertThatThrownBy(() -> radOrderService.create(11L, 7L, null, "XRAY", null, List.of()))
                .isInstanceOf(InvalidRadOrderException.class)
                .extracting(e -> ((InvalidRadOrderException) e).getErrorCode())
                .isEqualTo("BEXA003");   // >= 1 line required
        assertThatThrownBy(() -> radOrderService.create(11L, 7L, 8L, "XRAY", null,
                        List.of(new RadOrderService.LineInput(5L, BigDecimal.ONE))))
                .isInstanceOf(InvalidRadOrderException.class)   // encounter XOR admission
                .hasMessageContaining("mutually exclusive");
        assertThatThrownBy(() -> radOrderService.create(11L, null, null, "PET", null,
                        List.of(new RadOrderService.LineInput(5L, BigDecimal.ONE))))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("Unknown modality");
        assertNoOutboxRows();   // rejected creates emit nothing

        // ---- create with two lines ---------------------------------------------------
        RadOrderBooking order = radOrderService.create(505L, 7777L, null, "ct", (short) 2, List.of(
                new RadOrderService.LineInput(501L, new BigDecimal("2.50")),
                new RadOrderService.LineInput(502L, BigDecimal.ONE)));
        long orderId = order.getRadOrderId();

        assertThat(order.getStatus()).isEqualTo(RadOrderBooking.STATUS_SCHEDULED);
        assertThat(order.getPriority()).isEqualTo(RadOrderBooking.PRIORITY_URGENT);
        assertThat(order.getModality()).isEqualTo("CT");   // normalized upper-case
        assertThat(order.getImageCaptured()).isFalse();
        assertThat(order.getDetails())
                .extracting(RadOrderDetail::getLineNo)
                .containsExactly(1L, 2L);
        assertThat(order.getDetails().get(0).getQty()).isEqualByComparingTo("2.50");

        assertNoOutboxRows();   // RadOrderCreated DEFERRED: booking emits NOTHING

        // ---- lifecycle: start -> capture-image -> report -> COMPLETED ---------------
        assertThat(radOrderService.start(orderId).getStatus())
                .isEqualTo(RadOrderBooking.STATUS_IN_PROGRESS);

        // start guard: second start rejected on real data
        assertThatThrownBy(() -> radOrderService.start(orderId))
                .isInstanceOf(InvalidRadOrderException.class)
                .extracting(e -> ((InvalidRadOrderException) e).getErrorCode())
                .isEqualTo("RAD_ILLEGAL_TRANSITION");

        assertThat(radOrderService.captureImage(orderId).getImageCaptured()).isTrue();

        // report payload guards
        assertThatThrownBy(() -> radOrderService.report(orderId, " ", "Dr. Ray"))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("reportText is required");

        RadOrderBooking reported = radOrderService.report(orderId, "No acute findings", "Dr. Ray");
        assertThat(reported.getStatus()).isEqualTo(RadOrderBooking.STATUS_COMPLETED);
        assertThat(reported.getReportText()).isEqualTo("No acute findings");
        assertThat(reported.getReportedBy()).isEqualTo("Dr. Ray");

        // post-completion guards: capture-image and cancel both rejected
        assertThatThrownBy(() -> radOrderService.captureImage(orderId))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("already COMPLETED");
        assertThatThrownBy(() -> radOrderService.cancel(orderId))
                .isInstanceOf(InvalidRadOrderException.class)
                .extracting(e -> ((InvalidRadOrderException) e).getErrorCode())
                .isEqualTo("RAD_ILLEGAL_TRANSITION");

        assertNoOutboxRows();   // lifecycle transitions emit NOTHING either

        // ---- persistence of the completed record on real PG -------------------------
        Object[] row = (Object[]) em.createNativeQuery(
                "SELECT status, is_image_captured, report_text, reported_by, priority, modality "
                + "FROM ihms_rad.t_radorderbooking WHERE rad_order_id = ?")
                .setParameter(1, orderId).getSingleResult();
        assertThat(row[0]).isEqualTo((short) RadOrderBooking.STATUS_COMPLETED);
        assertThat(row[1]).isEqualTo(Boolean.TRUE);
        assertThat(row[2]).isEqualTo("No acute findings");
        assertThat(row[3]).isEqualTo("Dr. Ray");
        assertThat(row[4]).isEqualTo((short) 2);
        assertThat(row[5]).isEqualTo("CT");

        // ---- DB-level CHECK pins encounter XOR admission at schema level too --------
        // Run on a SEPARATE auto-crolled-back connection: a PG check violation aborts
        // the surrounding @Transactional test transaction, poisoning every later statement.
        boolean ctxViolationFired;
        try (var conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try (var ps = conn.prepareStatement(
                    "INSERT INTO ihms_rad.t_radorderbooking "
                    + "(rad_order_id, patient_id, encounter_id, admission_id, modality, priority, status, is_image_captured) "
                    + "VALUES (nextval('ihms_rad.t_radorderbooking_seq'), 1, 7, 8, 'XRAY', 1, 1, FALSE)")) {
                ps.execute();
                conn.rollback();
                ctxViolationFired = false;   // INSERT went through -> constraint missing
            } catch (java.sql.SQLException expected) {
                assertThat(expected.getMessage()).contains("ck_t_radorderbooking_ctx");
                conn.rollback();
                ctxViolationFired = true;
            }
        }
        assertThat(ctxViolationFired).as("ck_t_radorderbooking_ctx must reject encounter+admission together").isTrue();

        // ---- cancel path for a second (scheduled) order ------------------------------
        RadOrderBooking other = radOrderService.create(506L, null, 8888L, "XRAY", null,
                List.of(new RadOrderService.LineInput(503L, BigDecimal.TEN)));
        assertThat(other.getStatus()).isEqualTo(RadOrderBooking.STATUS_SCHEDULED);
        assertThat(other.getPriority()).isEqualTo(RadOrderBooking.PRIORITY_ROUTINE);   // default routine
        RadOrderBooking cancelled = radOrderService.cancel(other.getRadOrderId());
        assertThat(cancelled.getStatus()).isEqualTo(RadOrderBooking.STATUS_CANCELLED);

        // ---- list filters -------------------------------------------------------------
        assertThat(radOrderService.list((short) RadOrderBooking.STATUS_CANCELLED, 506L, null))
                .extracting(RadOrderBooking::getRadOrderId)
                .containsExactly(other.getRadOrderId());
        assertThat(radOrderService.list(null, 999_999L, null)).isEmpty();
        assertThat(radOrderService.list(null, null, "CT"))
                .extracting(RadOrderBooking::getRadOrderId)
                .containsExactly(orderId);

        assertNoOutboxRows();   // FINAL negative outbox assertion for the whole flow
    }
}
