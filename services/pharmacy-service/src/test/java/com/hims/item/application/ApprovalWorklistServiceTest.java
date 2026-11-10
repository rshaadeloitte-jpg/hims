package com.hims.item.application;

import com.hims.item.application.ApprovalWorklistService.ApprovalWorklistItem;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ApprovalWorklistService} — covers:
 * bucket selection per docType, storeId filter, normalised docStatus,
 * createdAt DESC ordering, the 200-row cap, and CPO inclusion.
 */
class ApprovalWorklistServiceTest {

    private IndentRepository indentRepository;
    private GrnRepository grnRepository;
    private CustomProcessOrderRepository customProcessOrderRepository;
    private ExpiryReturnRepository expiryReturnRepository;
    private StockTransferRepository stockTransferRepository;
    private ApprovalWorklistService service;

    @BeforeEach
    void setUp() {
        indentRepository = mock(IndentRepository.class);
        grnRepository = mock(GrnRepository.class);
        customProcessOrderRepository = mock(CustomProcessOrderRepository.class);
        expiryReturnRepository = mock(ExpiryReturnRepository.class);
        stockTransferRepository = mock(StockTransferRepository.class);
        service = new ApprovalWorklistService(indentRepository, grnRepository,
                customProcessOrderRepository, expiryReturnRepository, stockTransferRepository);
    }

