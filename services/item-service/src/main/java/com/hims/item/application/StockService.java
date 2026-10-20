package com.hims.item.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.model.ItemStockLedger;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.ItemStockBalanceRepository;
import com.hims.item.domain.repository.ItemStockLedgerRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * D-1 SINGLE-WRITER STOCK ENGINE (AGT-02 §5.5 / IMP-DEC-021/025).
 * The ONLY component in the platform that mutates item_stock_balance and
 * item_stock_ledger. Every movement runs in one REQUIRES_NEW transaction:
 * <ol>
 *   <li>lock the balance row (SELECT ... FOR UPDATE, serializes concurrent movements)</li>
 *   <li>append the ledger row (prev/new balance)</li>
 *   <li>update the balance (in_hand ± qty, available = in_hand − blocked, last_ledger_id, version)</li>
 *   <li>append an {@code InventoryUpdated} event to the transactional outbox</li>
 * </ol>
 * Legacy multi-writers (Inventory/Expiry/ItemStockSvc/CSSD/Medication/Pharmacy +
 * CIMS_ADDITEMSTOCK / PharmacyServiceImpl:2140 dual-write) are retired at W5 —
 * consumers call this API or subscribe to the event.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    public static final String EVENT_TYPE_INVENTORY_UPDATED = "InventoryUpdated";
    public static final short OP_ADD = 1;
    public static final short OP_SUBTRACT = 2;
    public static final long AUDIT_ACTOR_ID = 1L;   // MIG-004: principal id

    private final ItemStockBalanceRepository balanceRepository;
    private final ItemStockLedgerRepository ledgerRepository;
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final ItemBatchRepository batchRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public StockService(ItemStockBalanceRepository balanceRepository,
                        ItemStockLedgerRepository ledgerRepository,
                        ItemRepository itemRepository,
                        StoreRepository storeRepository,
                        ItemBatchRepository batchRepository,
                        OutboxService outboxService,
                        ObjectMapper objectMapper) {
        this.balanceRepository = balanceRepository;
        this.ledgerRepository = ledgerRepository;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.batchRepository = batchRepository;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    /**
     * Record a stock movement in its OWN transaction (REQUIRES_NEW) — the public
     * single-writer entry point used by POST /api/v1/stock/movements.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemStockBalance postMovement(Long storeId, Long itemId, Long batchId, Long departmentId,
                                         Short movementType, Short operation, BigDecimal quantity,
                                         String refDocType, Long refDocId, String remarks) {
        return applyMovement(storeId, itemId, batchId, departmentId, movementType, operation,
                quantity, refDocType, refDocId, remarks);
    }

    /**
     * Record a stock movement joining the CALLER'S transaction (default REQUIRED
     * propagation). Used by multi-line documents (e.g. GRN posting) that must keep
     * header + lines + all balance/ledger mutations atomic in ONE tx while still
     * going through the same D-1 lock-or-create + ledger + event path.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public ItemStockBalance applyMovement(Long storeId, Long itemId, Long batchId, Long departmentId,
                                          Short movementType, Short operation, BigDecimal quantity,
                                          String refDocType, Long refDocId, String remarks) {
        if (storeId == null || itemId == null || quantity == null || quantity.signum() <= 0) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        if (movementType == null || movementType < 1 || movementType > 30) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        if (operation == null || (operation != OP_ADD && operation != OP_SUBTRACT)) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        }
        if (!storeRepository.existsById(storeId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (batchId != null && !batchRepository.existsById(batchId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Batch not found");
        }

        // 1) lock-or-create the balance row
        ItemStockBalance balance = balanceRepository.findByStoreItemBatchForUpdate(storeId, itemId, batchId)
                .orElseGet(() -> {
                    ItemStockBalance created = new ItemStockBalance();
                    created.setStoreId(storeId);
                    created.setItemId(itemId);
                    created.setBatchId(batchId);
                    created.setDepartmentId(departmentId);
                    created.setStatus(true);
                    created.setSynchronizedd(false);
                    return balanceRepository.save(created);
                });

        BigDecimal prev = balance.getQtyInHand() == null ? BigDecimal.ZERO : balance.getQtyInHand();
        BigDecimal delta = operation == OP_ADD ? quantity : quantity.negate();
        BigDecimal next = prev.add(delta);

        // 2) append ledger (server-owned audit block — IMP-DEC-023 F-4)
        ItemStockLedger ledger = new ItemStockLedger();
        ledger.setStoreId(storeId);
        ledger.setItemId(itemId);
        ledger.setBatchId(batchId);
        ledger.setDepartmentId(departmentId);
        ledger.setMovementType(movementType);
        ledger.setOperation(operation);
        ledger.setQuantity(quantity);
        ledger.setPrevBalance(prev);
        ledger.setNewBalance(next);
        ledger.setRefDocType(refDocType);
        ledger.setRefDocId(refDocId);
        ledger.setRemarks(remarks);
        ledger.setStatus(true);
        ledger.setSynchronizedd(false);
        ledger.setAddedBy(AUDIT_ACTOR_ID);
        ledger.setAddedDatetime(Timestamp.from(Instant.now()));
        ledgerRepository.save(ledger);

        // 3) update balance
        balance.setQtyInHand(next);
        balance.setAvailableQty(next.subtract(
                balance.getBlockedQty() == null ? BigDecimal.ZERO : balance.getBlockedQty()));
        balance.setLastLedgerId(ledger.getId());
        balance.setVersion(balance.getVersion() == null ? 1L : balance.getVersion() + 1);
        ItemStockBalance saved = balanceRepository.save(balance);

        // 4) transactional outbox event
        emitInventoryUpdated(saved, ledger);

        log.info("Stock movement store={} item={} batch={} op={} qty={} newBalance={}",
                storeId, itemId, batchId, operation, quantity, next);
        return saved;
    }

    public List<ItemStockBalance> getBalances(Long storeId, Long itemId) {
        if (storeId != null && itemId != null) {
            return balanceRepository.findByStoreIdAndItemId(storeId, itemId);
        }
        if (storeId != null) {
            return balanceRepository.findByStoreId(storeId);
        }
        return balanceRepository.findAll();
    }

    public List<ItemStockLedger> getLedger(Long storeId, Long itemId) {
        if (storeId == null || itemId == null) {
            throw new InvalidItemRequestException("BEXA003", "storeId and itemId are required");
        }
        return ledgerRepository.findByStoreIdAndItemIdOrderByAddedDatetimeDescIdDesc(storeId, itemId);
    }

    /**
     * FEFO batch availability (MIG-012b, UNKNOWN-012 resolved). Read-only projection
     * for dispensing/issue screens: batches of one item in one store with positive
     * available qty, not expired, not freeze-recalled, ordered first-expiry-first-out
     * ({@code ORDER BY ExpiryDate, BatchId}, null expiry last) — exact parity with
     * legacy {@code CIMS_GETITEMBATCHLISTFORSEARCH_NEWONE_OUTER_IMPROVED}.
     */
    @Transactional(readOnly = true)
    public List<com.hims.item.application.BatchAvailabilityView> getAvailableBatches(Long storeId, Long itemId) {
        if (storeId == null || itemId == null) {
            throw new InvalidItemRequestException("BEXA003", "storeId and itemId are required");
        }
        if (!storeRepository.existsById(storeId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (!itemRepository.existsById(itemId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        }
        return balanceRepository.findAvailableBatchesFefo(storeId, itemId);
    }

    // ------------------------------------------------------------------ M13 stock adjustment (ItemStockLedger movementType 8)

    public static final short MOVEMENT_ADJUSTMENT = 8;
    public static final short MOVEMENT_CONSUMPTION = 13; // legacy consumption type (approx)
    public static final short MOVEMENT_EXPIRY_RETURN = 9;
    public static final short MOVEMENT_DAMAGE_RETURN = 10;

    /**
     * Stock adjustment (M13): explicit wrapper over D-1 postMovement with movementType=8.
     * Requires non-blank reason (legacy stockAdjusmentDetails.vm). Ponytail: no new table,
     * reuse ledger + balance with ADJUSTMENT type.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemStockBalance adjustStock(Long storeId, Long itemId, Long batchId,
                                        BigDecimal quantity, Short operation, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidItemRequestException("BEXA003", "quantity must be > 0");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidItemRequestException("BEXA003", "reason is required for stock adjustment");
        }
        if (operation == null || (operation != OP_ADD && operation != OP_SUBTRACT)) {
            throw new InvalidItemRequestException("BEXA003", "operation must be 1=ADD or 2=SUBTRACT");
        }
        return postMovement(storeId, itemId, batchId, null,
                MOVEMENT_ADJUSTMENT, operation, quantity,
                "STOCK_ADJUSTMENT", null, reason.trim());
    }

    /** Consumption (M13 ward consumption): movementType ~13, subtract. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ItemStockBalance consumeStock(Long storeId, Long itemId, Long batchId,
                                         BigDecimal quantity, String reason) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new InvalidItemRequestException("BEXA003", "quantity must be > 0");
        }
        return postMovement(storeId, itemId, batchId, null,
                MOVEMENT_CONSUMPTION, OP_SUBTRACT, quantity,
                "CONSUMPTION", null, reason == null ? "ward consumption" : reason.trim());
    }

    private void emitInventoryUpdated(ItemStockBalance balance, ItemStockLedger ledger) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("storeId", balance.getStoreId());
        payload.put("itemId", balance.getItemId());
        payload.put("batchId", balance.getBatchId());
        payload.put("departmentId", balance.getDepartmentId() == null ? null : balance.getDepartmentId());
        payload.put("movementType", ledger.getMovementType());
        payload.put("operation", ledger.getOperation());
        payload.put("quantity", ledger.getQuantity());
        payload.put("newBalance", balance.getQtyInHand());
        payload.put("refDocType", ledger.getRefDocType());
        payload.put("refDocId", ledger.getRefDocId());
        try {
            outboxService.append(EVENT_TYPE_INVENTORY_UPDATED, "ItemStockBalance", balance.getId(),
                    String.valueOf(balance.getItemId()), objectMapper.writeValueAsString(payload),
                    "stock:" + balance.getItemId() + ":" + ledger.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize stock event", e);
        }
    }
}
