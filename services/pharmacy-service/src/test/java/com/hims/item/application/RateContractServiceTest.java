package com.hims.item.application;

import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.RateContract;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.RateContractRepository;
import com.hims.item.domain.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateContractServiceTest {
    @Mock private RateContractRepository repo;
    @Mock private SupplierRepository suppliers;
    @Mock private ItemRepository items;
    private RateContractService service;

    @BeforeEach void setUp() { service = new RateContractService(repo, suppliers, items); }

    @Test void create_ok() {
        when(suppliers.existsById(1L)).thenReturn(true);
        when(items.existsById(2L)).thenReturn(true);
        when(repo.findBySupplierIdAndItemId(1L, 2L)).thenReturn(List.of());
        when(repo.nextId()).thenReturn(50L);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        RateContract rc = service.create(1L, 2L, new BigDecimal("11.25"), new BigDecimal("5"), null, "2026-01-01", "2026-12-31", null);
        assertThat(rc.getContractNo()).isEqualTo("RC-50");
        assertThat(rc.getStatusFlag()).isEqualTo(RateContract.STATUS_ACTIVE);
    }

    @Test void create_windowInverted_isBadRequest() {
        when(suppliers.existsById(1L)).thenReturn(true);
        when(items.existsById(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(1L, 2L, new BigDecimal("1"), null, null, "2026-12-01", "2026-01-01", null))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void create_overlappingActive_isBadRequest() {
        when(suppliers.existsById(1L)).thenReturn(true);
        when(items.existsById(2L)).thenReturn(true);
        RateContract existing = new RateContract(); existing.setStatusFlag(RateContract.STATUS_ACTIVE);
        existing.setValidFrom(Date.valueOf("2026-01-01")); existing.setValidTo(Date.valueOf("2026-06-30"));
        when(repo.findBySupplierIdAndItemId(1L, 2L)).thenReturn(List.of(existing));
        assertThatThrownBy(() -> service.create(1L, 2L, new BigDecimal("1"), null, null, "2026-06-01", "2026-12-31", null))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test void lookupActive_usesRepository() {
        RateContract existing = new RateContract(); existing.setId(7L);
        when(repo.findActiveOnDate(1L, 2L, RateContract.STATUS_ACTIVE, Date.valueOf("2026-06-01"))).thenReturn(Optional.of(existing));
        Optional<RateContract> out = service.lookupActive(1L, 2L, "2026-06-01");
        assertThat(out).isPresent();
        assertThat(out.get().getId()).isEqualTo(7L);
    }

    @Test void create_unknownSupplier_isNotFound() {
        assertThatThrownBy(() -> service.create(99L, 2L, new BigDecimal("1"), null, null, "2026-01-01", "2026-12-31", null))
                .isInstanceOf(ItemNotFoundException.class);
    }
}
