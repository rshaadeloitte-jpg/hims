package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.PurchaseOrder;
import com.hims.item.domain.model.PurchaseOrderDetail;
import com.hims.item.domain.repository.PurchaseOrderDetailRepository;
import com.hims.item.domain.repository.PurchaseOrderRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.domain.repository.SupplierRepository;
import com.hims.item.domain.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * M14 purchase order lifecycle.
 * DRAFT -> PENDING_APPROVAL -> APPROVED -> ISSUED -> CLOSED  (or CANCELLED anywhere from DRAFT/PENDING/APPROVED).
 */
@Service
public class PurchaseOrderService {

    private static final long ACTOR = 1L;
    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderDetailRepository poDetailRepository;
    private final StoreRepository storeRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final QuotationRepository quotationRepository;

    public PurchaseOrderService(PurchaseOrderRepository poRepository,
                                PurchaseOrderDetailRepository poDetailRepository,
                                StoreRepository storeRepository,
                                SupplierRepository supplierRepository,
                                ItemRepository itemRepository,
                                QuotationRepository quotationRepository) {
        this.poRepository = poRepository;
        this.poDetailRepository = poDetailRepository;
        this.storeRepository = storeRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
        this.quotationRepository = quotationRepository;
    }

    public record Line(Long itemId, BigDecimal orderedQty, BigDecimal unitPrice,
                       BigDecimal taxPercent, Integer leadTimeDays, String remarks) {}

    @Transactional
    public PurchaseOrder create(Long storeId, Long supplierId, Long quotationId,
                                String orderDate, String expectedDate, String remarks,
                                List<Line> lines) {
        requireStore(storeId);
        requireSupplier(supplierId);
        if (lines == null || lines.isEmpty()) {
            throw new InvalidItemRequestException("BEXA003", "At least one PO line is required");
        }
        if (quotationId != null) {
            var q = quotationRepository.findById(quotationId)
                    .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Quotation not found"));
            if (!q.getSupplierId().equals(supplierId)) {
                throw new InvalidItemRequestException("BEXA003",
                        "Quotation supplier does not match PO supplier");
            }
        }
        for (Line l : lines) {
            requireItem(l.itemId());
            if (l.orderedQty() == null || l.orderedQty().signum() <= 0) {
                throw new InvalidItemRequestException("BEXA003", "Each PO line needs orderedQty > 0");
            }
            if (l.unitPrice() == null || l.unitPrice().signum() < 0) {
                throw new InvalidItemRequestException("BEXA003", "unitPrice must be non-negative");
            }
        }
        long id = poRepository.nextId();
        PurchaseOrder po = new PurchaseOrder();
        po.setId(id);
        po.setPoNo("PO-" + id);
        po.setStoreId(storeId);
        po.setSupplierId(supplierId);
        po.setQuotationId(quotationId);
        po.setOrderDate(parseDate(orderDate));
        po.setExpectedDate(parseDate(expectedDate));
        po.setRemarks(remarks);
        po.setDocStatus(PurchaseOrder.STATUS_DRAFT);
        po.setStatus(true);
        po.setAddedBy(ACTOR);
        po.setAddedDatetime(Timestamp.from(Instant.now()));
        po.setSynchronizedd(false);
        poRepository.save(po);
        for (Line l : lines) {
            PurchaseOrderDetail d = new PurchaseOrderDetail();
            d.setId(poDetailRepository.nextId());
            d.setPoId(id);
            d.setItemId(l.itemId());
            d.setOrderedQty(l.orderedQty());
            d.setReceivedQty(BigDecimal.ZERO);
            d.setUnitPrice(l.unitPrice());
            d.setTaxPercent(l.taxPercent() == null ? BigDecimal.ZERO : l.taxPercent());
            d.setLeadTimeDays(l.leadTimeDays());
            d.setRemarks(l.remarks());
            d.setStatus(true);
            d.setAddedBy(ACTOR);
            d.setAddedDatetime(Timestamp.from(Instant.now()));
            d.setSynchronizedd(false);
            poDetailRepository.save(d);
        }
        return po;
    }

