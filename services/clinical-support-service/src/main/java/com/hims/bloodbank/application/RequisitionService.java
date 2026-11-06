package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BagConflictException;
import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodRequisition;
import com.hims.bloodbank.domain.model.BloodRequisitionDetail;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionDetailRepository;
import com.hims.bloodbank.domain.repository.BloodRequisitionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final ClockHolder clocks;

    @PersistenceContext
    private EntityManager em;

    public RequisitionService(BloodRequisitionRepository requisitionRepository,
                              BloodRequisitionDetailRepository detailRepository,
                              BloodBagRepository bagRepository,
                              ClockHolder clocks) {
        this.requisitionRepository = requisitionRepository;
        this.detailRepository = detailRepository;
        this.bagRepository = bagRepository;
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

    /** Full view: header + details + linked bags (read-only — BR-BLD-004 stays in its job). */
    @Transactional(readOnly = true)
    public Map<String, Object> detail(long requisitionId) {
        BloodRequisition r = reqOrThrow(requisitionId);
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("requisition", r);
        view.put("details", detailRepository.findByRequisitionId(requisitionId));
        view.put("bags", bagRepository.findByRequisitionId(requisitionId));
        view.put("crossmatchedVolumeMl", crossmatchedVolume(requisitionId));
        view.put("issuedVolumeMl", issuedVolume(requisitionId));
        return view;
    }

    // ------------------------------------------------------------------ crossmatch / issue / cancel

    /** Cross-match a bag onto the requisition (BR-BLD-001 cumulative-volume rule). */
    @Transactional
    public BloodBag crossmatch(long requisitionId, long bagId) {
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
        b.setCrossMatched(true);
        b.setCrossMatchedTestDate(Timestamp.from(clocks.clock().instant()));
        b.setRequisitionId(requisitionId);
        stampAudit(b);
        b = bagRepository.save(b);

        recomputeStatus(r);   // partial -> stays 1; cumulative >= ordered -> 2
        return b;
    }

    /** Issue a bag: only crossmatched bags are issuable (400 otherwise). */
    @Transactional
    public BloodBag issue(long requisitionId, long bagId) {
        BloodRequisition r = reqOrThrow(requisitionId);
        if (r.getStatus() == BloodRequisition.STATUS_CANCELLED_EXPIRED) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Requisition " + requisitionId + " is cancelled/expired");
        }
        BloodBag b = bagOrThrow(bagId);
        if (!b.getCrossMatched() || !requisitionIdEquals(b, requisitionId)) {
            throw new InvalidBloodBankRequestException("BLD_STATE",
                    "Bag " + bagId + " is not crossmatched against requisition " + requisitionId);
        }
        if (b.getIssued()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " already issued");
        }
        if (b.getIsbagexpired()) {
            throw new BagConflictException("BLD_BAG_CONFLICT", "Bag " + bagId + " is expired");
        }
        b.setIssued(true);
        b.setIssuedRequisitionId(requisitionId);
        stampAudit(b);
        b = bagRepository.save(b);

        recomputeStatus(r);   // cumulative issued >= ordered -> 3 else stays current
        return b;
    }

    /** Cancel: status 1|2 -> 10; unissued crossmatched bags are released. */
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
        r.setStatus(BloodRequisition.STATUS_CANCELLED_EXPIRED);
        mirrorDetails(requisitionId, BloodRequisition.STATUS_CANCELLED_EXPIRED, false, null);
        r.setUpdatedBy(AUDIT_ACTOR_ID);
        r.setUpdatedDatetime(Timestamp.from(Instant.now()));
        return requisitionRepository.save(r);
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

    /** Cumulative cross-matched volume linked to this requisition (unissued, unexpired bags). */
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
        Timestamp now = Timestamp.from(Instant.now());
        if (entity instanceof BloodRequisition e) {
            e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now);
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        } else if (entity instanceof BloodRequisitionDetail e) {
            e.setAddedBy(AUDIT_ACTOR_ID); e.setAddedDatetime(now);
            e.setUpdatedBy(AUDIT_ACTOR_ID); e.setUpdatedDatetime(now);
        } else if (entity instanceof BloodBag e) {
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
