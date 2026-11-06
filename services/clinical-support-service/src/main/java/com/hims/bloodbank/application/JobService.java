package com.hims.bloodbank.application;

import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodDonor;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodDonorRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One-shot job endpoints for the legacy daily crons (ApplicationJobs.java:769-848,
 * 1045-1061). Every method is IDEMPOTENT: re-running it on settled data is a no-op
 * returning zero counts. CRON WIRING DEFERRED — the orchestrator invokes these
 * endpoints; do not add @Scheduled here without an explicit decision.
 *
 * BR-BLD-002 (daily 00:00): crossmatched & unissued bags whose crossMatchedTestDate
 *   is >= 48h old are decrossed; affected requisitions recompute toward 1.
 * BR-BLD-003 (daily 23:00): bags with expirydate = today become isbagexpired=true
 *   unless issued; crossmatched unissued expired bags also decross + requisition recalc.
 * BR-BLD-004: requisitions past requesteddate + orderValidationPeriod(h) without a
 *   collected sample and still status=1 -> status=10, order_expired=true,
 *   detail reason 'Order is expired'.
 * BR-BLD-005: donors in DNR_NEW(1)/DNR_FIT(2) untouched for >= 72h -> donationStatus=7.
 */
@Service
public class JobService {

    public static final long AUDIT_ACTOR_ID = 1L;

    private final BloodBagRepository bagRepository;
    private final BloodRequisitionRepository requisitionRepository;
    private final BloodRequisitionDetailRepository detailRepository;
    private final BloodDonorRepository donorRepository;
    private final RequisitionService requisitionService;
    private final ClockHolder clocks;
    private final int defaultValidationHours;

    public JobService(BloodBagRepository bagRepository,
                      BloodRequisitionRepository requisitionRepository,
                      BloodRequisitionDetailRepository detailRepository,
                      BloodDonorRepository donorRepository,
                      RequisitionService requisitionService,
                      ClockHolder clocks,
                      @Value("${app.bloodbank.order-validation-hours:72}") int defaultValidationHours) {
        this.bagRepository = bagRepository;
        this.requisitionRepository = requisitionRepository;
        this.detailRepository = detailRepository;
        this.donorRepository = donorRepository;
        this.requisitionService = requisitionService;
        this.clocks = clocks;
        this.defaultValidationHours = defaultValidationHours;
    }

