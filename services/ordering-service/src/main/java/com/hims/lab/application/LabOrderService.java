package com.hims.lab.application;

import com.hims.lab.application.exceptions.InvalidLabRequestException;
import com.hims.lab.application.exceptions.LabEntityNotFoundException;
import com.hims.lab.domain.model.PathOrderBooking;
import com.hims.lab.domain.model.PathOrderDetail;
import com.hims.lab.domain.model.PathoPatientParameterDetails;
import com.hims.lab.domain.repository.PathOrderBookingRepository;
import com.hims.lab.domain.repository.PathOrderDetailRepository;
import com.hims.lab.domain.repository.PathoPatientParameterDetailsRepository;
import com.hims.lab.infrastructure.outbox.OutboxPublisher;
import com.hims.lab.infrastructure.outbox.OutboxService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lab result-side slice (GAP-REGISTER WP-7, Catalog #7). Order BOOKING stays in the
 * monolith until Wave 4 per MIGRATION-WAVES; this service owns the result/worklist side.
 *
 * Status machine (header t_pathorderbooking.status, mirrored on details):
 *   1 ORDERED --collect--> 2 SAMPLE_COLLECTED --start--> 3 IN_PROGRESS
 *   --finalize ALL details--> 4 FINALIZED ;  cancel from 1|2|3 --> 5 CANCELLED.
 * Illegal transition -> 400 LAB_ILLEGAL_TRANSITION.
 *
 * Result parameters upsert keyed (detailId, parameterName); finalize-per-detail marks
 * parameter rows is_final=true + finalized_at. E-05 LabResultFinalized is emitted via
 * the transactional outbox EXACTLY ONCE per booking — when the LAST detail finalizes
 * (type string 'LabResultFinalized', schema lab-result-finalized.v1, partition key
 * encounterId, payload keys exactly {bookingId, detailIds[], patientId, encounterId,
 * results[{parameterName,value,unit}], status, finalizedAt}).
 */
@Service
public class LabOrderService {

    private static final Logger log = LoggerFactory.getLogger(LabOrderService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final PathOrderBookingRepository bookingRepository;
    private final PathOrderDetailRepository detailRepository;
    private final PathoPatientParameterDetailsRepository parameterRepository;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager em;

    public LabOrderService(PathOrderBookingRepository bookingRepository,
                           PathOrderDetailRepository detailRepository,
                           PathoPatientParameterDetailsRepository parameterRepository,
                           OutboxService outboxService) {
        this.bookingRepository = bookingRepository;
        this.detailRepository = detailRepository;
        this.parameterRepository = parameterRepository;
        this.outboxService = outboxService;
    }

    // ------------------------------------------------------------------ create

    /** Create a booking with test lines: patientId required, encounterId nullable. */
    @Transactional
    public PathOrderBooking create(Long patientId, Long encounterId, Boolean isOutSourced,
                                   List<TestLine> tests) {
        if (patientId == null) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "patientId is required");
        }
        if (tests == null || tests.isEmpty()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "At least one test line (testCode/testName) is required");
        }

        PathOrderBooking booking = new PathOrderBooking();
        booking.setBookingId(nextId("t_pathorderbooking_seq"));
        booking.setPatientId(patientId);
        booking.setEncounterId(encounterId);
        booking.setBookingNo("LAB-" + booking.getBookingId());
        booking.setBookingStatus(PathOrderBooking.STATUS_ORDERED);
        booking.setIsOutSourced(Boolean.TRUE.equals(isOutSourced));
        booking.setStatus(true);
        booking.setSynchronizedd(false);
        booking.setAddedBy(AUDIT_ACTOR_ID);
        booking.setAddedDatetime(now());
        booking = bookingRepository.save(booking);

        for (TestLine line : tests) {
            if (line == null || line.testCode() == null || line.testCode().isBlank()
                    || line.testName() == null || line.testName().isBlank()) {
                throw new InvalidLabRequestException("LAB_REQ_INVALID",
                        "Each test line needs non-blank testCode and testName");
            }
            PathOrderDetail d = new PathOrderDetail();
            d.setDetailId(nextId("t_pathorderdetail_seq"));
            d.setBookingId(booking.getBookingId());
            d.setTestCode(line.testCode().trim());
            d.setTestName(line.testName().trim());
            d.setDetailStatus(PathOrderBooking.STATUS_ORDERED);
            d.setIsFinal(false);
            d.setStatus(true);
            d.setSynchronizedd(false);
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(booking.getAddedDatetime());
            detailRepository.save(d);
        }
        log.info("Created lab booking {} ({}) for patient {} with {} test lines",
                booking.getBookingNo(), booking.getBookingId(), patientId, tests.size());
        return get(booking.getBookingId());
    }

    // ------------------------------------------------------- status machine ops

    /** ORDERED -> SAMPLE_COLLECTED (mirrored onto every detail). */
    @Transactional
    public PathOrderBooking collect(long bookingId) {
        return transition(bookingId, PathOrderBooking.STATUS_SAMPLE_COLLECTED, "collect");
    }

    /** SAMPLE_COLLECTED -> IN_PROGRESS (mirrored onto every detail). */
    @Transactional
    public PathOrderBooking start(long bookingId) {
        return transition(bookingId, PathOrderBooking.STATUS_IN_PROGRESS, "start");
    }

    /** 1|2|3 -> CANCELLED; never from FINALIZED or already-cancelled bookings. */
    @Transactional
    public PathOrderBooking cancel(long bookingId) {
        return transition(bookingId, PathOrderBooking.STATUS_CANCELLED, "cancel");
    }

    private PathOrderBooking transition(long bookingId, short target, String action) {
        PathOrderBooking booking = load(bookingId);
        short current = booking.getBookingStatus();
        boolean legal =
                (target == PathOrderBooking.STATUS_SAMPLE_COLLECTED && current == PathOrderBooking.STATUS_ORDERED)
             || (target == PathOrderBooking.STATUS_IN_PROGRESS && current == PathOrderBooking.STATUS_SAMPLE_COLLECTED)
             || (target == PathOrderBooking.STATUS_CANCELLED && current != PathOrderBooking.STATUS_CANCELLED
                    && current != PathOrderBooking.STATUS_FINALIZED);
        if (!legal) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Cannot " + action + " booking " + bookingId + " from status " + current);
        }
        Timestamp nowTs = now();
        booking.setBookingStatus(target);
        booking.setUpdatedBy(AUDIT_ACTOR_ID);
        booking.setUpdatedDatetime(nowTs);
        bookingRepository.save(booking);
        for (PathOrderDetail d : detailsOf(bookingId)) {
            d.setDetailStatus(target);
            d.setUpdatedBy(AUDIT_ACTOR_ID);
            d.setUpdatedDatetime(nowTs);
            detailRepository.save(d);
        }
        log.info("Booking {} {} : {} -> {}", bookingId, action, current, target);
        return get(bookingId);
    }

    // ------------------------------------------------------------------ results

    /**
     * Upsert result parameter rows for one detail (keyed by parameter_name, case-insensitive).
     * Rows absent from the payload are removed; re-entry resets is_final so results must be
     * finalized again. Guarded: booking must not be cancelled/finalized and the detail itself
     * must not already be final (results are locked after finalize).
     */
    @Transactional
    public PathOrderDetail upsertResults(long bookingId, long detailId, List<ResultRow> rows) {
        PathOrderBooking booking = load(bookingId);
        if (booking.isCancelled() || booking.isFinalized()) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Results cannot be modified on a " + statusName(booking.getBookingStatus()) + " booking");
        }
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (Boolean.TRUE.equals(detail.getIsFinal())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already finalized; results are locked");
        }
        if (rows == null || rows.isEmpty()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "At least one result row {parameterName,value,unit} is required");
        }

        Set<String> incomingNames = new HashSet<>();
        for (ResultRow row : rows) {
            if (row.parameterName() == null || row.parameterName().isBlank()) {
                throw new InvalidLabRequestException("LAB_REQ_INVALID", "parameterName is required");
            }
            String name = row.parameterName().trim();
            if (!incomingNames.add(name.toLowerCase())) {
                throw new InvalidLabRequestException("LAB_REQ_INVALID",
                        "Duplicate parameterName in payload: " + name);
            }
            PathoPatientParameterDetails p = parameterRepository
                    .findByDetailIdAndParameterNameIgnoreCase(detailId, name)
                    .orElseGet(() -> {
                        PathoPatientParameterDetails np = new PathoPatientParameterDetails();
                        np.setParamId(nextId("t_pathopatientparameterdetails_seq"));
                        np.setDetailId(detailId);
                        np.setParameterName(name);
                        np.setAddedBy(AUDIT_ACTOR_ID);
                        np.setAddedDatetime(now());
                        return np;
                    });
            p.setValue(row.value());
            p.setUnit(row.unit());
            p.setReferenceRange(row.referenceRange());
            // M15: critical detection — legacy PathoOrderServiceImpl:6506 (value vs "min-max")
            p.setIsCritical(isCriticalValue(row.value(), row.referenceRange()));
            p.setCriticalNotifiedAt(null);
            p.setIsFinal(false);          // re-entry un-finalizes until /finalize runs again
            p.setFinalizedAt(null);
            p.setUpdatedBy(AUDIT_ACTOR_ID);
            p.setUpdatedDatetime(now());
            parameterRepository.save(p);
        }

        // drop rows removed from the payload
        List<Long> keep = parameterRepository.findByDetailIdOrderByParamIdAsc(detailId).stream()
                .filter(p -> incomingNames.contains(p.getParameterName().toLowerCase()))
                .map(PathoPatientParameterDetails::getParamId)
                .toList();
        var all = parameterRepository.findByDetailIdOrderByParamIdAsc(detailId);
        for (PathoPatientParameterDetails p : all) {
            if (!keep.contains(p.getParamId())) {
                parameterRepository.delete(p);
            }
        }
        return loadDetail(detailId);
    }

    // ------------------------------------------------------------------ finalize

    /**
     * Finalize ONE detail: locks its parameter rows (is_final=true + finalized_at).
     * When the LAST detail of the booking finalizes -> booking FINALIZED and E-05
     * LabResultFinalized appended to the outbox exactly once (partition key encounterId).
     */
    @Transactional
    public PathOrderBooking finalizeDetail(long bookingId, long detailId) {
        PathOrderBooking booking = load(bookingId);
        if (booking.getBookingStatus() != PathOrderBooking.STATUS_IN_PROGRESS) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Finalize requires booking status "
                            + PathOrderBooking.STATUS_IN_PROGRESS + " (IN_PROGRESS), got "
                            + booking.getBookingStatus());
        }
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (Boolean.TRUE.equals(detail.getIsFinal())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already finalized");
        }
        long paramCount = parameterRepository.findByDetailIdOrderByParamIdAsc(detailId).size();
        if (paramCount == 0) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " has no result parameters to finalize");
        }

        Timestamp nowTs = now();
        for (PathoPatientParameterDetails p : parameterRepository.findByDetailIdOrderByParamIdAsc(detailId)) {
            p.setIsFinal(true);
            p.setFinalizedAt(nowTs);
            p.setUpdatedBy(AUDIT_ACTOR_ID);
            p.setUpdatedDatetime(nowTs);
            parameterRepository.save(p);
        }
        detail.setIsFinal(true);
        detail.setFinalizedAt(nowTs);
        detail.setUpdatedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedDatetime(nowTs);
        detailRepository.save(detail);

        // M15: propagate critical marker if any param for this detail is critical
        boolean detailHasCritical = parameterRepository.findByDetailIdOrderByParamIdAsc(detailId).stream()
                .anyMatch(p -> Boolean.TRUE.equals(p.getIsCritical()));
        if (detailHasCritical) {
            booking.setIsCritical(true);
        }

        if (detailRepository.countByBookingIdAndIsFinalFalse(bookingId) > 0) {
            if (Boolean.TRUE.equals(booking.getIsCritical())) {
                bookingRepository.save(booking);
            }
            log.info("Detail {} of booking {} finalized; waiting on remaining details", detailId, bookingId);
            return get(bookingId);   // NOT all final yet -> no event
        }

        // whole booking done: header FINALIZED + single E-05 emission
        // ensure header critical reflects any critical across ALL details
        boolean anyCritical = parameterRepository.findByDetailIdInOrderByParamIdAsc(
                        detailsOf(bookingId).stream().map(PathOrderDetail::getDetailId).toList())
                .stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsCritical()));
        if (anyCritical) booking.setIsCritical(true);
        booking.setBookingStatus(PathOrderBooking.STATUS_FINALIZED);
        booking.setFinalizedAt(nowTs);
        booking.setUpdatedBy(AUDIT_ACTOR_ID);
        booking.setUpdatedDatetime(nowTs);
        PathOrderBooking saved = bookingRepository.save(booking);
        emitLabResultFinalized(saved, detailsOf(bookingId), nowTs);
        return get(bookingId);
    }

    // ------------------------------------------------------------------ M15 workflow

    /** Generate barcode for a detail (idempotent): LAB-BC-{yyyyMM}{%07d seq}. */
    @Transactional
    public PathOrderDetail generateBarcode(long bookingId, long detailId) {
        PathOrderBooking booking = load(bookingId);
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (booking.isCancelled() || booking.isFinalized()) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Barcode cannot be generated on a " + statusName(booking.getBookingStatus()) + " booking");
        }
        if (detail.getBarcode() != null && !detail.getBarcode().isBlank()) {
            return detail; // idempotent
        }
        // ponytail: yyyyMM + 7-digit seq; mirrors waste-service bag barcode pattern
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        String yyyymm = String.format("%04d%02d", nowDate.getYear(), nowDate.getMonthValue());
        long seq = nextId("lab_barcode_seq");
        String barcode = yyyymm + String.format("%07d", seq % 10_000_000L);
        detail.setBarcode(barcode);
        if (detail.getSampleNo() == null || detail.getSampleNo().isBlank()) {
            detail.setSampleNo(barcode); // legacy SampleNoStr reuse; barcode == sampleNo initially
        }
        detail.setUpdatedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedDatetime(now());
        detailRepository.save(detail);
        log.info("Generated barcode {} for detail {} of booking {}", barcode, detailId, bookingId);
        return detail;
    }

    /** Accept a sample: IsAccepted=true + acceptanceDate (ported from SampleAcceptDto / CIMS_SampleAccept). */
    @Transactional
    public PathOrderDetail acceptSample(long bookingId, long detailId, String acceptanceReason) {
        PathOrderBooking booking = load(bookingId);
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (booking.isCancelled()) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION", "Cannot accept on a cancelled booking");
        }
        if (Boolean.TRUE.equals(detail.getIsRejected())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already rejected; cannot accept");
        }
        if (Boolean.TRUE.equals(detail.getIsAccepted())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already accepted");
        }
        if (detail.getSampleNo() == null || detail.getSampleNo().isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Sample number required before accept — generate barcode first");
        }
        Timestamp nowTs = now();
        detail.setIsAccepted(true);
        detail.setAcceptanceReason(acceptanceReason);
        detail.setAcceptanceDate(nowTs);
        detail.setUpdatedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedDatetime(nowTs);
        detailRepository.save(detail);
        log.info("Accepted sample {} detail {} booking {}", detail.getSampleNo(), detailId, bookingId);
        return detail;
    }

    /** Reject a sample: IsRejected=true + rejectionReason (ported from sampleRejected / RejectResultDto). */
    @Transactional
    public PathOrderDetail rejectSample(long bookingId, long detailId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID", "rejectionReason is required");
        }
        PathOrderBooking booking = load(bookingId);
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (Boolean.TRUE.equals(detail.getIsVerified())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already verified; cannot reject");
        }
        if (Boolean.TRUE.equals(detail.getIsRejected())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already rejected");
        }
        if (detail.getSampleNo() == null || detail.getSampleNo().isBlank()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Sample number required before reject — generate barcode first");
        }
        Timestamp nowTs = now();
        detail.setIsRejected(true);
        detail.setRejectionReason(rejectionReason.trim());
        detail.setRejectionDate(nowTs);
        detail.setIsAccepted(false);
        detail.setUpdatedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedDatetime(nowTs);
        // rejection clears finalize/verify so results must be re-entered
        detail.setIsFinal(false);
        detail.setFinalizedAt(null);
        detail.setIsVerified(false);
        detail.setVerifiedAt(null);
        detailRepository.save(detail);
        log.info("Rejected sample {} detail {} booking {} reason={}", detail.getSampleNo(), detailId, bookingId, rejectionReason);
        return detail;
    }

    /** Double-verification: locks the finalized detail as verified (KDL verify parity). */
    @Transactional
    public PathOrderDetail verifyDetail(long bookingId, long detailId) {
        PathOrderBooking booking = load(bookingId);
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        if (!Boolean.TRUE.equals(detail.getIsFinal())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " must be finalized before verification");
        }
        if (Boolean.TRUE.equals(detail.getIsRejected())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Rejected detail cannot be verified");
        }
        if (Boolean.TRUE.equals(detail.getIsVerified())) {
            throw new InvalidLabRequestException("LAB_ILLEGAL_TRANSITION",
                    "Detail " + detailId + " is already verified");
        }
        Timestamp nowTs = now();
        detail.setIsVerified(true);
        detail.setVerifiedAt(nowTs);
        detail.setVerifiedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedBy(AUDIT_ACTOR_ID);
        detail.setUpdatedDatetime(nowTs);
        detailRepository.save(detail);

        // If ALL details of this booking are now verified -> emit verified event once
        boolean allVerified = detailsOf(bookingId).stream()
                .allMatch(d -> Boolean.TRUE.equals(d.getIsVerified()));
        if (allVerified) {
            emitLabResultVerified(booking, detailsOf(bookingId), nowTs);
        }
        log.info("Verified detail {} of booking {}", detailId, bookingId);
        return detail;
    }

    /** Critical alert notify: emits LabCriticalAlert for details with is_critical params (WhatsApp/SMS hook). */
    @Transactional
    public PathOrderDetail notifyCritical(long bookingId, long detailId) {
        PathOrderBooking booking = load(bookingId);
        PathOrderDetail detail = loadDetail(detailId);
        if (detail.getBookingId() != booking.getBookingId()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " does not belong to booking " + bookingId);
        }
        List<PathoPatientParameterDetails> params = parameterRepository.findByDetailIdOrderByParamIdAsc(detailId);
        List<PathoPatientParameterDetails> criticals = params.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsCritical())).toList();
        if (criticals.isEmpty()) {
            throw new InvalidLabRequestException("LAB_REQ_INVALID",
                    "Detail " + detailId + " has no critical values to notify");
        }
        Timestamp nowTs = now();
        for (PathoPatientParameterDetails p : criticals) {
            if (p.getCriticalNotifiedAt() == null) {
                p.setCriticalNotifiedAt(nowTs);
                p.setUpdatedBy(AUDIT_ACTOR_ID);
                p.setUpdatedDatetime(nowTs);
                parameterRepository.save(p);
            }
        }
        // mark header critical too
        if (!Boolean.TRUE.equals(booking.getIsCritical())) {
            booking.setIsCritical(true);
            booking.setUpdatedBy(AUDIT_ACTOR_ID);
            booking.setUpdatedDatetime(nowTs);
            bookingRepository.save(booking);
        }
        emitLabCriticalAlert(booking, detail, criticals, nowTs);
        return loadDetail(detailId);
    }

    // ponytail: critical detection mirrors legacy 6506-6511 (numeric value vs "min-max" range)
    static boolean isCriticalValue(String valueStr, String referenceRange) {
        if (valueStr == null || referenceRange == null) return false;
        if (!referenceRange.contains("-")) return false;
        String[] parts = referenceRange.trim().split("-");
        if (parts.length != 2) return false;
        try {
            double resultNum = Double.parseDouble(valueStr.trim());
            double rangeMin = Double.parseDouble(parts[0].trim());
            double rangeMax = Double.parseDouble(parts[1].trim());
            return resultNum < rangeMin || resultNum > rangeMax;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ outbox helpers

    void emitLabCriticalAlert(PathOrderBooking booking, PathOrderDetail detail,
                              List<PathoPatientParameterDetails> criticals, Timestamp at) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", String.valueOf(booking.getBookingId()));
        payload.put("detailId", String.valueOf(detail.getDetailId()));
        payload.put("patientId", String.valueOf(booking.getPatientId()));
        payload.put("encounterId", booking.getEncounterId() == null ? null : String.valueOf(booking.getEncounterId()));
        payload.put("barcode", detail.getBarcode());
        payload.put("sampleNo", detail.getSampleNo());
        payload.put("criticalParams", criticals.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("parameterName", p.getParameterName());
            m.put("value", p.getValue());
            m.put("referenceRange", p.getReferenceRange());
            return m;
        }).toList());
        payload.put("notifiedAt", at.toInstant().toString());
        String partitionKey = booking.getEncounterId() != null
                ? String.valueOf(booking.getEncounterId())
                : String.valueOf(booking.getPatientId());
        outboxService.append(OutboxPublisher.LabEvents.LAB_CRITICAL_ALERT, "labOrder",
                booking.getBookingId(), partitionKey, toJson(payload),
                "lab-critical-alert:" + detail.getDetailId() + ":" + at.getTime());
        log.info("Emitted LabCriticalAlert for booking {} detail {} ({} critical params)",
                booking.getBookingId(), detail.getDetailId(), criticals.size());
    }

    void emitLabResultVerified(PathOrderBooking booking, List<PathOrderDetail> details, Timestamp at) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", String.valueOf(booking.getBookingId()));
        payload.put("detailIds", details.stream().map(d -> String.valueOf(d.getDetailId())).toList());
        payload.put("patientId", String.valueOf(booking.getPatientId()));
        payload.put("encounterId", booking.getEncounterId() == null ? null : String.valueOf(booking.getEncounterId()));
        payload.put("status", "VERIFIED");
        payload.put("verifiedAt", at.toInstant().toString());
        String partitionKey = booking.getEncounterId() != null
                ? String.valueOf(booking.getEncounterId())
                : "no-encounter:" + booking.getBookingId();
        outboxService.append(OutboxPublisher.LabEvents.LAB_RESULT_VERIFIED, "labOrder",
                booking.getBookingId(), partitionKey, toJson(payload),
                "lab-result-verified:" + booking.getBookingId());
        log.info("Emitted LabResultVerified for booking {}", booking.getBookingId());
    }

    /**
     * E-05 LabResultFinalized — payload keys EXACTLY {bookingId, detailIds[],
     * patientId, encounterId, results[{parameterName,value,unit}], status,
     * finalizedAt}; partition key encounterId (all results of a visit ordered).
     * Only fired when the WHOLE booking finalizes (never per-detail).
     */
    void emitLabResultFinalized(PathOrderBooking booking,
                                List<PathOrderDetail> details,
                                Timestamp finalizedAt) {
        List<PathoPatientParameterDetails> params =
                parameterRepository.findByDetailIdInOrderByParamIdAsc(
                        details.stream().map(PathOrderDetail::getDetailId).toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("bookingId", String.valueOf(booking.getBookingId()));
        payload.put("detailIds", details.stream()
                .map(d -> String.valueOf(d.getDetailId())).toList());
        payload.put("patientId", String.valueOf(booking.getPatientId()));
        payload.put("encounterId", booking.getEncounterId() == null
                ? null : String.valueOf(booking.getEncounterId()));
        List<Map<String, Object>> results = new ArrayList<>();
        for (PathoPatientParameterDetails p : params) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("parameterName", p.getParameterName());
            r.put("value", p.getValue());
            r.put("unit", p.getUnit());
            results.add(r);
        }
        payload.put("results", results);
        payload.put("status", "FINALIZED");
        payload.put("finalizedAt", finalizedAt.toInstant().toString());

        String partitionKey = booking.getEncounterId() != null
                ? String.valueOf(booking.getEncounterId())
                : "no-encounter:" + booking.getBookingId();   // documented fallback when encounterId is null
        outboxService.append(OutboxPublisher.LabEvents.LAB_RESULT_FINALIZED, "labOrder",
                booking.getBookingId(), partitionKey, toJson(payload),
                "lab-result-finalized:" + booking.getBookingId());
        log.info("Emitted {} for booking {} ({} results)", OutboxPublisher.LabEvents.LAB_RESULT_FINALIZED,
                booking.getBookingId(), results.size());
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public PathOrderBooking get(long bookingId) {
        PathOrderBooking booking = load(bookingId);
        List<PathOrderDetail> details = detailsOf(bookingId);
        for (PathOrderDetail d : details) {
            d.setParameters(parameterRepository.findByDetailIdOrderByParamIdAsc(d.getDetailId()));
        }
        booking.setDetails(details);
        return booking;
    }

    @Transactional(readOnly = true)
    public List<PathOrderBooking> worklist(Short status, Long patientId) {
        List<PathOrderBooking> bookings = bookingRepository.search(status, patientId);
        for (PathOrderBooking b : bookings) {
            b.setDetails(detailsOf(b.getBookingId()));
        }
        return bookings;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> workload() {
        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (short s = PathOrderBooking.STATUS_ORDERED; s <= PathOrderBooking.STATUS_CANCELLED; s++) {
            counts.put(statusName(s), bookingRepository.countByBookingStatus(s));
        }
        long total = bookingRepository.count();
        counts.put("TOTAL", total);
        return counts;
    }

    @Transactional(readOnly = true)
    public List<PathOrderBooking> covidList() {
        // ponytail: in-memory filter on testName contains covid (case-insensitive); add DB query when volume grows
        List<PathOrderBooking> all = bookingRepository.findAll();
        List<PathOrderBooking> covid = new java.util.ArrayList<>();
        for (PathOrderBooking b : all) {
            List<PathOrderDetail> details = detailsOf(b.getBookingId());
            boolean hasCovid = details.stream()
                    .anyMatch(d -> d.getTestName() != null && d.getTestName().toLowerCase().contains("covid"));
            if (hasCovid) {
                b.setDetails(details);
                for (PathOrderDetail d : details) {
                    d.setParameters(parameterRepository.findByDetailIdOrderByParamIdAsc(d.getDetailId()));
                }
                covid.add(b);
            }
        }
        return covid;
    }

    // ------------------------------------------------------------------ helpers

    private PathOrderBooking load(long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Lab order booking not found: " + bookingId));
    }

    private PathOrderDetail loadDetail(long detailId) {
        return detailRepository.findById(detailId)
                .orElseThrow(() -> new LabEntityNotFoundException("NOT_FOUND",
                        "Patho order detail not found: " + detailId));
    }

    private List<PathOrderDetail> detailsOf(long bookingId) {
        return detailRepository.findByBookingIdOrderByDetailIdAsc(bookingId);
    }

    static String statusName(short s) {
        return switch (s) {
            case 1 -> "ORDERED";
            case 2 -> "SAMPLE_COLLECTED";
            case 3 -> "IN_PROGRESS";
            case 4 -> "FINALIZED";
            case 5 -> "CANCELLED";
            default -> String.valueOf(s);
        };
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_lab." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static Timestamp now() {
        return Timestamp.from(Instant.now());
    }

    /** One requested test line at booking creation. */
    public record TestLine(String testCode, String testName) {}

    /** One result-parameter row for upsert. */
    public record ResultRow(String parameterName, String value, String unit, String referenceRange) {}
}
