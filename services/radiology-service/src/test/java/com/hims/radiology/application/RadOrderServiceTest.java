package com.hims.radiology.application;

import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.domain.model.RadOrderBooking;
import com.hims.radiology.domain.repository.RadOrderDetailRepository;
import com.hims.radiology.domain.repository.RadOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L0 — validation guards and lifecycle rules, no Spring context.
 * Register WP-R1 assertions: >=1 line required (BEXA003), encounterId XOR admissionId,
 * modality XRAY/CT/MRI/US, priority 1|2, start guard SCHEDULED-only,
 * capture-image 400 when COMPLETED, report requires IN_PROGRESS,
 * cancel guard not-COMPLETED. NO events exist (RadOrderCreated DEFERRED) —
 * there is no outbox collaborator to mock by design.
 */
class RadOrderServiceTest {

    private RadOrderRepository orderRepository;
    private RadOrderDetailRepository detailRepository;
    private RadOrderService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        orderRepository = mock(RadOrderRepository.class);
        detailRepository = mock(RadOrderDetailRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new RadOrderService(orderRepository, detailRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(detailRepository.findByRadOrderIdOrderByLineNoAsc(anyLong())).thenReturn(List.of());
        when(orderRepository.findById(anyLong())).thenAnswer(inv -> {
            // simulate a persisted booking: return whatever was saved under that id
            return Optional.empty();
        });
    }

    private void persistFindable(long id, RadOrderBooking order) {
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
    }

    private RadOrderBooking savedBooking(long id, short status) {
        RadOrderBooking o = new RadOrderBooking();
        o.setRadOrderId(id);
        o.setPatientId(505L);
        o.setModality("CT");
        o.setPriority(RadOrderBooking.PRIORITY_ROUTINE);
        o.setStatus(status);
        o.setImageCaptured(false);
        return o;
    }

    private RadOrderService.LineInput line(long itemId, String qty) {
        return new RadOrderService.LineInput(itemId, new BigDecimal(qty));
    }

    // ---------------------------------------------------------------- create guards

    @Test
    void create_patientRequired() {
        assertThatThrownBy(() -> service.create(null, 7L, null, "XRAY", null, List.of(line(1L, "1"))))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("patientId");
    }

    @Test
    void create_modalityRequired_andMustBeKnown() {
        assertThatThrownBy(() -> service.create(505L, null, null, null, null, List.of(line(1L, "1"))))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("modality is required");
        assertThatThrownBy(() -> service.create(505L, null, null, "PET", null, List.of(line(1L, "1"))))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("Unknown modality");
    }

    @Test
    void create_priorityMustBeRoutineOrUrgent() {
        assertThatThrownBy(() -> service.create(505L, null, null, "MRI", (short) 9, List.of(line(1L, "1"))))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("priority");
    }

    @Test
    void create_atLeastOneLineRequired() {
        assertThatThrownBy(() -> service.create(505L, 7L, null, "XRAY", null, List.of()))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("At least one");
        assertThatThrownBy(() -> service.create(505L, 7L, null, "XRAY", null, null))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("At least one");
    }

    @Test
    void create_encounterXorAdmission_bothSuppliedRejected() {
        assertThatThrownBy(() -> service.create(505L, 7L, 8L, "XRAY", null, List.of(line(1L, "1"))))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("mutually exclusive");
    }

    @Test
    void create_eachLineNeedsItemIdAndPositiveQty() {
        assertThatThrownBy(() -> service.create(505L, 7L, null, "XRAY", null,
                        List.of(new RadOrderService.LineInput(null, BigDecimal.ONE))))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("itemId");
        assertThatThrownBy(() -> service.create(505L, 7L, null, "XRAY", null,
                        List.of(line(5L, "0"))))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("qty must be > 0");
        assertThatThrownBy(() -> service.create(505L, 7L, null, "XRAY", null,
                        List.of(new RadOrderService.LineInput(5L, new BigDecimal("-2")))))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("qty must be > 0");
    }

    @Test
    void create_valid_defaultsScheduled_routine_notCaptured() {
        // simulate persistence: findById returns the last saved aggregate
        java.util.Map<Long, RadOrderBooking> store = new java.util.HashMap<>();
        when(orderRepository.save(any())).thenAnswer(inv -> {
            RadOrderBooking o = inv.getArgument(0);
            if (o.getRadOrderId() == null) o.setRadOrderId(seq.incrementAndGet());
            store.put(o.getRadOrderId(), o);
            return o;
        });
        when(orderRepository.findById(org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0, Long.class))));

