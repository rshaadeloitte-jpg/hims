package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.CustomProcessOrder;
import com.hims.item.domain.model.CustomProcessOrderDetail;
import com.hims.item.domain.repository.CustomProcessOrderDetailRepository;
import com.hims.item.domain.repository.CustomProcessOrderRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.domain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * M13 custom process order lifecycle. Reuses the D-1 single-writer stock
 * engine for receive posting (movementType = GRN_RECEIPT = 2). No stock
 * movement on approve; receive is the only place stock changes.
 */
@Service
public class CustomProcessOrderService {
    private static final long ACTOR = 1L;
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;
    private static final String REF_DOC_TYPE = "CUSTOM_PROCESS_ORDER";

    private final CustomProcessOrderRepository cpoRepository;
    private final CustomProcessOrderDetailRepository detailRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final StockService stockService;

    public CustomProcessOrderService(CustomProcessOrderRepository cpoRepository,
                                     CustomProcessOrderDetailRepository detailRepository,
                                     StoreRepository storeRepository,
                                     SupplierRepository supplierRepository,
                                     ItemRepository itemRepository,
                                     StockService stockService) {
        this.cpoRepository = cpoRepository;
        this.detailRepository = detailRepository;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
        this.stockService = stockService;
    }

    public record CpoLine(Long itemId, Long batchId, BigDecimal orderedQty, BigDecimal unitPrice, String remarks) {}
    public record ReceiveLine(Long detailId, Long batchId, BigDecimal quantity, String remarks) {}

    @Transactional
    public CustomProcessOrder create(Long storeId, Long supplierId, Long deptId,
                                     String requestDate, String requiredDate, String remarks,
                                     List<CpoLine> lines) {
        if (storeId == null || !storeRepository.existsById(storeId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
        }
        if (supplierId != null && !supplierRepository.existsById(supplierId)) {
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one CPO line is required");
        }
        for (CpoLine line : lines) {
            if (line.itemId() == null || !itemRepository.existsById(line.itemId())) {
                throw new ItemNotFoundException("NOT_FOUND", "Item not found");
            }
            if (line.orderedQty() == null || line.orderedQty().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003", "Each CPO line needs orderedQty > 0");
            }
        }
        long id = cpoRepository.nextCpoId();
        CustomProcessOrder cpo = new CustomProcessOrder();
        cpo.setId(id);
        cpo.setCpoNo("CPO-" + id);
        cpo.setStoreId(storeId);
        cpo.setSupplierId(supplierId);
        cpo.setDeptId(deptId);
        cpo.setRequestDate(parseDate(requestDate));
        cpo.setRequiredDate(parseDate(requiredDate));
        cpo.setRemarks(remarks);
        cpo.setDocStatus(CustomProcessOrder.STATUS_PENDING);
        cpo.setStatus(true);
        cpo.setAddedBy(ACTOR);
        cpo.setAddedDatetime(Timestamp.from(Instant.now()));
        cpo.setSynchronizedd(false);
        cpoRepository.save(cpo);

        for (CpoLine line : lines) {
            CustomProcessOrderDetail d = new CustomProcessOrderDetail();
            d.setId(detailRepository.nextDetailId());
            d.setCpoId(id);
            d.setItemId(line.itemId());
            d.setBatchId(line.batchId());
            d.setOrderedQty(line.orderedQty());
            d.setReceivedQty(BigDecimal.ZERO);
            d.setUnitPrice(line.unitPrice());
            d.setRemarks(line.remarks());
            d.setStatus(true);
            d.setAddedBy(ACTOR);
            d.setAddedDatetime(Timestamp.from(Instant.now()));
            d.setSynchronizedd(false);
            detailRepository.save(d);
        }
        return cpo;
    }

    @Transactional
    public CustomProcessOrder approve(long id, Long actor) {
        CustomProcessOrder cpo = get(id);
        if (cpo.getDocStatus() != CustomProcessOrder.STATUS_PENDING) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only PENDING CPOs can be approved (current=" + cpo.getDocStatus() + ")");
        }
        cpo.setDocStatus(CustomProcessOrder.STATUS_APPROVED);
        cpo.setApprovedBy(actor == null ? ACTOR : actor);
        cpo.setApprovedDatetime(Timestamp.from(Instant.now()));
        stamp(cpo);
        return cpoRepository.save(cpo);
    }

