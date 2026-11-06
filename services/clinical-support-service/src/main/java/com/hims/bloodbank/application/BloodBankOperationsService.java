package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBag;
import com.hims.bloodbank.domain.model.BloodBagDiscard;
import com.hims.bloodbank.domain.model.BloodBagReturn;
import com.hims.bloodbank.domain.model.BloodBagTrail;
import com.hims.bloodbank.domain.model.BloodBankCrossUnit;
import com.hims.bloodbank.domain.model.BloodBankIndent;
import com.hims.bloodbank.domain.repository.BloodBagDiscardRepository;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodBagReturnRepository;
import com.hims.bloodbank.domain.repository.BloodBagTrailRepository;
import com.hims.bloodbank.domain.repository.BloodBankCrossUnitRepository;
import com.hims.bloodbank.domain.repository.BloodBankIndentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * M17 blood bank lifecycle extensions:
 *   * bag trail (append-only audit log per bag)
 *   * bag return (return-to-supplier / quarantine)
   *   * bag discard (write-off)
 *   * blood-bank indent (inter-store component request)
 *   * cross-unit orders (inter-blood-bank transfer)
 */
@Service
public class BloodBankOperationsService {
    private static final Logger log = LoggerFactory.getLogger(BloodBankOperationsService.class);
    private static final long ACTOR = 1L; // MIG-004 principal id

    private final BloodBagRepository bagRepository;
    private final BloodBagTrailRepository trailRepository;
    private final BloodBagReturnRepository returnRepository;
    private final BloodBagDiscardRepository discardRepository;
    private final BloodBankIndentRepository indentRepository;
    private final BloodBankCrossUnitRepository crossUnitRepository;

    public BloodBankOperationsService(BloodBagRepository bagRepository,
                                     BloodBagTrailRepository trailRepository,
                                     BloodBagReturnRepository returnRepository,
                                     BloodBagDiscardRepository discardRepository,
                                     BloodBankIndentRepository indentRepository,
                                     BloodBankCrossUnitRepository crossUnitRepository) {
        this.bagRepository = bagRepository;
        this.trailRepository = trailRepository;
        this.returnRepository = returnRepository;
        this.discardRepository = discardRepository;
        this.indentRepository = indentRepository;
        this.crossUnitRepository = crossUnitRepository;
    }

    // ============================================================ bag trail

    /** Append a single audit row for a bag event. */
    @Transactional
    public BloodBagTrail recordTrail(long bagId, short eventType, Long actorId,
                                     String refDocType, Long refDocId, String remarks) {
        if (!bagRepository.existsById(bagId)) {
            throw new BloodBankEntityNotFoundException("NOT_FOUND", "Bag not found: " + bagId);
        }
        if (eventType < 1 || eventType > 12) {
            throw new InvalidBloodBankRequestException("BEXA003", "Unknown eventType: " + eventType);
        }
        BloodBagTrail row = new BloodBagTrail();
        row.setId(trailRepository.nextId());
        row.setBagId(bagId);
        row.setEventType(eventType);
        row.setEventAt(Timestamp.from(Instant.now()));
        row.setActorId(actorId);
        row.setRefDocType(refDocType);
        row.setRefDocId(refDocId);
        row.setRemarks(remarks);
        row.setAddedBy(ACTOR);
        row.setAddedDatetime(Timestamp.from(Instant.now()));
        row.setSynchronizedd(false);
        return trailRepository.save(row);
    }

    @Transactional(readOnly = true)
    public List<BloodBagTrail> trail(long bagId) {
        return trailRepository.findByBagIdOrderByEventAtAsc(bagId);
    }

    // ============================================================ bag return

