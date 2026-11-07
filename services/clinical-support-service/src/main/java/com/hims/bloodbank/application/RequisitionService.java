package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BagConflictException;
import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodCompatibilityGate;
import com.hims.bloodbank.domain.model.BloodCrossmatchResult;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.model.BloodRequisitionDetail;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodCrossmatchResultRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Requisition lifecycle (GAP-REGISTER WP-9, Catalog #13).
 *
 * BR-BLD-001 status machine — 1=new; 2=cross-matched when cumulative cross-matched
 * volume >= ordered volume (else stays 1); 3=issued when cumulative issued volume
 * >= ordered volume; 10=cancelled/expired. Cumulative volumes are recomputed from
 * the linked bag states on every mutation ({@link #recomputeStatus}), so a decross
 * naturally walks a requisition back from 2 to 1 and partial cross-match never
 * promotes past 1.
 *
 * Patient-safety cross-match gate:
 * 1. crossmatch() records a PENDING test result and reserves the bag.
 * 2. recordCrossmatchResult() validates ABO/Rh compatibility gate (Gate 2).
 *    Only COMPATIBLE promotes bag.crossMatched=true. Incompatible requests receive 409.
 *    INCOMPATIBLE releases the bag reservation.
 * 3. issue() requires an active, unexpired COMPATIBLE result for that bag, patient, and requisition.
 *
 * NOTE: NO BloodRequisitionCreated event is emitted on create — DEFERRED per
 * MICROSERVICE-CATALOG #13 / KAFKA-EVENT-CATALOG §3; do not add silently.
 *
 * NOTE: BR-BLD-004 expiry is an explicit POST /api/v1/jobs/expire-requisitions scan,
 * NOT a hidden read-side mutation inside get().
 */
@Service
public class RequisitionService {

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    /** Legacy literal reason written by the BR-BLD-004 expiry scan (detail level). */
    public static final String ORDER_EXPIRED_REASON = "Order is expired";

    private final BloodRequisitionRepository requisitionRepository;
    private final BloodRequisitionDetailRepository detailRepository;
    private final BloodBagRepository bagRepository;
    private final BloodCrossmatchResultRepository crossmatchResultRepository;
    private final ClockHolder clocks;

    @PersistenceContext
    private EntityManager em;

    public RequisitionService(BloodRequisitionRepository requisitionRepository,
                              BloodRequisitionDetailRepository detailRepository,
                              BloodBagRepository bagRepository,
                              BloodCrossmatchResultRepository crossmatchResultRepository,
                              ClockHolder clocks) {
        this.requisitionRepository = requisitionRepository;
        this.detailRepository = detailRepository;
        this.bagRepository = bagRepository;
        this.crossmatchResultRepository = crossmatchResultRepository;
        this.clocks = clocks;
    }

    // ------------------------------------------------------------------ create / read

    @Transactional
    public BloodRequisition create(Long patientId, String bloodGroup, Integer orderedVolumeMl,
                                   Timestamp requesteddate, Integer orderValidationHours,
                                   List<Map<String, Object>> components) {
        if (patientId == null) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "patientId is required");
        }
        if (bloodGroup == null || bloodGroup.isBlank()) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "bloodGroup is required");
        }
        if (orderedVolumeMl == null || orderedVolumeMl <= 0) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID",
                    "orderedVolumeMl must be a positive integer");
        }
        BloodRequisition r = new BloodRequisition();
        r.setRequisitionId(nextId("t_bloodrequisition_seq"));
        r.setPatientId(patientId);
        r.setBloodGroup(bloodGroup);
        r.setOrderedVolumeMl(orderedVolumeMl);
        r.setRequesteddate(requesteddate != null ? requesteddate
                : Timestamp.from(clocks.clock().instant()));
        if (orderValidationHours != null && orderValidationHours > 0) {
            r.setOrderValidationHours(orderValidationHours);
        }
        stampAudit(r);
        r = requisitionRepository.save(r);

        if (components != null && !components.isEmpty()) {
            int total = 0;
            for (Map<String, Object> c : components) {
                String component = str(c.get("component"));
                Integer volume = c.get("volumeMl") instanceof Number n ? n.intValue() : null;
                if (component == null || component.isBlank() || volume == null || volume <= 0) {
                    throw new InvalidBloodBankRequestException("BLD_REQ_INVALID",
                            "each component needs component + positive volumeMl");
                }
                total += volume;
                BloodRequisitionDetail d = new BloodRequisitionDetail();
                d.setDetailId(nextId("t_bloodrequisitiondetail_seq"));
                d.setRequisitionId(r.getRequisitionId());
                d.setComponent(component);
                d.setVolumeMl(volume);
                d.setRequisitionStatus(BloodRequisition.STATUS_NEW);
                stampAudit(d);
                detailRepository.save(d);
            }
            if (total > orderedVolumeMl) {
                throw new InvalidBloodBankRequestException("BLD_REQ_INVALID",
                        "component volumes (" + total + "ml) exceed orderedVolumeMl");
            }
        }
        return r;
    }

    @Transactional(readOnly = true)
    public BloodRequisition get(long requisitionId) {
        return reqOrThrow(requisitionId);
    }

    @Transactional(readOnly = true)
    public List<BloodRequisition> list(Integer status, Long patientId) {
        if (status != null) {
            return requisitionRepository.findByStatus(status);
        }
        if (patientId != null) {
            return requisitionRepository.findByPatientId(patientId);
        }
        return requisitionRepository.findAll();
    }

    /** Full view: header + details + linked bags + crossmatch results (read-only — BR-BLD-004 stays in its job). */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(long requisitionId) {
        BloodRequisition r = reqOrThrow(requisitionId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("requisition", r);
        view.put("details", detailRepository.findByRequisitionId(requisitionId));
        view.put("bags", bagRepository.findByRequisitionId(requisitionId));
        view.put("crossmatchResults", crossmatchResultRepository.findByRequisitionId(requisitionId));
        view.put("crossmatchedVolumeMl", crossmatchedVolume(requisitionId));
        view.put("issuedVolumeMl", issuedVolume(requisitionId));
        return view;
    }

    // ------------------------------------------------------------------ crossmatch / issue / cancel

    /**
     * Cross-match initiation: records a PENDING result and reserves the bag onto the requisition.
     * Bag is reserved (requisitionId linked), but crossMatched remains false until COMPATIBLE result recorded.
     */
    @Transactional
    public BloodCrossmatchResult crossmatch(long requisitionId, long bagId, String method, String remarks) {
        BloodRequisition r = reqOrThrow(requisitionId);
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " is cancelled/expired");
        }
        BloodBag b = bagOrThrow(bagId);
        if (b.getIssued()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " already issued");
        }
        if (b.getIsbagexpired()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " is expired");
        }
        if (b.getCrossMatched()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " already crossmatched");
        }
        if (b.getRequisitionId() != null && b.getRequisitionId() != requisitionId) {
            throw new BagConflictException("BLD_BAG_CONFLICT",
                    "Bag " + bagId + " is already reserved for requisition " + b.getRequisitionId());
        }

        // Reserve the bag for this requisition
        b.setRequisitionId(requisitionId);
        b.setCrossMatched(false);
        stampAudit(b);
        bagRepository.save(b);

        Long detailId = detailRepository.findByRequisitionId(requisitionId).stream()
                .findFirst()
                .map(BloodRequisitionDetail::getDetailId)
                .orElse(null);

        // Record PENDING result record
        BloodCrossmatchResult resultRecord = new BloodCrossmatchResult();
        resultRecord.setId(nextId("t_bloodcrossmatchingresult_seq"));
        resultRecord.setRequisitionId(requisitionId);
        resultRecord.setRequisitionDetailId(detailId);
        resultRecord.setBagId(bagId);
        resultRecord.setPatientId(r.getPatientId());
        resultRecord.setMethod(method != null ? method : "Standard Crossmatch");
        resultRecord.setResult(BloodCrossmatchResult.RESULT_PENDING);
        resultRecord.setRemarks(remarks);
        resultRecord.setIsDecrossed(false);
        stampAudit(resultRecord);
        resultRecord = crossmatchResultRepository.save(resultRecord);

        return resultRecord;
    }

    /**
     * Cross-match convenience overload.
     */
    @Transactional
    public BloodCrossmatchResult crossmatch(long requisitionId, long bagId) {
        return crossmatch(requisitionId, bagId, null, null);
    }

    /**
     * Records compatibility test result (POST /api/v1/requisitions/{id}/crossmatch/{resultId}/result).
     *
     * Gate 2 ABO/Rh check:
     * - COMPATIBLE requires bag blood group to be compatible with patient blood group for that component.
     *   If incompatible, throws 409 BagConflictException ("BLD_INCOMPATIBLE").
     * - On COMPATIBLE, bag.crossMatched becomes true and cumulative volume recomputes.
     * - On INCOMPATIBLE, bag reservation is released (requisition link and crossMatched cleared).
     */
    @Transactional
    public BloodCrossmatchResult recordCrossmatchResult(long requisitionId, long resultId,
                                                        String resultOutcome, String tester,
                                                        String remarks, String method) {
        BloodRequisition r = reqOrThrow(requisitionId);
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " is cancelled/expired");
        }
        BloodCrossmatchResult record = crossmatchResultRepository.findById(resultId)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("BLD_NOT_FOUND",
                        "Crossmatch result " + resultId + " not found"));

        if (record.getRequisitionId() != requisitionId) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Crossmatch result " + resultId + " does not belong to requisition " + requisitionId);
        }
        if (!BloodCrossmatchResult.RESULT_PENDING.equalsIgnoreCase(record.getResult())) {
            throw new BagConflictException("BLD_STATE",
                    "Crossmatch result " + resultId + " is already recorded as " + record.getResult());
        }
        if (Boolean.TRUE.equals(record.getIsDecrossed())) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Crossmatch result " + resultId + " has already been decrossed");
        }

        if (resultOutcome == null || resultOutcome.isBlank()) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID", "result is required");
        }
        String normalizedOutcome = resultOutcome.trim().toUpperCase(Locale.ROOT);
        if (!BloodCrossmatchResult.RESULT_COMPATIBLE.equals(normalizedOutcome)
                && !BloodCrossmatchResult.RESULT_INCOMPATIBLE.equals(normalizedOutcome)) {
            throw new InvalidBloodBankRequestException("BLD_REQ_INVALID",
                    "result must be COMPATIBLE or INCOMPATIBLE, got: " + resultOutcome);
        }

        BloodBag bag = bagOrThrow(record.getBagId());
        Timestamp now = Timestamp.from(clocks.clock().instant());

        if (BloodCrossmatchResult.RESULT_COMPATIBLE.equals(normalizedOutcome)) {
            // Determine component from detail if present
            String component = null;
            if (record.getRequisitionDetailId() != null) {
                component = detailRepository.findById(record.getRequisitionDetailId())
                        .map(BloodRequisitionDetail::getComponent)
                        .orElse(null);
            }
            if (component == null) {
                component = detailRepository.findByRequisitionId(requisitionId).stream()
                        .findFirst()
                        .map(BloodRequisitionDetail::getComponent)
                        .orElse(null);
            }

            if (component == null || component.isBlank()) {
                throw new BagConflictException("BLD_COMPONENT_UNKNOWN",
                        "Cannot record COMPATIBLE: blood component is unknown for requisition " + requisitionId);
            }

            // ABO/Rh Gate check
            boolean compatible = BloodCompatibilityGate.isCompatible(r.getBloodGroup(), bag.getBloodGroup(), component);
            if (!compatible) {
                throw new BagConflictException("BLD_INCOMPATIBLE",
                        "Incompatible blood group: Patient (" + r.getBloodGroup()
                                + ") cannot receive Donor Bag (" + bag.getBloodGroup()
                                + ") for component " + component);
            }

            record.setResult(BloodCrossmatchResult.RESULT_COMPATIBLE);
            record.setTestDate(now);
            record.setTestedBy(tester);
            if (remarks != null) record.setRemarks(remarks);
            if (method != null) record.setMethod(method);
            stampAudit(record);
            record = crossmatchResultRepository.save(record);

            bag.setCrossMatched(true);
            bag.setCrossMatchedTestDate(now);
            stampAudit(bag);
            bagRepository.save(bag);

            recomputeStatus(r);
        } else {
            // INCOMPATIBLE releases the bag reservation
            record.setResult(BloodCrossmatchResult.RESULT_INCOMPATIBLE);
            record.setTestDate(now);
            record.setTestedBy(tester);
            if (remarks != null) record.setRemarks(remarks);
            if (method != null) record.setMethod(method);
            stampAudit(record);
            record = crossmatchResultRepository.save(record);

            bag.setCrossMatched(false);
            bag.setCrossMatchedTestDate(null);
            bag.setRequisitionId(null);
            stampAudit(bag);
            bagRepository.save(bag);

            recomputeStatus(r);
        }

        return record;
    }

    /**
     * Issue a bag:
     * - Requires an active, unexpired COMPATIBLE crossmatch result for that bag, patient, and requisition.
     * - Bag and crossmatch must not be expired at issue time.
     * - Cumulative issued volume >= ordered promotes requisition to status 3.
     */
    @Transactional
    public BloodBag issue(long requisitionId, long bagId) {
        BloodRequisition r = reqOrThrow(requisitionId);
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " is cancelled/expired");
        }
        BloodBag b = bagOrThrow(bagId);
        if (b.getIssued()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " already issued");
        }

        // Bag expiry gate at issue time (BR-BLD-003)
        LocalDate today = LocalDate.ofInstant(clocks.clock().instant(), clocks.clock().getZone());
        if (b.getIsbagexpired() || !b.getExpirydate().isAfter(today)) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " is expired");
        }

        // Bag crossmatch flag check
        if (!b.getCrossMatched() || !requisitionIdEquals(b, requisitionId)) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Bag " + bagId + " is not crossmatched against requisition " + requisitionId);
        }

        // Crossmatch result record check: must have a COMPATIBLE, non-decrossed result
        Optional<BloodCrossmatchResult> matchResult = crossmatchResultRepository
                .findFirstByBagIdAndRequisitionIdAndPatientIdAndResultAndIsDecrossedFalse(
                        bagId, requisitionId, r.getPatientId(), BloodCrossmatchResult.RESULT_COMPATIBLE);

        if (matchResult.isEmpty()) {
            List<BloodCrossmatchResult> anyResults = crossmatchResultRepository.findByBagId(bagId);
            String reason = "No COMPATIBLE crossmatch result found for bag " + bagId + " on requisition " + requisitionId;
            for (BloodCrossmatchResult res : anyResults) {
                if (res.getRequisitionId() != requisitionId || !res.getPatientId().equals(r.getPatientId())) {
                    reason = "Bag " + bagId + " crossmatch belongs to different patient or requisition";
                } else if (BloodCrossmatchResult.RESULT_PENDING.equalsIgnoreCase(res.getResult())) {
                    reason = "Bag " + bagId + " crossmatch result is PENDING";
                } else if (BloodCrossmatchResult.RESULT_INCOMPATIBLE.equalsIgnoreCase(res.getResult())) {
                    reason = "Bag " + bagId + " crossmatch result is INCOMPATIBLE";
                } else if (Boolean.TRUE.equals(res.getIsDecrossed())) {
                    reason = "Bag " + bagId + " crossmatch has been decrossed";
                }
            }
            throw new InvalidBloodBankRequestException("BLD_STATE", reason);
        }

        // Crossmatch test expiry check (48h validity per BR-BLD-002)
        BloodCrossmatchResult validResult = matchResult.get();
        Timestamp testDate = validResult.getTestDate();
        if (testDate == null) {
            testDate = b.getCrossMatchedTestDate();
        }
        if (testDate != null) {
            Instant cutoff = clocks.clock().instant().minus(Duration.ofHours(48));
            if (testDate.toInstant().isBefore(cutoff)) {
                throw new InvalidBloodBankRequestException("BLD_STATE",
                        "Crossmatch test for bag " + bagId + " is expired (>48 hours)");
            }
        }

        b.setIssued(true);
        b.setIssuedRequisitionId(requisitionId);
        stampAudit(b);
        b = bagRepository.save(b);

        recomputeStatus(r);   // cumulative issued >= ordered -> 3 else stays current
        return b;
    }

    /** Cancel: status 1|2 -> 10; unissued crossmatched bags are released and crossmatch results decrossed. */
    @Transactional
    public BloodRequisition cancel(long requisitionId) {
        BloodRequisition r = reqOrThrow(requisitionId);
        if (r.getStatus() == BloodRequisition.STATUS_ISSUED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " already fully issued");
        }
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " already cancelled/expired");
        }
        for (BloodBag b : bagRepository.findByRequisitionId(requisitionId)) {
            if (!b.getIssued()) {           // release reservations like the legacy cancel path
                b.setCrossMatched(false);
                b.setCrossMatchedTestDate(null);
                b.setRequisitionId(null);
                bagRepository.save(b);
            }
        }
        // Mark crossmatch results as decrossed
        for (BloodCrossmatchResult res : crossmatchResultRepository.findByRequisitionId(requisitionId)) {
            if (!Boolean.TRUE.equals(res.getIsDecrossed())) {
                res.setIsDecrossed(true);
                res.setUpdatedBy(AUDIT_ACTOR_ID);
                res.setUpdatedDatetime(Timestamp.from(clocks.clock().instant()));
                crossmatchResultRepository.save(res);
            }
        }
        r.setStatus(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        mirrorDetails(requisitionId, BloodRequisition.STATUS_CANCELLED_EXPIRED, false, null);
        r.setUpdatedBy(AUDIT_ACTOR_ID);
        r.setUpdatedDatetime(Timestamp.from(clocks.clock().instant()));
        return requisitionRepository.save(r);
    }

    // ------------------------------------------------------------------ read endpoints for crossmatch results

    @Transactional(readOnly = true)
    public List<BloodCrossmatchResult> getCrossmatchResults(long requisitionId) {
        reqOrThrow(requisitionId);
        return crossmatchResultRepository.findByRequisitionId(requisitionId);
    }

    @Transactional(readOnly = true)
    public BloodCrossmatchResult getCrossmatchResult(long resultId) {
        return crossmatchResultRepository.findById(resultId)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("BLD_NOT_FOUND",
                        "Crossmatch result " + resultId + " not found"));
    }

    // ------------------------------------------------------------------ status machine core

    /**
     * Recompute BR-BLD-001 status from linked bag state:
     *   issued volume >= ordered -> 3; else crossmatched (unissued, unexpired) volume
     *   >= ordered -> 2; else 1. Terminal status 10 is never overwritten.
     */
    public void recomputeStatus(BloodRequisition r) {
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            return;
        }
        int cm = crossmatchedVolume(r.getRequisitionId());
        int iss = issuedVolume(r.getRequisitionId());
        int next;
        if (iss >= r.getOrderedVolumeMl()) {
            next = BloodRequisition.STATUS_ISSUED;
        } else if (cm >= r.getOrderedVolumeMl()) {
            next = BloodRequisition.STATUS_CROSS_MATCHED;
        } else {
            next = BloodRequisition.STATUS_NEW;
        }
        if (next != r.getStatus()) {
            r.setStatus(next);
            r.setUpdatedBy(AUDIT_ACTOR_ID);
            r.setUpdatedDatetime(Timestamp.from(Instant.now()));
            requisitionRepository.save(r);
        } else {
            requisitionRepository.save(r);
        }
    }

    private void mirrorDetails(long requisitionId, int statusCode, boolean expiredFlag, String reason) {
        for (BloodRequisitionDetail d : detailRepository.findByRequisitionId(requisitionId)) {
            d.setRequisitionStatus(statusCode);
            if (expiredFlag) {
                d.setIsorderexpired(true);
            }
            if (reason != null) {
                d.setExpiryReason(reason);
            }
            d.setUpdatedBy(AUDIT_ACTOR_ID);
            d.setUpdatedDatetime(Timestamp.from(Instant.now()));
            detailRepository.save(d);
        }
    }

    /** Cumulative cross-matched volume linked to this requisition (unissued, unexpired bags with crossMatched=true). */
    public int crossmatchedVolume(long requisitionId) {
        return bagRepository.findByRequisitionId(requisitionId).stream()
                .filter(b -> b.getCrossMatched() && !b.getIsbagexpired())
                .mapToInt(BloodBag::getVolumeMl)
                .sum();
    }

    /** Cumulative issued volume linked to this requisition. */
    public int issuedVolume(long requisitionId) {
        return bagRepository.findByRequisitionId(requisitionId).stream()
                .filter(BloodBag::getIssued)
                .mapToInt(BloodBag::getVolumeMl)
                .sum();
    }

    // ------------------------------------------------------------------ helpers

    private static boolean requisitionIdEquals(BloodBag b, long requisitionId) {
        return b.getRequisitionId() != null && b.getRequisitionId() == requisitionId;
    }

    private BloodRequisition reqOrThrow(long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("BLD_NOT_FOUND",
                        "Requisition " + id + " not found"));
    }

    private BloodBag bagOrThrow(long id) {
        return bagRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("BLD_NOT_FOUND",
                        "Bag " + id + " not found"));
    }

    void stampAudit(Object entity) {
        Timestamp now = Timestamp.from(clocks.clock().instant());
        if (entity instanceof BloodRequisition e) {
            e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now);
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        } else if (entity instanceof BloodRequisitionDetail e) {
            e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now);
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        } else if (entity instanceof BloodBag e) {
            if (e.getAddedDatetime() == null) { e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now); }
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        } else if (entity instanceof BloodCrossmatchResult e) {
            if (e.getAddedDatetime() == null) { e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now); }
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        }
    }

    private Long nextId(String sequenceName) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('ihms_bloodbank." + sequenceName + "')")
                .getSingleResult();
        return n.longValue();
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
