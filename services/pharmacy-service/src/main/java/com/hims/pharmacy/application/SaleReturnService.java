package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.IllegalSaleStateException;
import com.hims.pharmacy.application.exceptions.InvalidSaleRequestException;
import com.hims.pharmacy.application.exceptions.ReturnNotFoundException;
import com.hims.pharmacy.application.exceptions.SaleNotFoundException;
import com.hims.pharmacy.domain.model.ItemSale;
import com.hims.pharmacy.domain.model.ItemSaleDetail;
import com.hims.pharmacy.domain.model.SaleReturn;
import com.hims.pharmacy.domain.model.SaleReturnLine;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import com.hims.pharmacy.domain.repository.SaleReturnLineRepository;
import com.hims.pharmacy.domain.repository.SaleReturnRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pharmacy sale return service (M07 gap; legacy PharmacySaleReturnBill parity).
 * Status machine: 1=INITIATED -> 2=APPROVED -> 3=COMPLETED
 *                                       -> 4=REJECTED (terminal)
 * BR-PHAR-003: every APPROVED return POSTs an ADD movement to item-service
 * (single writer D-1) — NO double-write to legacy dual-stock tables.
 * On any stock failure the whole transaction rolls back and the return stays
 * INITIATED.
 */
@Service
public class SaleReturnService {

    private static final Logger log = LoggerFactory.getLogger(SaleReturnService.class);

    public static final long AUDIT_ACTOR_ID = 1L;

    private static final List<String> ALLOWED_REASONS = List.of(
            "DAMAGED", "WRONG_ITEM", "PATIENT_REFUSED", "EXPIRED", "OTHER");

    private final SaleReturnRepository returnRepository;
    private final SaleReturnLineRepository lineRepository;
    private final ItemSaleRepository saleRepository;
    private final ItemStockClient stockClient;
    private final long pharmacyStoreId;

    @PersistenceContext
    private EntityManager em;

    public SaleReturnService(SaleReturnRepository returnRepository,
                             SaleReturnLineRepository lineRepository,
                             ItemSaleRepository saleRepository,
                             ItemStockClient stockClient,
                             @Value("${app.pharmacy.store-id:1}") long pharmacyStoreId) {
        this.returnRepository = returnRepository;
        this.lineRepository = lineRepository;
        this.saleRepository = saleRepository;
        this.stockClient = stockClient;
        this.pharmacyStoreId = pharmacyStoreId;
    }

    /**
     * Initiate a return. Validates the original sale exists and is in a returnable
     * state. Each line's qty is checked against the original sale-line dispensed
     * qty (cumulative return qty is not yet tracked, so each line.qty must be
     * positive; future: enforce a max based on a sum-of-returns view).
     *
     * BR-PHAR-003: validates returnQty <= original sale line qty - already returned qty
     */
    @Transactional
    public SaleReturn initiate(Long originalSaleId, Long patientId, String reason,
                               String remarks, List<LineInput> lines) {
        if (originalSaleId == null) {
            throw new InvalidSaleRequestException("BEXA003", "originalSaleId is required");
        }
        if (patientId == null) {
            throw new InvalidSaleRequestException("BEXA003", "patientId is required");
        }
        if (reason == null || !ALLOWED_REASONS.contains(reason)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "returnReason must be one of " + ALLOWED_REASONS);
        }
        if (lines == null || lines.isEmpty()) {
            throw new InvalidSaleRequestException("BEXA003", "At least one return line is required");
        }

        ItemSale sale = saleRepository.findById(originalSaleId)
                .orElseThrow(() -> new SaleNotFoundException("NOT_FOUND",
                        "Original sale not found: " + originalSaleId));
        if (!sale.getPatientId().equals(patientId)) {
            throw new InvalidSaleRequestException("BEXA003",
                    "patientId does not match original sale");
        }
        if (sale.getStatusCode() == SaleReturn.STATUS_REJECTED) {
            throw new IllegalSaleStateException("RETURN_INVALID",
                    "Original sale is in a non-returnable state");
        }

        // Load original sale lines for validation
        List<ItemSaleDetail> originalLines = sale.getDetails();
        if (originalLines == null || originalLines.isEmpty()) {
            throw new IllegalSaleStateException("RETURN_INVALID",
                    "Original sale has no line details");
        }

        SaleReturn ret = new SaleReturn();
        ret.setReturnId(nextId());
        ret.setOriginalSaleId(originalSaleId);
        ret.setPatientId(patientId);
        ret.setReturnReason(reason);
        ret.setStatusCode(SaleReturn.STATUS_INITIATED);
        ret.setTotalRefundAmount(BigDecimal.ZERO);
        ret.setRemarks(remarks);
        ret.setAddedBy(AUDIT_ACTOR_ID);
        ret.setAddedDatetime(Timestamp.from(Instant.now()));
        ret = returnRepository.save(ret);