    @Transactional
    public PurchaseOrder submit(long id) {
        PurchaseOrder po = get(id);
        requireState(po, PurchaseOrder.STATUS_DRAFT, "Only DRAFT POs can be submitted");
        po.setDocStatus(PurchaseOrder.STATUS_PENDING_APPROVAL);
        stamp(po);
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder approve(long id, Long actor) {
        PurchaseOrder po = get(id);
        requireState(po, PurchaseOrder.STATUS_PENDING_APPROVAL, "Only PENDING_APPROVAL POs can be approved");
        po.setDocStatus(PurchaseOrder.STATUS_APPROVED);
        po.setApprovedBy(actor == null ? ACTOR : actor);
        po.setApprovedDatetime(Timestamp.from(Instant.now()));
        stamp(po);
        PurchaseOrder saved = poRepository.save(po);
        if (saved.getQuotationId() != null) {
            quotationRepository.findById(saved.getQuotationId()).ifPresent(q -> {
                if (q.getDocStatus() == com.hims.item.domain.model.Quotation.STATUS_SUBMITTED) {
                    q.setDocStatus(com.hims.item.domain.model.Quotation.STATUS_AWARDED);
                    q.setAwardedAt(Timestamp.from(Instant.now()));
                    q.setAwardedPoId(saved.getId());
                    quotationRepository.save(q);
                }
            });
        }
        return saved;
    }

    @Transactional
    public PurchaseOrder reject(long id) {
        PurchaseOrder po = get(id);
        requireState(po, PurchaseOrder.STATUS_PENDING_APPROVAL, "Only PENDING_APPROVAL POs can be rejected");
        po.setDocStatus(PurchaseOrder.STATUS_DRAFT);
        stamp(po);
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder issue(long id) {
        PurchaseOrder po = get(id);
        requireState(po, PurchaseOrder.STATUS_APPROVED, "Only APPROVED POs can be issued");
        po.setDocStatus(PurchaseOrder.STATUS_ISSUED);
        po.setIssuedAt(Timestamp.from(Instant.now()));
        stamp(po);
        return poRepository.save(po);
    }

    @Transactional
    public PurchaseOrder cancel(long id) {
        PurchaseOrder po = get(id);
        if (po.getDocStatus() == PurchaseOrder.STATUS_CLOSED || po.getDocStatus() == PurchaseOrder.STATUS_ISSUED) {
            throw new InvalidItemRequestException("ITEM_STATE",
                    "ISSUED/CLOSED POs cannot be cancelled");
        }
        po.setDocStatus(PurchaseOrder.STATUS_CANCELLED);
        stamp(po);
        return poRepository.save(po);
    }

    /** Called from GrnService when a receive fully matches a PO line. */
    @Transactional
    public void recordReceive(long poId, long itemId, BigDecimal qty) {
        for (PurchaseOrderDetail d : poDetailRepository.findByPoId(poId)) {
            if (itemId == d.getItemId().longValue()) {
                d.setReceivedQty(d.getReceivedQty().add(qty));
                poDetailRepository.save(d);
            }
        }
        boolean allDone = poDetailRepository.findByPoId(poId).stream()
                .allMatch(d -> d.getReceivedQty().compareTo(d.getOrderedQty()) >= 0);
        if (allDone) {
            poRepository.findById(poId).ifPresent(po -> {
                if (po.getDocStatus() == PurchaseOrder.STATUS_ISSUED) {
                    po.setDocStatus(PurchaseOrder.STATUS_CLOSED);
                    po.setClosedAt(Timestamp.from(Instant.now()));
                    stamp(po);
                    poRepository.save(po);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public PurchaseOrder get(long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "PO not found: " + id));
    }
    @Transactional(readOnly = true)
    public List<PurchaseOrderDetail> details(long id) {
        get(id);
        return poDetailRepository.findByPoId(id);
    }
    @Transactional(readOnly = true)
    public List<PurchaseOrder> list(Long storeId, Long supplierId, Short status) {
        if (status != null) return poRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (storeId != null) return poRepository.findByStoreIdOrderByAddedDatetimeDesc(storeId);
        if (supplierId != null) return poRepository.findBySupplierIdOrderByAddedDatetimeDesc(supplierId);
        return poRepository.findAll();
    }

    private void requireStore(Long id) {
        if (id == null || !storeRepository.existsById(id))
            throw new ItemNotFoundException("NOT_FOUND", "Store not found");
    }
    private void requireSupplier(Long id) {
        if (id == null || !supplierRepository.existsById(id))
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
    }
    private void requireItem(Long id) {
        if (id == null || !itemRepository.existsById(id))
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
    }
    private void requireState(PurchaseOrder po, short expected, String message) {
        if (po.getDocStatus() != expected)
            throw new InvalidItemRequestException("ITEM_STATE", message + " (current=" + po.getDocStatus() + ")");
    }
    private void stamp(PurchaseOrder po) {
        po.setUpdatedBy(ACTOR);
        po.setUpdatedDatetime(Timestamp.from(Instant.now()));
    }
    private static java.sql.Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return java.sql.Date.valueOf(java.time.LocalDate.parse(s));
    }

    public static Line toLine(com.fasterxml.jackson.databind.JsonNode n) {
        return new Line(longOrNull(n, "itemId"), decimalOrNull(n, "orderedQty"),
                decimalOrNull(n, "unitPrice"), decimalOrNull(n, "taxPercent"),
                intOrNull(n, "leadTimeDays"), textOrNull(n, "remarks"));
    }
    private static String textOrNull(com.fasterxml.jackson.databind.JsonNode n, String f) {
        com.fasterxml.jackson.databind.JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }
    private static Long longOrNull(com.fasterxml.jackson.databind.JsonNode n, String f) {
        com.fasterxml.jackson.databind.JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() || v.asLong(0) == 0 ? null : v.asLong();
    }
    private static Integer intOrNull(com.fasterxml.jackson.databind.JsonNode n, String f) {
        com.fasterxml.jackson.databind.JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.asInt();
    }
    private static BigDecimal decimalOrNull(com.fasterxml.jackson.databind.JsonNode n, String f) {
        com.fasterxml.jackson.databind.JsonNode v = n.path(f);
        return v.isMissingNode() || v.isNull() ? null : v.decimalValue();
    }
}
