package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.Item;
import com.hims.item.domain.model.Quotation;
import com.hims.item.domain.model.QuotationDetail;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.QuotationDetailRepository;
import com.hims.item.domain.repository.QuotationRepository;
import com.hims.item.domain.repository.SupplierRepository;
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
class QuotationServiceTest {
    @Mock private QuotationRepository quotationRepository;
    @Mock private QuotationDetailRepository detailRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ItemRepository itemRepository;
    private QuotationService service;

    @BeforeEach void setUp() { service = new QuotationService(quotationRepository, detailRepository, supplierRepository, itemRepository); }

    @Test void create_ok() {
        when(supplierRepository.existsById(2L)).thenReturn(true);
        when(itemRepository.existsById(10L)).thenReturn(true);
        when(quotationRepository.nextId()).thenReturn(7L);
        when(detailRepository.nextId()).thenReturn(700L);
        Quotation q = service.create(2L, "ENQ-1", "2099-12-31", "ok",
                List.of(new QuotationService.Line(10L, new BigDecimal("20"), new BigDecimal("9.5"), null, 4, null)));
        assertThat(q.getQuotationNo()).isEqualTo("QT-7");
        assertThat(q.getDocStatus()).isEqualTo(Quotation.STATUS_DRAFT);
    }

    @Test void submit_thenReject() {
        Quotation q = new Quotation(); q.setId(1L); q.setDocStatus(Quotation.STATUS_DRAFT);
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(q));
        when(quotationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Quotation out = service.submit(1L);
        assertThat(out.getDocStatus()).isEqualTo(Quotation.STATUS_SUBMITTED);
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(out));
        Quotation rejected = service.reject(1L);
        assertThat(rejected.getDocStatus()).isEqualTo(Quotation.STATUS_REJECTED);
    }

    @Test void expire_onlySubmitted() {
        Quotation q = new Quotation(); q.setId(1L); q.setDocStatus(Quotation.STATUS_DRAFT);
        when(quotationRepository.findById(1L)).thenReturn(Optional.of(q));
        assertThatThrownBy(() -> service.expire(1L)).isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void compare_returnsRowsAcrossSuppliers() {
        Item item = new Item(); item.setId(10L);
        when(itemRepository.existsById(10L)).thenReturn(true);
        Quotation q1 = new Quotation(); q1.setId(1L); q1.setDocStatus(Quotation.STATUS_SUBMITTED);
        Quotation q2 = new Quotation(); q2.setId(2L); q2.setDocStatus(Quotation.STATUS_AWARDED);
        when(quotationRepository.findQuotationsByItemId(10L)).thenReturn(List.of(q1, q2));
        QuotationDetail d1 = new QuotationDetail(); d1.setItemId(10L); d1.setQuantity(new BigDecimal("5")); d1.setUnitPrice(new BigDecimal("8"));
        QuotationDetail d2 = new QuotationDetail(); d2.setItemId(10L); d2.setQuantity(new BigDecimal("5")); d2.setUnitPrice(new BigDecimal("7"));
        when(detailRepository.findByQuotationId(1L)).thenReturn(List.of(d1));
        when(detailRepository.findByQuotationId(2L)).thenReturn(List.of(d2));
        List<QuotationService.CompareRow> rows = service.compare(10L);
        assertThat(rows).hasSize(2);
    }
}
