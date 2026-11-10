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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PurchaseOrderServiceTest {

    @Mock private PurchaseOrderRepository poRepository;
    @Mock private PurchaseOrderDetailRepository poDetailRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private QuotationRepository quotationRepository;
    private PurchaseOrderService service;

    @BeforeEach void setUp() { service = new PurchaseOrderService(poRepository, poDetailRepository, storeRepository, supplierRepository, itemRepository, quotationRepository); }

    @Test void create_minimal_ok() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(supplierRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(poRepository.nextId()).thenReturn(101L);
        when(poDetailRepository.nextId()).thenReturn(1001L);
        PurchaseOrder po = service.create(1L, 2L, null, null, null, "ok",
                List.of(new PurchaseOrderService.Line(10L, new BigDecimal("5"), new BigDecimal("12.5"), null, 3, "x")));
        assertThat(po.getPoNo()).isEqualTo("PO-101");
        assertThat(po.getDocStatus()).isEqualTo(PurchaseOrder.STATUS_DRAFT);
    }

    @Test void create_unknownStore_isNotFound() {
        assertThatThrownBy(() -> service.create(99L, 2L, null, null, null, null, List.of()))
                .isInstanceOf(ItemNotFoundException.class);
    }

    @Test void create_blankLines_isBadRequest() {
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(supplierRepository.existsById(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, 2L, null, null, null, null, List.of()))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void submit_draft_ok() {
        PurchaseOrder existing = new PurchaseOrder(); existing.setId(1L); existing.setDocStatus(PurchaseOrder.STATUS_DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        PurchaseOrder out = service.submit(1L);
        assertThat(out.getDocStatus()).isEqualTo(PurchaseOrder.STATUS_PENDING_APPROVAL);
    }

    @Test void approve_pending_awardsQuotation() {
        PurchaseOrder existing = new PurchaseOrder(); existing.setId(5L); existing.setDocStatus(PurchaseOrder.STATUS_PENDING_APPROVAL);
        existing.setQuotationId(7L);
        when(poRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.hims.item.domain.model.Quotation q = new com.hims.item.domain.model.Quotation();
        q.setId(7L); q.setDocStatus(com.hims.item.domain.model.Quotation.STATUS_SUBMITTED);
        when(quotationRepository.findById(7L)).thenReturn(Optional.of(q));
        PurchaseOrder out = service.approve(5L, null);
        assertThat(out.getDocStatus()).isEqualTo(PurchaseOrder.STATUS_APPROVED);
        assertThat(q.getDocStatus()).isEqualTo(com.hims.item.domain.model.Quotation.STATUS_AWARDED);
    }

    @Test void issue_nonApproved_isBadRequest() {
        PurchaseOrder existing = new PurchaseOrder(); existing.setId(1L); existing.setDocStatus(PurchaseOrder.STATUS_DRAFT);
        when(poRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.issue(1L)).isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void cancel_issued_isBadRequest() {
        PurchaseOrder existing = new PurchaseOrder(); existing.setId(1L); existing.setDocStatus(PurchaseOrder.STATUS_ISSUED);
        when(poRepository.findById(1L)).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> service.cancel(1L)).isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void recordReceive_closesPoWhenAllLinesDone() {
        PurchaseOrder po = new PurchaseOrder(); po.setId(2L); po.setDocStatus(PurchaseOrder.STATUS_ISSUED);
        when(poRepository.findById(2L)).thenReturn(Optional.of(po));
        PurchaseOrderDetail d = new PurchaseOrderDetail(); d.setItemId(10L); d.setOrderedQty(new BigDecimal("10")); d.setReceivedQty(new BigDecimal("5"));
        when(poDetailRepository.findByPoId(2L)).thenReturn(List.of(d));
        when(poRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poDetailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.recordReceive(2L, 10L, new BigDecimal("5"));
        assertThat(po.getDocStatus()).isEqualTo(PurchaseOrder.STATUS_CLOSED);
    }
}