        RadOrderBooking created = service.create(505L, 7777L, null, "xray", null, List.of(line(501L, "2.5")));
        assertThat(created.getStatus()).isEqualTo(RadOrderBooking.STATUS_SCHEDULED);
        assertThat(created.getPriority()).isEqualTo(RadOrderBooking.PRIORITY_ROUTINE);   // default routine
        assertThat(created.getImageCaptured()).isFalse();
        assertThat(created.getModality()).isEqualTo("XRAY");   // normalized upper-case
    }

    // ---------------------------------------------------------------- lifecycle guards

    @Test
    void start_guard_scheduledOnly() {
        RadOrderBooking inProgress = savedBooking(10L, RadOrderBooking.STATUS_IN_PROGRESS);
        persistFindable(10L, inProgress);
        assertThatThrownBy(() -> service.start(10L))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("RAD_ILLEGAL_TRANSITION"))
                .hasMessageContaining("only SCHEDULED orders can be started");

        RadOrderBooking completed = savedBooking(11L, RadOrderBooking.STATUS_COMPLETED);
        persistFindable(11L, completed);
        assertThatThrownBy(() -> service.start(11L))
                .isInstanceOf(InvalidRadOrderException.class)
                .hasMessageContaining("Cannot start");

        RadOrderBooking scheduled = savedBooking(12L, RadOrderBooking.STATUS_SCHEDULED);
        persistFindable(12L, scheduled);
        assertThat(service.start(12L).getStatus()).isEqualTo(RadOrderBooking.STATUS_IN_PROGRESS);
    }

    @Test
    void captureImage_guard_rejectedWhenCompleted_setsFlagOtherwise() {
        RadOrderBooking completed = savedBooking(20L, RadOrderBooking.STATUS_COMPLETED);
        persistFindable(20L, completed);
        assertThatThrownBy(() -> service.captureImage(20L))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("RAD_ILLEGAL_TRANSITION"))
                .hasMessageContaining("already COMPLETED");

        RadOrderBooking scheduled = savedBooking(21L, RadOrderBooking.STATUS_SCHEDULED);
        persistFindable(21L, scheduled);
        RadOrderBooking captured = service.captureImage(21L);
        assertThat(captured.getImageCaptured()).isTrue();
        // re-capture stays idempotent-true while not completed
        assertThat(service.captureImage(21L).getImageCaptured()).isTrue();
    }

    @Test
    void report_requiresInProgress_andPayloads_thenCompletes() {
        RadOrderBooking scheduled = savedBooking(30L, RadOrderBooking.STATUS_SCHEDULED);
        persistFindable(30L, scheduled);
        assertThatThrownBy(() -> service.report(30L, "findings", "Dr. Ray"))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("RAD_ILLEGAL_TRANSITION"))
                .hasMessageContaining("requires IN_PROGRESS");

        RadOrderBooking inProgress = savedBooking(31L, RadOrderBooking.STATUS_IN_PROGRESS);
        persistFindable(31L, inProgress);
        assertThatThrownBy(() -> service.report(31L, " ", "Dr. Ray"))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("reportText is required");
        assertThatThrownBy(() -> service.report(31L, "findings", null))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("BEXA003"))
                .hasMessageContaining("reportedBy is required");

        RadOrderBooking reported = service.report(31L, "No acute findings", "Dr. Ray");
        assertThat(reported.getStatus()).isEqualTo(RadOrderBooking.STATUS_COMPLETED);
        assertThat(reported.getReportText()).isEqualTo("No acute findings");
        assertThat(reported.getReportedBy()).isEqualTo("Dr. Ray");
    }

    @Test
    void cancel_guard_completedOnly() {
        RadOrderBooking completed = savedBooking(40L, RadOrderBooking.STATUS_COMPLETED);
        persistFindable(40L, completed);
        assertThatThrownBy(() -> service.cancel(40L))
                .isInstanceOfSatisfying(InvalidRadOrderException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo("RAD_ILLEGAL_TRANSITION"))
                .hasMessageContaining("completed orders cannot be cancelled");

        RadOrderBooking scheduled = savedBooking(41L, RadOrderBooking.STATUS_SCHEDULED);
        persistFindable(41L, scheduled);
        assertThat(service.cancel(41L).getStatus()).isEqualTo(RadOrderBooking.STATUS_CANCELLED);

        RadOrderBooking inProgress = savedBooking(42L, RadOrderBooking.STATUS_IN_PROGRESS);
        persistFindable(42L, inProgress);
        assertThat(service.cancel(42L).getStatus()).isEqualTo(RadOrderBooking.STATUS_CANCELLED);
    }

    @Test
    void get_unknown_throws404Code() {
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(com.hims.radiology.application.exceptions.RadOrderNotFoundException.class)
                .extracting(e -> ((com.hims.radiology.application.exceptions.RadOrderNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }
}