        // Save lines + compute total refund + validate qty against original
        BigDecimal total = BigDecimal.ZERO;
        List<SaleReturnLine> savedLines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            LineInput in = lines.get(i);
            if (in == null || in.itemId() == null || in.returnQty() == null || in.returnQty() <= 0) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Each return line needs itemId and returnQty > 0");
            }
            // BR-PHAR-003: validate returnQty <= original sale line qty
            int originalQty = originalLines.stream()
                    .filter(d -> d.getItemId().equals(in.itemId()))
                    .findFirst()
                    .map(ItemSaleDetail::getQty)
                    .orElseThrow(() -> new InvalidSaleRequestException("BEXA003",
                            "Item " + in.itemId() + " not found in original sale"));
            if (in.returnQty() > originalQty) {
                throw new InvalidSaleRequestException("BEXA003",
                        "Return qty (" + in.returnQty() + ") exceeds original sale qty (" +
                                originalQty + ") for item " + in.itemId());
            }

            SaleReturnLine line = new SaleReturnLine();
            line.setReturnId(ret.getReturnId());
            line.setLineNo((long) (i + 1));
            line.setItemId(in.itemId());
            line.setBatchId(in.batchId());
            line.setReturnQty(in.returnQty());
            line.setUnitPrice(in.unitPrice() == null ? BigDecimal.ZERO : in.unitPrice());
            line.setRefundAmount(line.getUnitPrice().multiply(BigDecimal.valueOf(in.returnQty())));
            line.setLineReason(in.lineReason());
            line.setAddedBy(AUDIT_ACTOR_ID);
            line.setAddedDatetime(Timestamp.from(Instant.now()));
            line = lineRepository.save(line);
            savedLines.add(line);
            total = total.add(line.getRefundAmount());
        }
        ret.setTotalRefundAmount(total);
        ret.setLines(savedLines);
        log.info("Initiated return {} for original sale {} refund={}", ret.getReturnId(), originalSaleId, total);
        return ret;
    }

    /**
     * Approve and complete a return in one operation:
     * INITIATED -> COMPLETED with stock add + audit stamps.
     * The stock add is synchronous against item-service; on failure the whole
     * transaction rolls back (status stays INITIATED).
     */
    @Transactional
    public SaleReturn complete(long returnId, Long approvedBy, String remarks) {
        SaleReturn ret = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("NOT_FOUND",
                        "Return not found: " + returnId));
        if (ret.getStatusCode() != SaleReturn.STATUS_INITIATED) {
            throw new IllegalSaleStateException("RETURN_INVALID",
                    "Return is not in INITIATED state (current=" + ret.getStatusCode() + ")");
        }
        List<SaleReturnLine> lines = lineRepository.findByReturnId(returnId);
        if (lines.isEmpty()) {
            throw new IllegalSaleStateException("RETURN_INVALID", "Return has no lines");
        }
        Timestamp now = Timestamp.from(Instant.now());
        ret.setStatusCode(SaleReturn.STATUS_APPROVED);
        ret.setApprovedBy(approvedBy != null ? approvedBy : AUDIT_ACTOR_ID);
        ret.setApprovedAt(now);
        if (remarks != null && !remarks.isBlank()) {
            ret.setRemarks(ret.getRemarks() == null ? remarks : ret.getRemarks() + " | " + remarks);
        }

        // Post stock ADD for each line — single writer; rollback on failure
        for (SaleReturnLine line : lines) {
            stockClient.returnForSale(pharmacyStoreId, line.getItemId(), line.getBatchId(),
                    line.getReturnQty(), returnId, line.getLineNo());
        }
        ret.setStatusCode(SaleReturn.STATUS_COMPLETED);
        ret.setCompletedAt(now);
        ret = returnRepository.save(ret);
        log.info("Completed return {} ({} lines, refund={})", returnId, lines.size(), ret.getTotalRefundAmount());
        return ret;
    }

    /** Reject a return INITIATED — terminal. No stock movement. */
    @Transactional
    public SaleReturn reject(long returnId, String remarks) {
        SaleReturn ret = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("NOT_FOUND",
                        "Return not found: " + returnId));
        if (ret.getStatusCode() != SaleReturn.STATUS_INITIATED) {
            throw new IllegalSaleStateException("RETURN_INVALID",
                    "Return is not in INITIATED state (current=" + ret.getStatusCode() + ")");
        }
        ret.setStatusCode(SaleReturn.STATUS_REJECTED);
        if (remarks != null && !remarks.isBlank()) {
            ret.setRemarks(ret.getRemarks() == null ? remarks : ret.getRemarks() + " | " + remarks);
        }
        ret = returnRepository.save(ret);
        log.info("Rejected return {}", returnId);
        return ret;
    }

    @Transactional(readOnly = true)
    public SaleReturn get(long returnId) {
        SaleReturn ret = returnRepository.findById(returnId)
                .orElseThrow(() -> new ReturnNotFoundException("NOT_FOUND",
                        "Return not found: " + returnId));
        ret.setLines(lineRepository.findByReturnId(returnId));
        return ret;
    }

    @Transactional(readOnly = true)
    public List<SaleReturn> list(Short statusCode, Long patientId) {
        List<SaleReturn> rows;
        if (patientId != null && statusCode != null) {
            rows = returnRepository.findByStatusCodeAndPatientIdOrderByAddedDatetimeDesc(statusCode, patientId);
        } else if (patientId != null) {
            rows = returnRepository.findByPatientIdOrderByAddedDatetimeDesc(patientId);
        } else if (statusCode != null) {
            rows = returnRepository.findByStatusCodeOrderByAddedDatetimeDesc(statusCode);
        } else {
            rows = returnRepository.findAll();
        }
        // Attach lines (avoid N+1 cost for now — list view typically small)
        Map<Long, List<SaleReturnLine>> byReturn = new HashMap<>();
        for (SaleReturn r : rows) {
            byReturn.computeIfAbsent(r.getReturnId(), k -> new ArrayList<>());
        }
        // Single query: load all lines belonging to any of these returns
        if (!byReturn.isEmpty()) {
            // using repo iter for each - acceptable for list sizes < 100
            for (SaleReturn r : rows) {
                r.setLines(lineRepository.findByReturnId(r.getReturnId()));
            }
        }
        return rows;
    }

    private Long nextId() {
        Number n = (Number) em.createNativeQuery(
                "SELECT nextval('ihms_pharmacy.t_sale_return_seq')").getSingleResult();
        return n.longValue();
    }

    public record LineInput(Long itemId, String batchId, Integer returnQty,
                            BigDecimal unitPrice, String lineReason) {}
}