    @Transactional
    public BloodBagReturn raiseReturn(long bagId, String reason, String returnedTo) {
        requireBag(bagId);
        if (reason == null || reason.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "reason is required");
        }
        long id = returnRepository.nextId();
        BloodBagReturn ret = new BloodBagReturn();
        ret.setId(id);
        ret.setReturnNo("BGR-" + id);
        ret.setBagId(bagId);
        ret.setReason(reason);
        ret.setReturnedTo(returnedTo);
        ret.setDocStatus(BloodBagReturn.STATUS_PENDING);
        ret.setAddedBy(ACTOR);
        ret.setAddedDatetime(Timestamp.from(Instant.now()));
        ret.setSynchronizedd(false);
        returnRepository.save(ret);
        recordTrail(bagId, BloodBagTrail.EVENT_RETURNED, ACTOR, "BLOODBAG_RETURN", ret.getId(), reason);
        return ret;
    }

    @Transactional
    public BloodBagReturn completeReturn(long id, Long actorId) {
        BloodBagReturn ret = getReturn(id);
        if (ret.getDocStatus() != BloodBagReturn.STATUS_PENDING) {
            throw new InvalidBloodBankRequestException("BGR_STATE",
                    "Return is not PENDING (current=" + ret.getDocStatus() + ")");
        }
        ret.setDocStatus(BloodBagReturn.STATUS_RETURNED);
        ret.setApprovedBy(actorId == null ? ACTOR : actorId);
        ret.setApprovedDatetime(Timestamp.from(Instant.now()));
        ret.setCompletedAt(Timestamp.from(Instant.now()));
        stamp(ret);
        return returnRepository.save(ret);
    }

    @Transactional
    public BloodBagReturn rejectReturn(long id, String remarks) {
        BloodBagReturn ret = getReturn(id);
        if (ret.getDocStatus() != BloodBagReturn.STATUS_PENDING) {
            throw new InvalidBloodBankRequestException("BGR_STATE",
                    "Return is not PENDING (current=" + ret.getDocStatus() + ")");
        }
        ret.setDocStatus(BloodBagReturn.STATUS_REJECTED);
        ret.setCompletedAt(Timestamp.from(Instant.now()));
        stamp(ret);
        return returnRepository.save(ret);
    }

    @Transactional(readOnly = true)
    public BloodBagReturn getReturn(long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("NOT_FOUND",
                        "Bag return not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BloodBagReturn> listReturns(Long bagId, Short status) {
        if (status != null) return returnRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (bagId != null) return returnRepository.findByBagIdOrderByAddedDatetimeDesc(bagId);
        return returnRepository.findAll();
    }

    // ============================================================ bag discard

    @Transactional
    public BloodBagDiscard raiseDiscard(long bagId, String reason) {
        requireBag(bagId);
        if (reason == null || reason.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "reason is required");
        }
        long id = discardRepository.nextId();
        BloodBagDiscard d = new BloodBagDiscard();
        d.setId(id);
        d.setDiscardNo("BGD-" + id);
        d.setBagId(bagId);
        d.setReason(reason);
        d.setDocStatus(BloodBagDiscard.STATUS_PENDING);
        d.setAddedBy(ACTOR);
        d.setAddedDatetime(Timestamp.from(Instant.now()));
        d.setSynchronizedd(false);
        discardRepository.save(d);
        recordTrail(bagId, BloodBagTrail.EVENT_DISCARDED, ACTOR, "BLOODBAG_DISCARD", d.getId(), reason);
        return d;
    }

    @Transactional
    public BloodBagDiscard completeDiscard(long id, Long actorId) {
        BloodBagDiscard d = getDiscard(id);
        if (d.getDocStatus() != BloodBagDiscard.STATUS_PENDING) {
            throw new InvalidBloodBankRequestException("BGD_STATE",
                    "Discard is not PENDING (current=" + d.getDocStatus() + ")");
        }
        d.setDocStatus(BloodBagDiscard.STATUS_DISCARDED);
        d.setApprovedBy(actorId == null ? ACTOR : actorId);
        d.setApprovedDatetime(Timestamp.from(Instant.now()));
        d.setCompletedAt(Timestamp.from(Instant.now()));
        stamp(d);
        return discardRepository.save(d);
    }

    @Transactional(readOnly = true)
    public BloodBagDiscard getDiscard(long id) {
        return discardRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("NOT_FOUND",
                        "Bag discard not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BloodBagDiscard> listDiscards(Long bagId, Short status) {
        if (status != null) return discardRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (bagId != null) return discardRepository.findByBagIdOrderByAddedDatetimeDesc(bagId);
        return discardRepository.findAll();
    }

    // ============================================================ blood-bank indent

    @Transactional
    public BloodBankIndent raiseIndent(long fromStoreId, long toStoreId, String bloodGroup,
                                        String component, int quantity, String requestedDate,
                                        String remarks) {
        if (fromStoreId == toStoreId) {
            throw new InvalidBloodBankRequestException("BEXA003",
                    "from_store_id and to_store_id must differ");
        }
        if (bloodGroup == null || bloodGroup.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "bloodGroup is required");
        }
        if (component == null || component.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "component is required");
        }
        if (quantity <= 0) {
            throw new InvalidBloodBankRequestException("BEXA003", "quantity must be > 0");
        }
        long id = indentRepository.nextId();
        BloodBankIndent ind = new BloodBankIndent();
        ind.setId(id);
        ind.setIndentNo("BNI-" + id);
        ind.setFromStoreId(fromStoreId);
        ind.setToStoreId(toStoreId);
        ind.setBloodGroup(bloodGroup);
        ind.setComponent(component);
        ind.setQuantity(quantity);
        ind.setRequestedDate(parseDate(requestedDate));
        ind.setRemarks(remarks);
        ind.setDocStatus(BloodBankIndent.STATUS_NEW);
        ind.setAddedBy(ACTOR);
        ind.setAddedDatetime(Timestamp.from(Instant.now()));
        ind.setSynchronizedd(false);
        indentRepository.save(ind);
        log.info("Blood-bank indent {} raised from_store={} to_store={} {} {} x{}",
                ind.getIndentNo(), fromStoreId, toStoreId, bloodGroup, component, quantity);
        return ind;
    }

    @Transactional
    public BloodBankIndent approveIndent(long id, Long actorId) {
        BloodBankIndent ind = getIndent(id);
        if (ind.getDocStatus() != BloodBankIndent.STATUS_NEW) {
            throw new InvalidBloodBankRequestException("BNI_STATE",
                    "Indent is not NEW (current=" + ind.getDocStatus() + ")");
        }
        ind.setDocStatus(BloodBankIndent.STATUS_APPROVED);
        ind.setApprovedBy(actorId == null ? ACTOR : actorId);
        ind.setApprovedDatetime(Timestamp.from(Instant.now()));
        stamp(ind);
        return indentRepository.save(ind);
    }

    @Transactional
    public BloodBankIndent issueIndent(long id) {
        BloodBankIndent ind = getIndent(id);
        if (ind.getDocStatus() != BloodBankIndent.STATUS_APPROVED) {
            throw new InvalidBloodBankRequestException("BNI_STATE",
                    "Indent is not APPROVED (current=" + ind.getDocStatus() + ")");
        }
        ind.setDocStatus(BloodBankIndent.STATUS_ISSUED);
        ind.setIssuedAt(Timestamp.from(Instant.now()));
        stamp(ind);
        return indentRepository.save(ind);
    }

    @Transactional
    public BloodBankIndent cancelIndent(long id) {
        BloodBankIndent ind = getIndent(id);
        if (ind.getDocStatus() == BloodBankIndent.STATUS_ISSUED) {
            throw new InvalidBloodBankRequestException("BNI_STATE", "ISSUED indent cannot be cancelled");
        }
        ind.setDocStatus(BloodBankIndent.STATUS_CANCELLED);
        stamp(ind);
        return indentRepository.save(ind);
    }

    @Transactional(readOnly = true)
    public BloodBankIndent getIndent(long id) {
        return indentRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("NOT_FOUND",
                        "Indent not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BloodBankIndent> listIndents(Long fromStoreId, Long toStoreId, Short status) {
        if (status != null) return indentRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (toStoreId != null) return indentRepository.findByToStoreIdOrderByAddedDatetimeDesc(toStoreId);
        if (fromStoreId != null) return indentRepository.findByFromStoreIdOrderByAddedDatetimeDesc(fromStoreId);
        return indentRepository.findAll();
    }

    // ============================================================ cross-unit orders

    @Transactional
    public BloodBankCrossUnit raiseCrossUnit(long fromBloodbankId, long toBloodbankId,
                                              String bloodGroup, String component, int quantity,
                                              String remarks) {
        if (fromBloodbankId == toBloodbankId) {
            throw new InvalidBloodBankRequestException("BEXA003",
                    "from_bloodbank_id and to_bloodbank_id must differ");
        }
        if (bloodGroup == null || bloodGroup.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "bloodGroup is required");
        }
        if (component == null || component.isBlank()) {
            throw new InvalidBloodBankRequestException("BEXA003", "component is required");
        }
        if (quantity <= 0) {
            throw new InvalidBloodBankRequestException("BEXA003", "quantity must be > 0");
        }
        long id = crossUnitRepository.nextId();
        BloodBankCrossUnit cu = new BloodBankCrossUnit();
        cu.setId(id);
        cu.setRequestNo("BCU-" + id);
        cu.setFromBloodbankId(fromBloodbankId);
        cu.setToBloodbankId(toBloodbankId);
        cu.setBloodGroup(bloodGroup);
        cu.setComponent(component);
        cu.setQuantity(quantity);
        cu.setRemarks(remarks);
        cu.setDocStatus(BloodBankCrossUnit.STATUS_REQUESTED);
        cu.setAddedBy(ACTOR);
        cu.setAddedDatetime(Timestamp.from(Instant.now()));
        cu.setSynchronizedd(false);
        crossUnitRepository.save(cu);
        log.info("Cross-unit order {} raised from={} to={} {} {} x{}",
                cu.getRequestNo(), fromBloodbankId, toBloodbankId, bloodGroup, component, quantity);
        return cu;
    }

    @Transactional
    public BloodBankCrossUnit acknowledgeCrossUnit(long id, Long actorId) {
        BloodBankCrossUnit cu = getCrossUnit(id);
        if (cu.getDocStatus() != BloodBankCrossUnit.STATUS_REQUESTED) {
            throw new InvalidBloodBankRequestException("BCU_STATE",
                    "Cross-unit order is not REQUESTED (current=" + cu.getDocStatus() + ")");
        }
        cu.setDocStatus(BloodBankCrossUnit.STATUS_ACKNOWLEDGED);
        cu.setAcknowledgedBy(actorId == null ? ACTOR : actorId);
        cu.setAcknowledgedDatetime(Timestamp.from(Instant.now()));
        stamp(cu);
        return crossUnitRepository.save(cu);
    }

    @Transactional
    public BloodBankCrossUnit receiveCrossUnit(long id, Long actorId) {
        BloodBankCrossUnit cu = getCrossUnit(id);
        if (cu.getDocStatus() != BloodBankCrossUnit.STATUS_ACKNOWLEDGED) {
            throw new InvalidBloodBankRequestException("BCU_STATE",
                    "Cross-unit order is not ACKNOWLEDGED (current=" + cu.getDocStatus() + ")");
        }
        cu.setDocStatus(BloodBankCrossUnit.STATUS_RECEIVED);
        cu.setReceivedBy(actorId == null ? ACTOR : actorId);
        cu.setReceivedDatetime(Timestamp.from(Instant.now()));
        stamp(cu);
        return crossUnitRepository.save(cu);
    }

    @Transactional
    public BloodBankCrossUnit cancelCrossUnit(long id) {
        BloodBankCrossUnit cu = getCrossUnit(id);
        if (cu.getDocStatus() == BloodBankCrossUnit.STATUS_RECEIVED) {
            throw new InvalidBloodBankRequestException("BCU_STATE",
                    "RECEIVED cross-unit order cannot be cancelled");
        }
        cu.setDocStatus(BloodBankCrossUnit.STATUS_CANCELLED);
        stamp(cu);
        return crossUnitRepository.save(cu);
    }

    @Transactional(readOnly = true)
    public BloodBankCrossUnit getCrossUnit(long id) {
        return crossUnitRepository.findById(id)
                .orElseThrow(() -> new BloodBankEntityNotFoundException("NOT_FOUND",
                        "Cross-unit order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<BloodBankCrossUnit> listCrossUnits(Long fromBloodbankId, Long toBloodbankId, Short status) {
        if (status != null) return crossUnitRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (toBloodbankId != null) return crossUnitRepository.findByToBloodbankIdOrderByAddedDatetimeDesc(toBloodbankId);
        if (fromBloodbankId != null) return crossUnitRepository.findByFromBloodbankIdOrderByAddedDatetimeDesc(fromBloodbankId);
        return crossUnitRepository.findAll();
    }

    // ============================================================ helpers

    private void requireBag(long bagId) {
        if (!bagRepository.existsById(bagId)) {
            throw new BloodBankEntityNotFoundException("NOT_FOUND", "Bag not found: " + bagId);
        }
    }
    private void stamp(com.hims.bloodbank.domain.model.BloodBankBase entity) {
        entity.setUpdatedBy(ACTOR);
        entity.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
    private static Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return Date.valueOf(java.time.LocalDate.parse(s));
    }
}
