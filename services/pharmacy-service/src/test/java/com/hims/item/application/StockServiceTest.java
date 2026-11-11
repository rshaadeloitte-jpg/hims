package com.hims.item.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hims.item.application.exceptions.InvalidItemRequestException;
import com.hims.item.application.exceptions.ItemNotFoundException;
import com.hims.item.domain.model.ItemStockBalance;
import com.hims.item.domain.repository.ItemBatchRepository;
import com.hims.item.domain.repository.ItemRepository;
import com.hims.item.domain.repository.ItemStockBalanceRepository;
import com.hims.item.domain.repository.ItemStockLedgerRepository;
import com.hims.item.domain.repository.StoreRepository;
import com.hims.item.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock private ItemStockBalanceRepository balanceRepository;
    @Mock private ItemStockLedgerRepository ledgerRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private ItemBatchRepository batchRepository;
    @Mock private OutboxService outboxService;

    private StockService service;

    @BeforeEach
    void setUp() {
        service = new StockService(balanceRepository, ledgerRepository, itemRepository,
                storeRepository, batchRepository, outboxService, mapper);
        // save() must return the entity (Mockito default null would NPE in the emit path)
        when(balanceRepository.save(any(ItemStockBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerRepository.save(any(com.hims.item.domain.model.ItemStockLedger.class))).thenAnswer(inv -> {
            com.hims.item.domain.model.ItemStockLedger l = inv.getArgument(0);
            l.setId(900L);
            return l;
        });
    }

    private ItemStockBalance balance(Long id, BigDecimal inHand) {
        ItemStockBalance b = new ItemStockBalance();
        b.setId(id);
        b.setStoreId(10L);
        b.setItemId(20L);
        b.setQtyInHand(inHand);
        b.setBlockedQty(BigDecimal.ZERO);
        b.setAvailableQty(inHand);
        b.setVersion(0L);
        return b;
    }

    @Test
    void movement_add_updatesBalanceAndEmitsEvent() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        ItemStockBalance existing = balance(1L, BigDecimal.valueOf(100));
        doReturn(Optional.of(existing)).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);

        ItemStockBalance result = service.postMovement(10L, 20L, null, null,
                (short) 2, StockService.OP_ADD, BigDecimal.valueOf(25), "GRN", 5L, "receipt");

        assertThat(result.getQtyInHand()).isEqualByComparingTo("125");
        assertThat(result.getAvailableQty()).isEqualByComparingTo("125");
        assertThat(result.getLastLedgerId()).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1L);
        verify(ledgerRepository).save(any());
        verify(outboxService).append(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void movement_subtract_reducesBalance() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        ItemStockBalance existing = balance(1L, BigDecimal.valueOf(100));
        doReturn(Optional.of(existing)).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);

        ItemStockBalance result = service.postMovement(10L, 20L, null, null,
                (short) 3, StockService.OP_SUBTRACT, BigDecimal.valueOf(30), "ISSUE", 9L, null);

        assertThat(result.getQtyInHand()).isEqualByComparingTo("70");
    }

    @Test
    void movement_createsBalanceRowWhenMissing() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        doReturn(Optional.empty()).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);
        doReturn(balance(99L, BigDecimal.ZERO)).when(balanceRepository).save(any(ItemStockBalance.class));

        ItemStockBalance result = service.postMovement(10L, 20L, null, null,
                (short) 1, StockService.OP_ADD, BigDecimal.valueOf(50), "OPENING", null, "opening");

        assertThat(result.getQtyInHand()).isEqualByComparingTo("50");
        assertThat(result.getStoreId()).isEqualTo(10L);
        assertThat(result.getItemId()).isEqualTo(20L);
    }

    @Test
    void movement_validatesInput() {
        assertThatThrownBy(() -> service.postMovement(10L, 20L, null, null,
                (short) 2, StockService.OP_ADD, BigDecimal.ZERO, null, null, null))
                .isInstanceOf(InvalidItemRequestException.class);
        assertThatThrownBy(() -> service.postMovement(10L, 20L, null, null,
                (short) 99, StockService.OP_ADD, BigDecimal.ONE, null, null, null))
                .isInstanceOf(InvalidItemRequestException.class);
        assertThatThrownBy(() -> service.postMovement(10L, 20L, null, null,
                (short) 2, (short) 9, BigDecimal.ONE, null, null, null))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test
    void movement_missingItemOrStore_isNotFound() {
        doReturn(false).when(itemRepository).existsById(20L);
        assertThatThrownBy(() -> service.postMovement(10L, 20L, null, null,
                (short) 2, StockService.OP_ADD, BigDecimal.ONE, null, null, null))
                .isInstanceOf(ItemNotFoundException.class);

        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(false).when(storeRepository).existsById(10L);
        assertThatThrownBy(() -> service.postMovement(10L, 20L, null, null,
                (short) 2, StockService.OP_ADD, BigDecimal.ONE, null, null, null))
                .isInstanceOf(ItemNotFoundException.class);
    }

    // ------------------------------------------------------------- M13 adjustment / consumption

    @Test
    void adjust_add_excessFound_increasesBalance() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        ItemStockBalance existing = balance(1L, BigDecimal.valueOf(100));
        doReturn(Optional.of(existing)).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);

        ItemStockBalance result = service.adjustStock(10L, 20L, null, BigDecimal.valueOf(5), StockService.OP_ADD, "physical count excess");
        assertThat(result.getQtyInHand()).isEqualByComparingTo("105");
        verify(ledgerRepository).save(any());
    }

    @Test
    void adjust_subtract_shortage_decreasesBalance() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        ItemStockBalance existing = balance(1L, BigDecimal.valueOf(100));
        doReturn(Optional.of(existing)).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);

        ItemStockBalance result = service.adjustStock(10L, 20L, null, BigDecimal.valueOf(10), StockService.OP_SUBTRACT, "breakage");
        assertThat(result.getQtyInHand()).isEqualByComparingTo("90");
    }

    @Test
    void adjust_requiresReason() {
        assertThatThrownBy(() -> service.adjustStock(10L, 20L, null, BigDecimal.ONE, StockService.OP_ADD, " "))
                .isInstanceOf(InvalidItemRequestException.class)
                .hasMessageContaining("reason");
        assertThatThrownBy(() -> service.adjustStock(10L, 20L, null, BigDecimal.ZERO, StockService.OP_ADD, "x"))
                .isInstanceOf(InvalidItemRequestException.class);
    }

    @Test
    void consume_subtractsViaMovementType13() {
        doReturn(true).when(itemRepository).existsById(20L);
        doReturn(true).when(storeRepository).existsById(10L);
        ItemStockBalance existing = balance(1L, BigDecimal.valueOf(50));
        doReturn(Optional.of(existing)).when(balanceRepository).findByStoreItemBatchForUpdate(10L, 20L, null);

        ItemStockBalance result = service.consumeStock(10L, 20L, null, BigDecimal.valueOf(5), "ward consumption");
        assertThat(result.getQtyInHand()).isEqualByComparingTo("45");
    }
}
