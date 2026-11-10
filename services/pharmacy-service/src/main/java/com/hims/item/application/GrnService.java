package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Grn;
import com.hims.item.domain.model.GrnDetail;
import com.hims.item.domain.model.ItemBatch;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.model.ItemStockLedger;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.GrnDetailRepository;
import com.hims.item.domain.repository.GrnRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.domain.repository.SupplierRepository;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GRN posting (WP-2 procurement slice). ONE transaction per GRN:
 * header + lines (+ any NEW batches created inline) are persisted and every line
 * runs through the D-1 single-writer stock path via
 * {@link StockService#applyMovement} (same tx): balance row SELECT … FOR UPDATE,
 * ledger append (movementType=2 GRN/RECEIPT, operation=ADD, refDocType=GRN),
 * balance update, InventoryUpdated outbox event. Finally a single aggregate
 * {@code GRNPosted} event is appended to the transactional outbox.
 */
@Service
public class GrnService {

    private static final Logger log = LoggerFactory.getLogger(GrnService.class);

    public static final String EVENT_TYPE_GRN_POSTED = "GRNPosted";
    public static final short MOVEMENT_GRN_RECEIPT = 2;   // 1=OPENING_BALANCE 2=GRN/RECEIPT
    public static final String REF_DOC_TYPE = "GRN";

    /** GRN request DTO (Jackson-bound). */
    public record GrnRequest(Long storeId, Long supplierId, String grnNo, String invoiceNo,
                             String invoiceDate, String receivedDate, String remarks,
                             Boolean provisional, Boolean consignment,
                             java.util.List<GrnLine> lines) { }

    /** GRN line DTO. If batchId is absent, batchCode (+optional expiry/mrp/rate) creates a NEW batch. */
    public record GrnLine(Long itemId, BigDecimal quantity, Long batchId, String batchCode,
                          String expiryDate, BigDecimal mrp, BigDecimal purchaseRate, String remarks) { }

    private final GrnRepository grnRepository;
    private final GrnDetailRepository grnDetailRepository;
    private final ItemBatchRepository batchRepository;
    private final ItemRepository itemRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public GrnService(GrnRepository grnRepository,
                      GrnDetailRepository grnDetailRepository,
                      ItemBatchRepository batchRepository,
                      ItemRepository itemRepository,
                      StoreRepository storeRepository,
                      SupplierRepository supplierRepository,
                      StockService stockService,
                      OutboxService outboxService,
                      ObjectMapper objectMapper) {
        this.grnRepository = grnRepository;
        this.grnDetailRepository = grnDetailRepository;
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
        this.stockService = stockService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Grn postGrn(JsonNode body) {
        GrnRequest req = bind(body);

        if (req.storeId() == null || !storeRepository.existsById(req.storeId())) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (req.supplierId() != null && !supplierRepository.existsById(req.supplierId())) {
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
        }
        if (req.lines() == null || req.lines().isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one GRN line is required");
        }

        long id = grnRepository.nextGrnId();
        Grn grn = new Grn();
        grn.setId(id);
        grn.setGrnNo(req.grnNo() == null || req.grnNo().isBlank() ? "GRN-" + id : req.grnNo());
        grn.setStoreId(req.storeId());
        grn.setSupplierId(req.supplierId());
        grn.setInvoiceNo(req.invoiceNo());
        grn.setInvoiceDate(parseDate(req.invoiceDate()));
        grn.setReceivedDate(parseDate(req.receivedDate()));
        grn.setRemarks(req.remarks());
        grn.setProvisional(Boolean.TRUE.equals(req.provisional()));
        grn.setConsignment(Boolean.TRUE.equals(req.consignment()));
        grn.setDocStatus(Grn.STATUS_PENDING);
        grn.setAddedBy(StockService.AUDIT_ACTOR_ID);
        grn.setAddedDatetime(Timestamp.from(Instant.now()));
        grn.setSynchronizedd(false);
        grnRepository.save(grn);

        for (GrnLine line : req.lines()) {
            if (line.itemId() == null || !itemRepository.existsById(line.itemId())) {
                throw new ItemNotFoundException("NOT_FOUND", "Item not found");
            }
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
            }
            Long batchId = resolveBatch(line.itemId(), line);

            GrnDetail detail = new GrnDetail();
            detail.setId(grnDetailRepository.nextGrnDetailId());
            detail.setGrnId(grn.getId());
            detail.setItemId(line.itemId());
            detail.setBatchId(batchId);
            detail.setQuantity(line.quantity());
            detail.setPurchaseRate(line.purchaseRate());
            detail.setMrp(line.mrp());
            detail.setRemarks(line.remarks());
            detail.setAddedBy(StockService.AUDIT_ACTOR_ID);
            detail.setAddedDatetime(Timestamp.from(Instant.now()));
            detail.setSynchronizedd(false);
            grnDetailRepository.save(detail);

            // D-1 single-writer stock path IN THE SAME TX: lock balance row
            // (SELECT ... FOR UPDATE), append ledger, update balance + event.
            stockService.applyMovement(req.storeId(), line.itemId(), batchId, null,
                    MOVEMENT_GRN_RECEIPT, StockService.OP_ADD, line.quantity(),
                    REF_DOC_TYPE, grn.getId(), line.remarks());
        }

        emitGrnPosted(grn, req);
        log.info("GRN posted id={} no={} store={} lines={}", grn.getId(), grn.getGrnNo(),
                grn.getStoreId(), req.lines().size());
        return grn;
    }

    @Transactional(readOnly = true)
    public Grn get(Long id) {
        return grnRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Record not found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<GrnDetail> getDetails(Long grnId) {
        return grnDetailRepository.findByGrnId(grnId);
    }

    /**
     * Resolve the batch for a GRN line: use the given batchId, or create a NEW
     * item_batch inside the same tx (visible to applyMovement's existsById check).
     */
    private Long resolveBatch(Long itemId, GrnLine line) {
        if (line.batchId() != null) {
            if (!batchRepository.existsById(line.batchId())) {
                throw new ItemNotFoundException("NOT_FOUND", "Batch not found");
            }
            return line.batchId();
        }
        if (line.batchCode() == null || line.batchCode().isBlank()) {
            throw new InvalidItemRequestException("BEXA003",
                    "batchId or batchCode is required on every GRN line");
        }
        long batchSeq = batchRepository.nextBatchId();
        ItemBatch batch = new ItemBatch();
        batch.setId(batchSeq);
        batch.setItemId(itemId);
        batch.setBatchCode(line.batchCode());
        batch.setExpiryDate(parseDate(line.expiryDate()));
        batch.setMrp(line.mrp());
        batch.setPurchaseRate(line.purchaseRate());
        batch.setQuantity(line.quantity());
        batch.setAddedBy(StockService.AUDIT_ACTOR_ID);
        batch.setAddedDatetime(Timestamp.from(Instant.now()));
        batch.setSynchronizedd(false);
        ItemBatch saved = batchRepository.save(batch);
        return saved.getId();
    }

    private void emitGrnPosted(Grn grn, GrnRequest req) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("grnId", grn.getId());
        payload.put("grnNo", grn.getGrnNo());
        payload.put("storeId", grn.getStoreId());
        payload.put("supplierId", grn.getSupplierId());
        payload.put("invoiceNo", grn.getInvoiceNo());
        payload.put("lineCount", req.lines() == null ? 0 : req.lines().size());
        try {
            outboxService.append(EVENT_TYPE_GRN_POSTED, "Grn", grn.getId(),
                    String.valueOf(grn.getStoreId()), objectMapper.writeValueAsString(payload),
                    EVENT_TYPE_GRN_POSTED + ":" + grn.getId());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize GRNPosted payload", e);
        }
    }

    private GrnRequest bind(JsonNode body) {
        if (body == null || body.isNull() || body.isMissingNode()) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
        try {
            return objectMapper.treeToValue(body, GrnRequest.class);
        } catch (Exception e) {
            throw new InvalidItemRequestException("BEXA003", "Invalid input data !");
        }
    }

    private java.sql.Date parseDate(String iso) {
        return (iso == null || iso.isBlank()) ? null : java.sql.Date.valueOf(iso);
    }

}
