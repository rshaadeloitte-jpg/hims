package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ExpiryReturn;
import com.hims.item.domain.model.ExpiryReturnDetail;
import com.hims.item.domain.repository.ExpiryReturnDetailRepository;
import com.hims.item.domain.repository.ExpiryReturnRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.domain.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Expiry/damage return service (M13 gap). Ported from legacy
 * ExpiryItemServiceImpl (CIMS_ADDITEMEXPIRYRETURN + details).
 * Lifecycle: 1=PENDING -> 2=APPROVED -> 5=RETURNED_TO_SUPPLIER
 *            or 3=REJECTED / 4=CANCELLED (terminal).
 * Stock SUBTRACT (movementType 9 EXPIRY / 10 DAMAGE) runs on APPROVE so the
 * store ledger reflects the quarantine immediately (single-writer D-1 path).
 */
@Service
public class ExpiryReturnService {

    private static final Logger log = LoggerFactory.getLogger(ExpiryReturnService.class);

    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004 principal id

    private final ExpiryReturnRepository returnRepository;
    private final ExpiryReturnDetailRepository detailRepository;
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;

    public ExpiryReturnService(ExpiryReturnRepository returnRepository,
                               ExpiryReturnDetailRepository detailRepository,
                               ItemRepository itemRepository,
                               StoreRepository storeRepository,
                               SupplierRepository supplierRepository,
                               StockService stockService) {
        this.returnRepository = returnRepository;
        this.detailRepository = detailRepository;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
    }

    public record ReturnLine(Long itemId, Long batchId, Long grnId, BigDecimal quantity,
                             BigDecimal purchaseRate, String returnReason) {}

    @Transactional
    public ExpiryReturn create(Long storeId, Long supplierId, Short returnType,
                               String remarks, List<ReturnLine> lines) {
        if (storeId == null || !storeRepository.existsById(storeId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (supplierId != null && !supplierRepository.existsById(supplierId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
        }
        if (returnType == null || returnType < 1 || returnType > 3) {
            throw new InvalidItemRequestException("BEXA003",
                    "returnType must be 1=EXPIRED 2=DAMAGED 3=BOTH");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one return line is required");
        }

        long id = returnRepository.nextReturnId();
        ExpiryReturn ret = new ExpiryReturn();
        ret.setId(id);
        ret.setReturnNo("EXP-" + id);
        ret.setStoreId(storeId);
        ret.setSupplierId(supplierId);
        ret.setReturnType(returnType);
        ret.setDocStatus(ExpiryReturn.STATUS_PENDING);
        ret.setRemarks(remarks);
        ret.setAddedBy(AUDIT_ACTOR_ID);
        ret.setAddedDatetime(Timestamp.from(Instant.now()));
        returnRepository.save(ret);

        for (ReturnLine line : lines) {
            if (line.itemId() == null || !itemRepository.existsById(line.itemId())) {
                throw new ItemNotFoundException("NOT_FOUND", "Item not found");
            }
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003",
                        "Each line needs quantity > 0");
            }
            ExpiryReturnDetail d = new ExpiryReturnDetail();
            d.setId(detailRepository.nextDetailId());
            d.setExpiryReturnId(id);
            d.setItemId(line.itemId());
            d.setBatchId(line.batchId());
            d.setGrnId(line.grnId());
            d.setQuantity(line.quantity());
            d.setPurchaseRate(line.purchaseRate());
            d.setReturnReason(line.returnReason());
            d.setAddedBy(AUDIT_ACTOR_ID);
            d.setAddedDatetime(Timestamp.from(Instant.now()));
            detailRepository.save(d);
        }
        log.info("Expiry return {} created store={} type={} lines={}",
                id, storeId, returnType, lines.size());
        return ret;
    }

    @Transactional(readOnly = true)
    public ExpiryReturn get(long id) {
        return returnRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND",
                        "Expiry return not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ExpiryReturnDetail> details(long id) {
        get(id);
        return detailRepository.findByExpiryReturnId(id);
    }

    @Transactional(readOnly = true)
    public List<ExpiryReturn> list(Long storeId, Short docStatus) {
        if (docStatus != null) return returnRepository.findByDocStatusOrderByAddedDatetimeDesc(docStatus);
        if (storeId != null) return returnRepository.findByStoreIdOrderByAddedDatetimeDesc(storeId);
        return returnRepository.findAll();
    }

    /**
     * Approve (PENDING -> APPROVED) and quarantine stock via the D-1 engine:
     * EXPIRED/ BOTH lines use movementType 9, DAMAGED lines use 10,
     * operation SUBTRACT. Idempotent guard: already-approved returns 400.
     */
    @Transactional
    public ExpiryReturn approve(long id, Long approvedBy) {
        ExpiryReturn ret = get(id);
        if (ret.getDocStatus() != ExpiryReturn.STATUS_PENDING) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Return is not PENDING (current=" + ret.getDocStatus() + ")");
        }
        short movement = ret.getReturnType() == ExpiryReturn.TYPE_DAMAGED
                ? StockService.MOVEMENT_DAMAGE_RETURN
                : StockService.MOVEMENT_EXPIRY_RETURN;
        for (ExpiryReturnDetail d : detailRepository.findByExpiryReturnId(id)) {
            stockService.applyMovement(ret.getStoreId(), d.getItemId(), d.getBatchId(), null,
                    movement, StockService.OP_SUBTRACT, d.getQuantity(),
                    "EXPIRY_RETURN", id, d.getReturnReason());
        }
        ret.setDocStatus(ExpiryReturn.STATUS_APPROVED);
        ret.setApprovedBy(approvedBy != null ? approvedBy : AUDIT_ACTOR_ID);
        ret.setApprovedDatetime(Timestamp.from(Instant.now()));
        ret.setUpdatedBy(AUDIT_ACTOR_ID);
        ret.setUpdatedDatetime(Timestamp.from(Instant.now()));
        log.info("Expiry return {} approved (stock quarantined, movement {})", id, movement);
        return returnRepository.save(ret);
    }