    /**
     * BR-BLD-002 + BR-BLD-003 combined sweep. Returns summary counts, e.g.
     * {agedDecrossed, bagsExpired, expiredDecrossed, requisitionsRecalculated}.
     */
    @Transactional
    public Map<String, Object> decrossExpired() {
        Instant now = clocks.clock().instant();
        Set<Long> touchedRequisitions = new HashSet<>();
        int agedDecrossed = 0;
        int bagsExpired = 0;
        int expiredDecrossed = 0;

        // ---- BR-BLD-002: 48h decross timer (inclusive boundary: exactly 2880 min counts)
        Timestamp cutoff = Timestamp.from(now.minus(Duration.ofHours(48)));
        List<BloodBag> aged = bagRepository
                .findByCrossMatchedTrueAndIssuedFalseAndIsbagexpiredFalseAndCrossMatchedTestDateLessThanEqual(cutoff);
        for (BloodBag b : aged) {
            Long reqId = b.getRequisitionId();   // capture BEFORE decross clears the link
            decross(b);
            agedDecrossed++;
            if (reqId != null) {
                touchedRequisitions.add(reqId);
            }
        }

        // ---- BR-BLD-003: expirydate = today -> isbagexpired=true unless issued
        LocalDate today = LocalDate.ofInstant(now, clocks.clock().getZone());
        List<BloodBag> expiring = bagRepository.findByExpirydateEqualsAndIssuedFalseAndIsbagexpiredFalse(today);
        for (BloodBag b : expiring) {
            b.setIsbagexpired(true);
            b.setUpdatedBy(AUDIT_ACTOR_ID);
            b.setUpdatedDatetime(Timestamp.from(Instant.now()));
            bagRepository.save(b);
            bagsExpired++;
            if (b.getCrossMatched()) {
                Long reqId = b.getRequisitionId();   // capture BEFORE decross clears the link
                decross(b);          // crossmatched unissued expired -> decross
                expiredDecrossed++;
                if (reqId != null) {
                    touchedRequisitions.add(reqId);
                }
            }
        }

        // ---- requisition status recalc toward 1 (never touches 10; 3 only holds while issued volume suffices)
        for (Long reqId : touchedRequisitions) {
            requisitionRepository.findById(reqId).ifPresent(requisitionService::recomputeStatus);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("agedDecrossed", agedDecrossed);
        summary.put("bagsExpired", bagsExpired);
        summary.put("expiredDecrossed", expiredDecrossed);
        summary.put("requisitionsRecalculated", touchedRequisitions.size());
        return summary;
    }

    /**
     * BR-BLD-004 scan. validationHours overrides the per-requisition snapshot when given;
     * predicate: requesteddate + hours <= now AND NOT sample_collected AND status=1.
     */
    @Transactional
    public Map<String, Object> expireRequisitions(Integer validationHours) {
        Instant now = clocks.clock().instant();
        int expiredCount = 0;
        int scanned = 0;
        for (BloodRequisition r : requisitionRepository.findByStatus(BloodRequisition.STATUS_NEW)) {
            scanned++;
            int hours = validationHours != null && validationHours > 0
                    ? validationHours
                    : (r.getOrderValidationHours() != null ? r.getOrderValidationHours() : defaultValidationHours);
            Timestamp cutoff = Timestamp.from(r.getRequesteddate().toInstant().plus(Duration.ofHours(hours)));
            boolean due = !Timestamp.from(now).before(cutoff)   // requesteddate + hours <= now
                    && !r.getSampleCollected();
            if (due) {
                r.setStatus(BloodRequisition.STATUS_CANCELLED_EXPIRED);
                r.setOrderExpired(true);
                r.setUpdatedBy(AUDIT_ACTOR_ID);
                r.setUpdatedDatetime(Timestamp.from(Instant.now()));
                requisitionRepository.save(r);
                for (var d : detailRepository.findByRequisitionId(r.getRequisitionId())) {
                    d.setRequisitionStatus(BloodRequisition.STATUS_CANCELLED_EXPIRED);
                    d.setIsorderexpired(true);
                    d.setExpiryReason(RequisitionService.ORDER_EXPIRED_REASON);   // legacy literal
                    d.setUpdatedBy(AUDIT_ACTOR_ID);
                    d.setUpdatedDatetime(Timestamp.from(Instant.now()));
                    detailRepository.save(d);
                }
                expiredCount++;
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scanned", scanned);
        summary.put("expired", expiredCount);
        return summary;
    }

    /** BR-BLD-005: DNR_NEW/DNR_FIT donors untouched for >= 72h -> donationStatus=7. */
    @Transactional
    public Map<String, Object> cancelStaleDonors() {
        Instant now = clocks.clock().instant();
        Timestamp cutoff = Timestamp.from(now.minus(Duration.ofHours(72)));   // inclusive boundary
        List<BloodDonor> stale = donorRepository.findByDonationStatusInAndLastActionAtLessThanEqual(
                java.util.List.of(BloodDonor.DNR_NEW, BloodDonor.DNR_FIT), cutoff);
        int cancelled = 0;
        for (BloodDonor d : stale) {
            d.setDonationStatus(BloodDonor.DNR_CANCELLATION);
            d.setUpdatedBy(AUDIT_ACTOR_ID);
            d.setUpdatedDatetime(Timestamp.from(Instant.now()));
            donorRepository.save(d);
            cancelled++;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scanned", stale.size());
        summary.put("cancelled", cancelled);
        return summary;
    }

    /** Decross a bag: crosSMatched=false, test date + link cleared (legacy reason=0 path). */
    private void decross(BloodBag b) {
        b.setCrossMatched(false);
        b.setCrossMatchedTestDate(null);
        b.setRequisitionId(null);
        b.setUpdatedBy(AUDIT_ACTOR_ID);
        b.setUpdatedDatetime(Timestamp.from(Instant.now()));
        bagRepository.save(b);
    }
}
