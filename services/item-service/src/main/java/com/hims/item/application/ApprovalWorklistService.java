package com.hims.item.application;

import com.hims.item.domain.model.CustomProcessOrder;
import com.hims.item.domain.model.ExpiryReturn;
import com.hims.item.domain.model.Grn;
import com.hims.item.domain.model.Indent;
import com.hims.item.domain.model.StockTransfer;
import com.hims.item.domain.repository.CustomProcessOrderRepository;
import com.hims.item.domain.repository.ExpiryReturnRepository;
import com.hims.item.domain.repository.GrnRepository;
import com.hims.item.domain.repository.IndentRepository;
import com.hims.item.domain.repository.StockTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Approval worklist (M13 gap) — single read-model that consolidates the
 * actionable queue of inventory documents across the module so a store
 * manager can act on them in one place.
 *
 * <p>Buckets sourced (read-only) from existing repositories:
 * <ul>
 *   <li>{@code INDENT}         from {@link IndentRepository}</li>
 *   <li>{@code GRN}            from {@link GrnRepository}</li>
 *   <li>{@code CPO}            from {@link CustomProcessOrderRepository}
 *       (present in the working tree, included here so the {@code docType=CPO}
 *       filter returns the actionable CPO queue without an extra round-trip)</li>
 *   <li>{@code EXPIRY_RETURN}  from {@link ExpiryReturnRepository}</li>
 *   <li>{@code STOCK_TRANSFER} from {@link StockTransferRepository}</li>
 * </ul>
 *
 * <p>Per-doc-type {@code docStatus} is normalised at the boundary of this
 * read model to a shared space:
 * <pre>
 *   1 = PENDING          (actionable)
 *   2 = APPROVED
 *   3 = REJECTED / RECEIVED
 *   4 = CANCELLED
 * </pre>
 * The natural source mapping is:
 * <ul>
 *   <li>Indent: 1/2/3/4 -> 1/2/3/4 (1:1)</li>
 *   <li>Grn: 1/2/3/4 -> 1/2/3/4 (1:1)</li>
 *   <li>CustomProcessOrder: 1=PENDING/2=APPROVED/3=PARTIALLY_RECEIVED/
 *       4=RECEIVED/5=CANCELLED -> 1/2/3/3/4</li>
 *   <li>ExpiryReturn: 1/2/3/4/5(RETURNED_TO_SUPPLIER) -> 1/2/3/4/3</li>
 *   <li>StockTransfer: 1=REQUESTED/2=DISPATCHED/3=RECEIVED/4=CANCELLED
 *       -> 1/2/3/4 (REQUESTED == PENDING, RECEIVED collapses into
 *       the REJECTED/RECEIVED bucket)</li>
 * </ul>
 *
 * <p>Only the actionable (PENDING/REQUESTED) bucket is queried per repo
 * because that is what a store manager needs to act on; the normalised
 * {@code docStatus} value on the returned row is always 1 in the current
 * contract. Result is sorted by {@code createdAt} DESC and capped at
 * 200 rows per call to keep the endpoint cheap.
 */
