package com.hims.item.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Quotation;
import com.hims.item.domain.model.QuotationDetail;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.QuotationDetailRepository;
import com.hims.item.domain.repository.QuotationRepository;
import com.hims.item.domain.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/** M14 quotation lifecycle. SUBMITTED -> AWARDED (via PO) | REJECTED | EXPIRED. */
@Service
public class QuotationService {
    private static final long ACTOR = 1L;
    private final QuotationRepository quotationRepository;
    private final QuotationDetailRepository detailRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;

    public QuotationService(QuotationRepository quotationRepository,
                            QuotationDetailRepository detailRepository,
                            SupplierRepository supplierRepository,
                            ItemRepository itemRepository) {
        this.quotationRepository = quotationRepository;
        this.detailRepository = detailRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
    }

    public record Line(Long itemId, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal taxPercent, Integer leadTimeDays, String remarks) {}
    public record CompareRow(Long quotationId, String quotationNo, Long supplierId,
                             String supplierCode, String supplierName,
                             Long itemId, BigDecimal quantity, BigDecimal unitPrice,
                             BigDecimal taxPercent, Integer leadTimeDays) {}

    @Transactional
    public Quotation create(Long supplierId, String enquiryRef, String validUntil,
                            String remarks, List<Line> lines) {
        if (supplierId == null || !supplierRepository.existsById(supplierId))
            throw new ItemNotFoundException("NOT_FOUND", "Supplier not found");
        if (lines == null || lines.isEmpty())
            throw new InvalidItemRequestException("BEXA003", "At least one quotation line is required");
        for (Line l : lines) {
            if (l.itemId() == null || !itemRepository.existsById(l.itemId()))
                throw new ItemNotFoundException("NOT_FOUND", "Item not found");
            if (l.quantity() == null || l.quantity().signum() <= 0)
                throw new InvalidItemRequestException("BEXA003", "Each quotation line needs quantity > 0");
            if (l.unitPrice() == null || l.unitPrice().signum() < 0)
                throw new InvalidItemRequestException("BEXA003", "unitPrice must be non-negative");
        }
        long id = quotationRepository.nextId();
        Quotation q = new Quotation();
        q.setId(id);
        q.setQuotationNo("QT-" + id);
        q.setSupplierId(supplierId);
        q.setEnquiryRef(enquiryRef);
        q.setValidUntil(parseDate(validUntil));
        q.setRemarks(remarks);
        q.setDocStatus(Quotation.STATUS_DRAFT);
        q.setStatus(true);
        q.setAddedBy(ACTOR);
        q.setAddedDatetime(Timestamp.from(Instant.now()));
        q.setSynchronizedd(false);
        quotationRepository.save(q);
        for (Line l : lines) {
            QuotationDetail d = new QuotationDetail();
            d.setId(detailRepository.nextId());
            d.setQuotationId(id);
            d.setItemId(l.itemId());
            d.setQuantity(l.quantity());
            d.setUnitPrice(l.unitPrice());
            d.setTaxPercent(l.taxPercent() == null ? BigDecimal.ZERO : l.taxPercent());
            d.setLeadTimeDays(l.leadTimeDays());
            d.setRemarks(l.remarks());
            d.setStatus(true);
            d.setAddedBy(ACTOR);
            d.setAddedDatetime(Timestamp.from(Instant.now()));
            d.setSynchronizedd(false);
            detailRepository.save(d);
        }
        return q;
    }

    @Transactional
    public Quotation submit(long id) {
        Quotation q = get(id);
        if (q.getDocStatus() != Quotation.STATUS_DRAFT)
            throw new InvalidItemRequestException("ITEM_STATE", "Only DRAFT quotations can be submitted");
        q.setDocStatus(Quotation.STATUS_SUBMITTED);
        q.setSubmittedAt(Timestamp.from(Instant.now()));
        stamp(q);
        return quotationRepository.save(q);
    }

    @Transactional
    public Quotation reject(long id) {
        Quotation q = get(id);
        if (q.getDocStatus() != Quotation.STATUS_SUBMITTED && q.getDocStatus() != Quotation.STATUS_DRAFT)
            throw new InvalidItemRequestException("ITEM_STATE", "Only DRAFT or SUBMITTED quotations can be rejected");
        q.setDocStatus(Quotation.STATUS_REJECTED);
        stamp(q);
        return quotationRepository.save(q);
    }

    @Transactional
    public Quotation expire(long id) {
        Quotation q = get(id);
        if (q.getDocStatus() != Quotation.STATUS_SUBMITTED)
            throw new InvalidItemRequestException("ITEM_STATE", "Only SUBMITTED quotations can be expired");
        q.setDocStatus(Quotation.STATUS_EXPIRED);
        stamp(q);
        return quotationRepository.save(q);
    }

    @Transactional(readOnly = true)
    public Quotation get(long id) {
        return quotationRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("NOT_FOUND", "Quotation not found"));
    }
    @Transactional(readOnly = true)
    public List<QuotationDetail> details(long id) {
        get(id);
        return detailRepository.findByQuotationId(id);
    }
    @Transactional(readOnly = true)
    public List<Quotation> list(Long supplierId, Short status) {
        if (status != null) return quotationRepository.findByDocStatusOrderByAddedDatetimeDesc(status);
        if (supplierId != null) return quotationRepository.findBySupplierIdOrderByAddedDatetimeDesc(supplierId);
        return quotationRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CompareRow> compare(Long itemId) {
        if (itemId == null || !itemRepository.existsById(itemId))
            throw new ItemNotFoundException("NOT_FOUND", "Item not found");
        List<Quotation> quotations = quotationRepository.findQuotationsByItemId(itemId);
        List<CompareRow> out = new java.util.ArrayList<>();
        for (Quotation q : quotations) {
            if (q.getDocStatus() != Quotation.STATUS_SUBMITTED && q.getDocStatus() != Quotation.STATUS_AWARDED)
                continue;
            for (QuotationDetail d : detailRepository.findByQuotationId(q.getId())) {
                if (itemId.equals(d.getItemId())) {
                    out.add(new CompareRow(q.getId(), q.getQuotationNo(), q.getSupplierId(),
                            null, null, d.getItemId(), d.getQuantity(), d.getUnitPrice(),
                            d.getTaxPercent(), d.getLeadTimeDays()));
                }
            }
        }
        return out;
    }

    private void stamp(Quotation q) { q.setUpdatedBy(ACTOR); q.setUpdatedDatetime(Timestamp.from(Instant.now())); }
    private static java.sql.Date parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return java.sql.Date.valueOf(java.time.LocalDate.parse(s));
    }
    public static Line toLine(JsonNode n) {
        return new Line(longOrNull(n,"itemId"), decimalOrNull(n,"quantity"),
                decimalOrNull(n,"unitPrice"), decimalOrNull(n,"taxPercent"),
                intOrNull(n,"leadTimeDays"), textOrNull(n,"remarks"));
    }
    private static String textOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asText(); }
    private static Long longOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()||v.asLong(0)==0?null:v.asLong(); }
    private static Integer intOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.asInt(); }
    private static BigDecimal decimalOrNull(JsonNode n, String f) { JsonNode v = n.path(f); return v.isMissingNode()||v.isNull()?null:v.decimalValue(); }
}