    @Transactional
    public CustomProcessOrder cancel(long id) {
        CustomProcessOrder cpo = get(id);
        if (cpo.getDocStatus() != CustomProcessOrder.STATUS_PENDING) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only PENDING CPOs can be cancelled (current=" + cpo.getDocStatus() + ")");
        }
        cpo.setDocStatus(CustomProcessOrder.STATUS_CANCELLED);
        stamp(cpo);
        return cpoRepository.save(cpo);
    }

    @Transactional
    public CustomProcessOrder receive(long id, List<ReceiveLine> receiveLines) {
        if (receiveLines == null || receiveLines.isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one receive line is required");
        }
        CustomProcessOrder cpo = get(id);
        if (cpo.getDocStatus() != CustomProcessOrder.STATUS_APPROVED
                && cpo.getDocStatus() != CustomProcessOrder.STATUS_PARTIALLY_RECEIVED) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "Only APPROVED or PARTIALLY_RECEIVED CPOs can receive (current=" + cpo.getDocStatus() + ")");
        }
        List<CustomProcessOrderDetail> details = detailRepository.findByCpoId(id);
        for (ReceiveLine rl : receiveLines) {
            if (rl.detailId() == null || rl.quantity() == null || rl.quantity().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003",
                        "Each receive line needs detailId and quantity > 0");
            }
            CustomProcessOrderDetail d = details.stream()
                    .filter(x -> rl.detailId().equals(x.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND",
                            "CPO detail not found: " + rl.detailId()));
            BigDecimal already = d.getReceivedQty() == null ? BigDecimal.ZERO : d.getReceivedQty();
            BigDecimal outstanding = d.getOrderedQty().subtract(already);
            if (rl.quantity().compareTo(outstanding) > 0) {
                throw new InvalidItemRequestException("BEXA003",
                        "Receive quantity exceeds outstanding for detail " + d.getId());
            }
            Long batchId = rl.batchId() != null ? rl.batchId() : d.getBatchId();
            stockService.applyMovement(cpo.getStoreId(), d.getItemId(), batchId, null,
                    (short) 2, StockService.OP_ADD, rl.quantity(),
                    REF_DOC_TYPE, cpo.getId(), rl.remarks() != null ? rl.remarks() : d.getRemarks());
            d.setReceivedQty(already.add(rl.quantity()));
            d.setBatchId(batchId);
            detailRepository.save(d);
        }
        boolean allReceived = details.stream().allMatch(d -> {
            BigDecimal r = d.getReceivedQty() == null ? BigDecimal.ZERO : d.getReceivedQty();
            return d.getOrderedQty().compareTo(r) <= 0;
        });
        cpo.setDocStatus(allReceived ? CustomProcessOrder.STATUS_RECEIVED
                : CustomProcessOrder.STATUS_PARTIALLY_RECEIVED);
        stamp(cpo);
        return cpoRepository.save(cpo);
    }

    @Transactional(readOnly = true)
    public CustomProcessOrder get(long id) {
        return cpoRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "CPO not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CustomProcessOrderDetail> details(long id) {
        get(id);
        return detailRepository.findByCpoId(id);
    }

    @Transactional(readOnly = true)
    public List<CustomProcessOrder> list(Long storeId, Short status) {
        if (status != null) return cpoRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (storeId != null) return cpoRepository.findByStoreIdOrderByAddedDatetimeDesc(storeId);
        return cpoRepository.findAll();
    }

    private void stamp(CustomProcessOrder cpo) {
        cpo.setUpdatedBy(ACTOR);
        cpo.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }

    private static java.sql.Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return java.sql.Date.valueOf(LocalDate.parse(s, ISO));
    }

    /** Allow controllers to bind from JsonNode arrays into typed records. */
    public static CpoLine toLine(JsonNode n) {
        return new CpoLine(longOrNull(n, "itemId"), longOrNull(n, "batchId"),
                decimalOrNull(n, "orderedQty"), decimalOrNull(n, "unitPrice"),
                textOrNull(n, "remarks"));
    }

    public static ReceiveLine toReceive(JsonNode n) {
        return new ReceiveLine(longOrNull(n, "detailId"), longOrNull(n, "batchId"),
                decimalOrNull(n, "quantity"), textOrNull(n, "remarks"));
    }

    private static String textOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
    private static Long longOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() || v.asLong(0) == 0 ? null : v.asLong();
    }
    private static BigDecimal decimalOrNull(JsonNode n, String f) {
        JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.decimalValue();
    }
}