    /** Reject (PENDING only) — no stock movement. */
    @Transactional
    public ExpiryReturn reject(long id) {
        ExpiryReturn ret = get(id);
        if (ret.getDocStatus() != ExpiryReturn.STATUS_PENDING) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only PENDING returns can be rejected (current=" + ret.getDocStatus() + ")");
        }
        ret.setDocStatus(ExpiryReturn.STATUS_REJECTED);
        ret.setUpdatedBy(AUDIT_ACTOR_ID);
        ret.setUpdatedDatetime(Timestamp.from(Instant.now()));
        log.info("Expiry return {} rejected", id);
        return returnRepository.save(ret);
    }

    /** Cancel (PENDING only) — no stock movement. */
    @Transactional
    public ExpiryReturn cancel(long id) {
        ExpiryReturn ret = get(id);
        if (ret.getDocStatus() != ExpiryReturn.STATUS_PENDING) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only PENDING returns can be cancelled (current=" + ret.getDocStatus() + ")");
        }
        ret.setDocStatus(ExpiryReturn.STATUS_CANCELLED);
        ret.setUpdatedBy(AUDIT_ACTOR_ID);
        ret.setUpdatedDatetime(Timestamp.from(Instant.now()));
        log.info("Expiry return {} cancelled", id);
        return returnRepository.save(ret);
    }

    /**
     * Return-to-supplier (APPROVED only): stamps supplier credit note fields
     * and terminal status 5. Legacy expiryItemReturnToSupplier parity.
     */
    @Transactional
    public ExpiryReturn returnToSupplier(long id, String creditNoteNo, BigDecimal creditNoteAmount) {
        ExpiryReturn ret = get(id);
        if (ret.getDocStatus() != ExpiryReturn.STATUS_APPROVED) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only APPROVED returns can be sent to supplier (current=" + ret.getDocStatus() + ")");
        }
        if (ret.getSupplierId() == null) {
            throw new InvalidItemRequestException("BEXA003",
                    "Return has no supplier — cannot return to supplier");
        }
        ret.setDocStatus(ExpiryReturn.STATUS_RETURNED);
        ret.setReturnedAt(Timestamp.from(Instant.now()));
        ret.setCreditNoteNo(creditNoteNo);
        ret.setCreditNoteAmount(creditNoteAmount);
        ret.setUpdatedBy(AUDIT_ACTOR_ID);
        ret.setUpdatedDatetime(Timestamp.from(Instant.now()));
        log.info("Expiry return {} returned to supplier creditNote={}", id, creditNoteNo);
        return returnRepository.save(ret);
    }
}