@Service
public class ApprovalWorklistService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorklistService.class);

    /** Max rows returned per call to keep the endpoint cheap. */
    public static final int MAX_ROWS = 200;

    // docType tokens
    public static final String TYPE_INDENT = "INDENT";
    public static final String TYPE_GRN = "GRN";
    public static final String TYPE_CPO = "CPO";
    public static final String TYPE_EXPIRY_RETURN = "EXPIRY_RETURN";
    public static final String TYPE_STOCK_TRANSFER = "STOCK_TRANSFER";

    // Shared normalised docStatus space (read-model boundary)
    public static final short STATUS_PENDING = 1;
    public static final short STATUS_APPROVED = 2;
    public static final short STATUS_REJECTED_OR_RECEIVED = 3;
    public static final short STATUS_CANCELLED = 4;

    private final IndentRepository indentRepository;
    private final GrnRepository grnRepository;
    private final CustomProcessOrderRepository customProcessOrderRepository;
    private final ExpiryReturnRepository expiryReturnRepository;
    private final StockTransferRepository stockTransferRepository;

    /**
     * Read-model row: one actionable document.
     *
     * @param docType   one of {@link #TYPE_INDENT}, {@link #TYPE_GRN},
     *                  {@link #TYPE_CPO}, {@link #TYPE_EXPIRY_RETURN},
     *                  {@link #TYPE_STOCK_TRANSFER}
     * @param id        primary key of the document
     * @param docNo     document number (e.g. indent no, grn no, cpo no, return no, transfer no)
     * @param storeId   owning/affected store (for stock transfer, the source store)
     * @param docStatus normalised status (1=PENDING 2=APPROVED 3=REJECTED/RECEIVED 4=CANCELLED)
     * @param remarks   free-text remarks from the source row (may be {@code null})
     * @param createdAt {@code addedDatetime} of the source row
     */
    public record ApprovalWorklistItem(String docType, Long id, String docNo, Long storeId,
                                       Short docStatus, String remarks, Timestamp createdAt) { }

    public ApprovalWorklistService(IndentRepository indentRepository,
                                   GrnRepository grnRepository,
                                   CustomProcessOrderRepository customProcessOrderRepository,
                                   ExpiryReturnRepository expiryReturnRepository,
                                   StockTransferRepository stockTransferRepository) {
        this.indentRepository = indentRepository;
        this.grnRepository = grnRepository;
        this.customProcessOrderRepository = customProcessOrderRepository;
        this.expiryReturnRepository = expiryReturnRepository;
        this.stockTransferRepository = stockTransferRepository;
    }

    /**
     * Build the consolidated worklist.
     *
     * @param storeId when non-null, restrict to documents tied to that store
     *                (Indent/Grn/ExpiryReturn/CPO by storeId; StockTransfer by
     *                {@code fromStoreId}); when {@code null} return all rows
     *                for the requested {@code docType} (or all types when
     *                {@code docType} is also {@code null}).
     * @param docType one of {@link #TYPE_INDENT}, {@link #TYPE_GRN},
     *                {@link #TYPE_CPO}, {@link #TYPE_EXPIRY_RETURN},
     *                {@link #TYPE_STOCK_TRANSFER}, or {@code null} for all.
     * @return up to {@link #MAX_ROWS} items ordered by {@code createdAt} DESC.
     */
    @Transactional(readOnly = true)
    public List<ApprovalWorklistItem> worklist(Long storeId, String docType) {
        String wanted = (docType == null) ? null : docType.trim().toUpperCase();

        List<ApprovalWorklistItem> out = new ArrayList<>();
        if (wanted == null || TYPE_INDENT.equals(wanted)) {
            collectIndents(storeId, out);
        }
        if (wanted == null || TYPE_GRN.equals(wanted)) {
            collectGrns(storeId, out);
        }
        if (wanted == null || TYPE_CPO.equals(wanted)) {
            collectCustomProcessOrders(storeId, out);
        }
        if (wanted == null || TYPE_EXPIRY_RETURN.equals(wanted)) {
            collectExpiryReturns(storeId, out);
        }
        if (wanted == null || TYPE_STOCK_TRANSFER.equals(wanted)) {
            collectStockTransfers(storeId, out);
        }

        // Newest first, stable tie-breaker on docType+id to keep order deterministic.
        out.sort(Comparator
                .comparing(ApprovalWorklistItem::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ApprovalWorklistItem::docType)
                .thenComparing(ApprovalWorklistItem::id,
                        Comparator.nullsLast(Comparator.naturalOrder())));

        if (out.size() > MAX_ROWS) {
            return new ArrayList<>(out.subList(0, MAX_ROWS));
        }
        return out;
    }

    // --- bucket collectors ------------------------------------------------

    private void collectIndents(Long storeId, List<ApprovalWorklistItem> out) {
        // Only the actionable (PENDING) bucket is interesting for a worklist;
        // the other terminal/approved states are not actionable.
        List<Indent> rows = indentRepository.findByDocStatus(Indent.STATUS_PENDING);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Indent i : rows) {
            if (storeId != null && !storeId.equals(i.getStoreId())) {
                continue;
            }
            out.add(new ApprovalWorklistItem(
                    TYPE_INDENT,
                    i.getId(),
                    i.getIndentNo(),
                    i.getStoreId(),
                    STATUS_PENDING,
                    i.getRemarks(),
                    i.getAddedDatetime()));
        }
    }

    private void collectGrns(Long storeId, List<ApprovalWorklistItem> out) {
        // Only the actionable (PENDING) bucket is interesting for a worklist.
        List<Grn> rows = grnRepository.findByDocStatus(Grn.STATUS_PENDING);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Grn g : rows) {
            if (storeId != null && !storeId.equals(g.getStoreId())) {
                continue;
            }
            out.add(new ApprovalWorklistItem(
                    TYPE_GRN,
                    g.getId(),
                    g.getGrnNo(),
                    g.getStoreId(),
                    STATUS_PENDING,
                    g.getRemarks(),
                    g.getAddedDatetime()));
        }
    }

    private void collectCustomProcessOrders(Long storeId, List<ApprovalWorklistItem> out) {
        // CustomProcessOrderRepository exposes
        // findByDocStatusOrderByAddedDatetimeDesc(Short).
        List<CustomProcessOrder> rows = customProcessOrderRepository
                .findByDocStatusOrderByAddedDatetimeDesc(CustomProcessOrder.STATUS_PENDING);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (CustomProcessOrder c : rows) {
            if (storeId != null && !storeId.equals(c.getStoreId())) {
                continue;
            }
            out.add(new ApprovalWorklistItem(
                    TYPE_CPO,
                    c.getId(),
                    c.getCpoNo(),
                    c.getStoreId(),
                    STATUS_PENDING,
                    c.getRemarks(),
                    c.getAddedDatetime()));
        }
    }

    private void collectExpiryReturns(Long storeId, List<ApprovalWorklistItem> out) {
        // ExpiryReturnRepository already exposes
        // findByDocStatusOrderByAddedDatetimeDesc(Short).
        List<ExpiryReturn> rows = expiryReturnRepository
                .findByDocStatusOrderByAddedDatetimeDesc(ExpiryReturn.STATUS_PENDING);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (ExpiryReturn r : rows) {
            if (storeId != null && !storeId.equals(r.getStoreId())) {
                continue;
            }
            out.add(new ApprovalWorklistItem(
                    TYPE_EXPIRY_RETURN,
                    r.getId(),
                    r.getReturnNo(),
                    r.getStoreId(),
                    STATUS_PENDING,
                    r.getRemarks(),
                    r.getAddedDatetime()));
        }
    }

    private void collectStockTransfers(Long storeId, List<ApprovalWorklistItem> out) {
        // StockTransfer.STATUS_REQUESTED == 1 == the actionable PENDING bucket
        // for transfers (REQUESTED -> DISPATCHED -> RECEIVED lifecycle).
        List<StockTransfer> rows = stockTransferRepository
                .findByDocStatusOrderByAddedDatetimeDesc(StockTransfer.STATUS_REQUESTED);
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (StockTransfer t : rows) {
            // "Owning" store for an actionable transfer is the source store:
            // that's the one a manager is dispatching from.
            if (storeId != null && !storeId.equals(t.getFromStoreId())) {
                continue;
            }
            out.add(new ApprovalWorklistItem(
                    TYPE_STOCK_TRANSFER,
                    t.getId(),
                    t.getTransferNo(),
                    t.getFromStoreId(),
                    STATUS_PENDING,
                    t.getRemarks(),
                    t.getAddedDatetime()));
        }
    }
}