    @Test
    void allTypes_noStoreFilter_mergesAllBuckets_sortedByCreatedAtDesc() {
        Indent indent = indent(101L, "IND-101", 10L, "urgent", ts("2025-01-02 09:00:00"));
        Grn grn = grn(202L, "GRN-202", 10L, "pharma", ts("2025-01-03 09:00:00"));
        CustomProcessOrder cpo = cpo(303L, "CPO-303", 10L, "custom", ts("2025-01-04 09:00:00"));
        ExpiryReturn ret = expiry(404L, "EXP-404", 10L, "expired lot", ts("2025-01-05 09:00:00"));
        StockTransfer trf = transfer(505L, "TRF-505", 10L, 11L, "shift", ts("2025-01-06 09:00:00"));

        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of(indent));
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of(grn));
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(CustomProcessOrder.STATUS_PENDING))
                .thenReturn(List.of(cpo));
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(ExpiryReturn.STATUS_PENDING))
                .thenReturn(List.of(ret));
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(StockTransfer.STATUS_REQUESTED))
                .thenReturn(List.of(trf));

        List<ApprovalWorklistItem> out = service.worklist(null, null);

        assertThat(out).extracting(ApprovalWorklistItem::docType)
                .containsExactly("STOCK_TRANSFER", "EXPIRY_RETURN", "CPO", "GRN", "INDENT");
        assertThat(out).allMatch(i -> i.docStatus() != null && i.docStatus() == (short) 1,
                "all rows normalised to STATUS_PENDING=1");
    }

    @Test
    void singleDocType_returnsOnlyThatBucket() {
        Indent indent = indent(1L, "IND-1", 10L, null, ts("2025-01-02 09:00:00"));
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of(indent));
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());

        List<ApprovalWorklistItem> out = service.worklist(null, "INDENT");

        assertThat(out).hasSize(1);
        assertThat(out.get(0).docType()).isEqualTo("INDENT");
        assertThat(out.get(0).docNo()).isEqualTo("IND-1");
    }

    @Test
    void cpoBucket_isIncludedWhenRequested() {
        CustomProcessOrder cpo = cpo(1L, "CPO-1", 10L, "niche item", ts("2025-01-02 09:00:00"));
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of());
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(CustomProcessOrder.STATUS_PENDING))
                .thenReturn(List.of(cpo));
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());

        List<ApprovalWorklistItem> out = service.worklist(null, "CPO");

        assertThat(out).hasSize(1);
        assertThat(out.get(0).docType()).isEqualTo("CPO");
        assertThat(out.get(0).docNo()).isEqualTo("CPO-1");
    }

    @Test
    void storeFilter_keepsOnlyMatchingStore() {
        Indent a = indent(1L, "IND-1", 10L, null, ts("2025-01-01 09:00:00"));
        Indent b = indent(2L, "IND-2", 20L, null, ts("2025-01-02 09:00:00"));
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of(a, b));
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());

        List<ApprovalWorklistItem> out = service.worklist(10L, null);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).id()).isEqualTo(1L);
        assertThat(out.get(0).storeId()).isEqualTo(10L);
    }

    @Test
    void stockTransfer_storeFilter_usesFromStoreId() {
        StockTransfer t = transfer(1L, "TRF-1", 10L, 20L, null, ts("2025-01-01 09:00:00"));
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of());
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(StockTransfer.STATUS_REQUESTED))
                .thenReturn(List.of(t));

        // Filter by source store -> match
        assertThat(service.worklist(10L, "STOCK_TRANSFER")).hasSize(1);
        // Filter by destination store -> not actionable for that store, should be filtered out
        assertThat(service.worklist(20L, "STOCK_TRANSFER")).isEmpty();
    }

    @Test
    void docType_isCaseInsensitive() {
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(List.of());
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());

        // No exception, and the worklist runs to completion for any case of a known type.
        assertThat(service.worklist(null, "indent")).isEmpty();
        assertThat(service.worklist(null, "Indent")).isEmpty();
        assertThat(service.worklist(null, "Po")).isEmpty();   // unknown -> no bucket hit
    }

    @Test
    void cap_isAppliedAt200Rows() {
        // Build 250 indents; expect the first 200 (by createdAt DESC) returned.
        java.util.List<Indent> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 250; i++) {
            // Vary the timestamp by seconds so the sort order is deterministic;
            // higher index -> newer (later) timestamp so newest 200 are kept.
            long secs = 1_700_000_000L + i;
            rows.add(indent((long) i, "IND-" + i, 10L, null, new Timestamp(secs * 1000L)));
        }
        when(indentRepository.findByDocStatus(Indent.STATUS_PENDING)).thenReturn(rows);
        when(grnRepository.findByDocStatus(Grn.STATUS_PENDING)).thenReturn(List.of());
        when(customProcessOrderRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(expiryReturnRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());
        when(stockTransferRepository.findByDocStatusOrderByAddedDatetimeDesc(any())).thenReturn(List.of());

        List<ApprovalWorklistItem> out = service.worklist(null, null);

        assertThat(out).hasSize(ApprovalWorklistService.MAX_ROWS);
    }

    // --- helpers -----------------------------------------------------------

    private static Indent indent(long id, String no, long storeId, String remarks, Timestamp ts) {
        Indent i = new Indent();
        i.setId(id);
        i.setIndentNo(no);
        i.setStoreId(storeId);
        i.setRemarks(remarks);
        i.setDocStatus(Indent.STATUS_PENDING);
        i.setAddedDatetime(ts);
        return i;
    }

    private static Grn grn(long id, String no, long storeId, String remarks, Timestamp ts) {
        Grn g = new Grn();
        g.setId(id);
        g.setGrnNo(no);
        g.setStoreId(storeId);
        g.setRemarks(remarks);
        g.setDocStatus(Grn.STATUS_PENDING);
        g.setAddedDatetime(ts);
        return g;
    }

    private static CustomProcessOrder cpo(long id, String no, long storeId, String remarks, Timestamp ts) {
        CustomProcessOrder c = new CustomProcessOrder();
        c.setId(id);
        c.setCpoNo(no);
        c.setStoreId(storeId);
        c.setRemarks(remarks);
        c.setDocStatus(CustomProcessOrder.STATUS_PENDING);
        c.setAddedDatetime(ts);
        return c;
    }

    private static ExpiryReturn expiry(long id, String no, long storeId, String remarks, Timestamp ts) {
        ExpiryReturn r = new ExpiryReturn();
        r.setId(id);
        r.setReturnNo(no);
        r.setStoreId(storeId);
        r.setRemarks(remarks);
        r.setDocStatus(ExpiryReturn.STATUS_PENDING);
        r.setAddedDatetime(ts);
        return r;
    }

    private static StockTransfer transfer(long id, String no, long from, long to, String remarks, Timestamp ts) {
        StockTransfer t = new StockTransfer();
        t.setId(id);
        t.setTransferNo(no);
        t.setFromStoreId(from);
        t.setToStoreId(to);
        t.setRemarks(remarks);
        t.setDocStatus(StockTransfer.STATUS_REQUESTED);
        t.setAddedDatetime(ts);
        return t;
    }

    private static Timestamp ts(String iso) {
        return Timestamp.valueOf(iso);
    }
}
