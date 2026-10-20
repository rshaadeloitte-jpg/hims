package com.hims.radiology.application;

import com.hims.radiology.application.exceptions.InvalidRadOrderException;
import com.hims.radiology.application.exceptions.RadOrderNotFoundException;
import com.hims.radiology.domain.model.RadOrderBooking;
import com.hims.radiology.domain.model.RadOrderDetail;
import com.hims.radiology.domain.repository.RadOrderDetailRepository;
import com.hims.radiology.domain.repository.RadOrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Radiology order booking slice (GAP-REGISTER-5 WP-R1, Catalog #8). NEW service
 * replacing the legacy radiology booking fragment; the post-saga OrderCreated event
 * (ordering->billing choreography) is DEFERRED per KAFKA-EVENT-CATALOG §3 —
 * never silently invented here (comment-only, no outbox exists in this schema).
 *
 * Status machine (t_radorderbooking.status SMALLINT):
 *   1=SCHEDULED -> start -> 2=IN_PROGRESS -> report {reportText, reportedBy} -> 3=COMPLETED
 *   cancel: allowed from any status except COMPLETED -> 4=CANCELLED
 *   capture-image: sets is_image_captured=true; 400 when already COMPLETED.
 *
 * Events: RadOrderCreated is DEFERRED per catalog ("E deferred") — NOTHING is
 * emitted on booking (no outbox table in ihms_rad, bloodbank WP-9 pattern);
 * the integration test asserts the absence explicitly.
 */
@Service
public class RadOrderService {

    private static final Logger log = LoggerFactory.getLogger(RadOrderService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    public static final Set<String> MODALITIES = Set.of("XRAY", "CT", "MRI", "US");

    private final RadOrderRepository orderRepository;
    private final RadOrderDetailRepository detailRepository;

    @PersistenceContext
    private EntityManager em;

    public RadOrderService(RadOrderRepository orderRepository,
                           RadOrderDetailRepository detailRepository) {
        this.orderRepository = orderRepository;
        this.detailRepository = detailRepository;
    }

    // ------------------------------------------------------------------ create

    /**
     * Create a rad order booking header + lines. Guards (all 400 BEXA003-style):
     * patientId required; modality required and one of XRAY/CT/MRI/US;
     * priority (when supplied) must be 1=routine or 2=urgent (default routine);
     * at least ONE line required; encounterId XOR admissionId — at most one of the
     * two may be supplied; every line needs itemId and qty &gt; 0.
     * Emits NOTHING (RadOrderCreated DEFERRED per KAFKA-EVENT-CATALOG §3).
     */
    @Transactional
    public RadOrderBooking create(Long patientId, Long encounterId, Long admissionId,
                                  String modality, Short priority, List<LineInput> lines) {
        if (patientId == null) {
            throw new InvalidRadOrderException("BEXA003", "patientId is required");
        }
        if (modality == null || modality.isBlank()) {
            throw new InvalidRadOrderException("BEXA003",
                    "modality is required (XRAY/CT/MRI/US)");
        }
        String normalizedModality = modality.trim().toUpperCase();
        if (!MODALITIES.contains(normalizedModality)) {
            throw new InvalidRadOrderException("BEXA003",
                    "Unknown modality: " + modality + " — allowed: XRAY, CT, MRI, US");
        }
        if (priority == null) {
            priority = RadOrderBooking.PRIORITY_ROUTINE;   // default 1=routine
        }
        if (priority != RadOrderBooking.PRIORITY_ROUTINE && priority != RadOrderBooking.PRIORITY_URGENT) {
            throw new InvalidRadOrderException("BEXA003",
                    "priority must be 1 (routine) or 2 (urgent)");
        }
        if (encounterId != null && admissionId != null) {
            throw new InvalidRadOrderException("BEXA003",
                    "encounterId and admissionId are mutually exclusive — supply at most one");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidRadOrderException("BEXA003",
                    "At least one rad order line {itemId,qty} is required");
        }
        for (LineInput line : lines) {
            if (line == null || line.itemId() == null) {
                throw new InvalidRadOrderException("BEXA003",
                        "Each line needs a non-null itemId");
            }
            if (line.qty() == null || line.qty().signum() <= 0) {
                throw new InvalidRadOrderException("BEXA003",
                        "Line qty must be > 0 for itemId " + line.itemId());
            }
        }

        Timestamp nowTs = now();
        RadOrderBooking order = new RadOrderBooking();
        order.setRadOrderId(nextId("t_radorderbooking_seq"));
        order.setPatientId(patientId);
        order.setEncounterId(encounterId);
        order.setAdmissionId(admissionId);
        order.setModality(normalizedModality);
        order.setPriority(priority);
        order.setStatus(RadOrderBooking.STATUS_SCHEDULED);
        order.setImageCaptured(false);
        order.setAddedBy(AUDIT_ACTOR_ID);
        order.setAddedDatetime(nowTs);
        orderRepository.save(order);

        long lineNo = 1L;
        for (LineInput line : lines) {
            RadOrderDetail d = new RadOrderDetail();
            d.setRadOrderId(order.getRadOrderId());
            d.setLineNo(lineNo++);
            d.setItemId(line.itemId());
            d.setQty(line.qty());
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(nowTs);
            detailRepository.save(d);
        }

        // NOTE: no outbox write here — RadOrderCreated is DEFERRED (register WP-R1);
        // post-saga OrderCreated is out of scope for this tranche (comment-only).
        log.info("Created rad order {} for patient {} modality {} with {} line(s) — no event emitted (RadOrderCreated DEFERRED)",
                order.getRadOrderId(), patientId, normalizedModality, lines.size());
        return get(order.getRadOrderId());
    }

    // ------------------------------------------------------------------ lifecycle

    /** Guard: SCHEDULED (1) only -> IN_PROGRESS (2). 400 RAD_ILLEGAL_TRANSITION otherwise. */
    @Transactional
    public RadOrderBooking start(long radOrderId) {
        RadOrderBooking order = load(radOrderId);
        if (!order.isScheduled()) {
            throw new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                    "Cannot start rad order " + radOrderId + " from status "
                            + statusName(order.getStatus())
                            + " — only SCHEDULED orders can be started");
        }
        order.setStatus(RadOrderBooking.STATUS_IN_PROGRESS);
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);
        log.info("Started rad order {}", radOrderId);
        return get(radOrderId);
    }

    /**
     * Set is_image_captured=true. Guard per register: 400 when already COMPLETED
     * (capture on a completed order would falsify the record). Other statuses allow
     * the capture flag to be set (re-capture / late capture before reporting).
     */
    @Transactional
    public RadOrderBooking captureImage(long radOrderId) {
        RadOrderBooking order = load(radOrderId);
        if (order.isCompleted()) {
            throw new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                    "Cannot capture image for rad order " + radOrderId
                            + " — order is already COMPLETED");
        }
        order.setImageCaptured(true);
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);
        log.info("Image captured for rad order {}", radOrderId);
        return get(radOrderId);
    }

    /**
     * Report the study: requires IN_PROGRESS (2); sets reportText/reportedBy and
     * status COMPLETED (3). reportText and reportedBy are required payloads.
     */
    @Transactional
    public RadOrderBooking report(long radOrderId, String reportText, String reportedBy) {
        RadOrderBooking order = load(radOrderId);
        if (reportText == null || reportText.isBlank()) {
            throw new InvalidRadOrderException("BEXA003", "reportText is required");
        }
        if (reportedBy == null || reportedBy.isBlank()) {
            throw new InvalidRadOrderException("BEXA003", "reportedBy is required");
        }
        if (!order.isInProgress()) {
            throw new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                    "Cannot report rad order " + radOrderId + " from status "
                            + statusName(order.getStatus())
                            + " — reporting requires IN_PROGRESS");
        }
        order.setReportText(reportText);
        order.setReportedBy(reportedBy);
        order.setStatus(RadOrderBooking.STATUS_COMPLETED);
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);
        log.info("Reported rad order {} by {}", radOrderId, reportedBy);
        return get(radOrderId);
    }

    /** Guard: NOT COMPLETED (cancel guard per register) -> CANCELLED (4). */
    @Transactional
    public RadOrderBooking cancel(long radOrderId) {
        RadOrderBooking order = load(radOrderId);
        if (order.isCompleted()) {
            throw new InvalidRadOrderException("RAD_ILLEGAL_TRANSITION",
                    "Cannot cancel rad order " + radOrderId + " from status COMPLETED"
                            + " — completed orders cannot be cancelled");
        }
        order.setStatus(RadOrderBooking.STATUS_CANCELLED);
        order.setUpdatedBy(AUDIT_ACTOR_ID);
        order.setUpdatedDatetime(now());
        orderRepository.save(order);
        log.info("Cancelled rad order {}", radOrderId);
        return get(radOrderId);
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public RadOrderBooking get(long radOrderId) {
        RadOrderBooking order = load(radOrderId);
        order.setDetails(detailsOf(radOrderId));
        return order;
    }

    @Transactional(readOnly = true)
    public List<RadOrderBooking> list(Short status, Long patientId, String modality) {
        String normalizedModality = modality == null || modality.isBlank()
                ? null : modality.trim().toUpperCase();
        List<RadOrderBooking> orders = orderRepository.search(status, patientId, normalizedModality);
        for (RadOrderBooking o : orders) {
            o.setDetails(detailsOf(o.getRadOrderId()));
        }
        return orders;
    }

    // ------------------------------------------------------------------ helpers

    private RadOrderBooking load(long radOrderId) {
        return orderRepository.findById(radOrderId)
                .orElseThrow(() -> new RadOrderNotFoundException("NOT_FOUND",
                        "Rad order not found: " + radOrderId));
    }

    private List<RadOrderDetail> detailsOf(long radOrderId) {
        return detailRepository.findByRadOrderIdOrderByLineNoAsc(radOrderId);
    }

    static String statusName(short s) {
        return switch (s) {
            case 1 -> "SCHEDULED";
            case 2 -> "IN_PROGRESS";
            case 3 -> "COMPLETED";
            case 4 -> "CANCELLED";
            default -> String.valueOf(s);
        };
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_rad." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested rad order line at creation. */
    public record LineInput(Long itemId, java.math.BigDecimal qty) {}

    /** M16 retest: mark a RadOrderDetail for retest with a reason. */
    @Transactional
    public com.hims.radiology.domain.model.RadOrderDetail requestRetest(long radOrderId, long lineNo, String reason) {
        com.hims.radiology.domain.model.RadOrderDetail d = detailRepository.findById(
                new com.hims.radiology.domain.model.RadOrderDetailKey(radOrderId, lineNo))
                .orElseThrow(() -> new com.hims.radiology.application.exceptions.RadOrderNotFoundException("NOT_FOUND", "Rad detail not found: " + radOrderId + "/" + lineNo));
        d.setRetestRequested(true);
        d.setRetestReason(reason);
        stampUpdate(d);
        return detailRepository.save(d);
    }

    /** M16 reschedule: rebook a RadOrder for a future scheduled timestamp. */
    @Transactional
    public com.hims.radiology.domain.model.RadOrderDetail reschedule(long radOrderId, long lineNo, java.sql.Timestamp when) {
        com.hims.radiology.domain.model.RadOrderDetail d = detailRepository.findById(
                new com.hims.radiology.domain.model.RadOrderDetailKey(radOrderId, lineNo))
                .orElseThrow(() -> new com.hims.radiology.application.exceptions.RadOrderNotFoundException("NOT_FOUND", "Rad detail not found: " + radOrderId + "/" + lineNo));
        d.setRescheduledTo(when);
        stampUpdate(d);
        return detailRepository.save(d);
    }
    private void stampUpdate(com.hims.radiology.domain.model.RadOrderBase entity) {
        entity.setUpdatedBy(AUDIT_ACTOR_ID);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
}
