package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.application.exceptions.CompoundNotFoundException;
import com.hims.pharmacy.application.exceptions.HomeMedicationNotFoundException;
import com.hims.pharmacy.domain.model.BcmaVerification;
import com.hims.pharmacy.domain.model.IvCompound;
import com.hims.pharmacy.domain.model.IvCompoundLine;
import com.hims.pharmacy.domain.model.HomeMedication;
import com.hims.pharmacy.domain.repository.BcmaVerificationRepository;
import com.hims.pharmacy.domain.repository.HomeMedicationRepository;
import com.hims.pharmacy.domain.repository.IvCompoundLineRepository;
import com.hims.pharmacy.domain.repository.IvCompoundRepository;
import com.hims.pharmacy.domain.repository.ItemSaleRepository;
import com.hims.pharmacy.domain.repository.ItemSaleDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BcmaServiceTest {

    private BcmaVerificationRepository bcmaRepository;
    private ItemSaleRepository saleRepository;
    private ItemSaleDetailRepository detailRepository;
    private BcmaService service;

    @BeforeEach
    void setUp() {
        bcmaRepository = mock(BcmaVerificationRepository.class);
        saleRepository = mock(ItemSaleRepository.class);
        detailRepository = mock(ItemSaleDetailRepository.class);
        service = new BcmaService(bcmaRepository, saleRepository, detailRepository);
        // Inject mocked EntityManager for nextId()
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seqQuery = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(0);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seqQuery);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
        // Mock repository.save() to return the saved entity
        when(bcmaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void recordScan_requiresPatientId() {
        assertThatThrownBy(() -> service.recordScan(null, 50L, 10L, 200L, true, true, true, true, true, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void recordScan_requiresSaleId() {
        assertThatThrownBy(() -> service.recordScan(100L, null, 10L, 200L, true, true, true, true, true, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void recordScan_setsAllRightsPassed() {
        // Mock sale lookup
        com.hims.pharmacy.domain.model.ItemSale sale = new com.hims.pharmacy.domain.model.ItemSale();
        sale.setSaleId(50L);
        sale.setPatientId(100L);
        when(saleRepository.findById(50L)).thenReturn(Optional.of(sale));
        // Mock detail lookup
        com.hims.pharmacy.domain.model.ItemSaleDetail detail = new com.hims.pharmacy.domain.model.ItemSaleDetail();
        detail.setSaleId(50L);
        detail.setLineNo(10L);
        detail.setItemId(200L);
        when(detailRepository.findBySaleIdOrderByLineNoAsc(50L)).thenReturn(java.util.List.of(detail));

        BcmaVerification result = service.recordScan(100L, 50L, 10L, 200L, true, true, true, true, true, 1L);
        assertThat(result.getAllRightsPassed()).isTrue();
        assertThat(result.getRightPatient()).isTrue();
        assertThat(result.getRightDrug()).isTrue();
    }

    @Test
    void recordScan_missingOneRight_setsAllRightsPassedFalse() {
        // Mock sale lookup
        com.hims.pharmacy.domain.model.ItemSale sale = new com.hims.pharmacy.domain.model.ItemSale();
        sale.setSaleId(50L);
        sale.setPatientId(100L);
        when(saleRepository.findById(50L)).thenReturn(Optional.of(sale));
        // Mock detail lookup
        com.hims.pharmacy.domain.model.ItemSaleDetail detail = new com.hims.pharmacy.domain.model.ItemSaleDetail();
        detail.setSaleId(50L);
        detail.setLineNo(10L);
        detail.setItemId(200L);
        when(detailRepository.findBySaleIdOrderByLineNoAsc(50L)).thenReturn(java.util.List.of(detail));

        BcmaVerification result = service.recordScan(100L, 50L, 10L, 200L, true, true, true, true, false, 1L);
        assertThat(result.getAllRightsPassed()).isFalse();
    }

    @Test
    void override_requiresReason() {
        BcmaVerification bcma = new BcmaVerification();
        bcma.setBcmaId(1L);
        when(bcmaRepository.findById(1L)).thenReturn(Optional.of(bcma));

        assertThatThrownBy(() -> service.override(1L, "", 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void markAdministered_rejectsAlreadyAdministered() {
        BcmaVerification bcma = new BcmaVerification();
        bcma.setBcmaId(1L);
        bcma.setAdministered(true);
        when(bcmaRepository.findById(1L)).thenReturn(Optional.of(bcma));

        assertThatThrownBy(() -> service.markAdministered(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BCMA_ALREADY_ADMINISTERED");
    }

    @Test
    void markAdministered_rejectsRightsNotMet() {
        BcmaVerification bcma = new BcmaVerification();
        bcma.setBcmaId(1L);
        bcma.setAllRightsPassed(false);
        bcma.setOverridden(false);
        when(bcmaRepository.findById(1L)).thenReturn(Optional.of(bcma));

        assertThatThrownBy(() -> service.markAdministered(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BCMA_RIGHTS_NOT_MET");
    }

    @Test
    void get_unknownBcma_mapsTo404() {
        when(bcmaRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }
}

class IvCompoundServiceTest {

    private IvCompoundRepository compoundRepository;
    private IvCompoundLineRepository lineRepository;
    private IvCompoundService service;

    @BeforeEach
    void setUp() {
        compoundRepository = mock(IvCompoundRepository.class);
        lineRepository = mock(IvCompoundLineRepository.class);
        service = new IvCompoundService(compoundRepository, lineRepository);
        when(compoundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Inject mocked EntityManager for nextId()
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seqQuery = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(0);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seqQuery);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
    }

    @Test
    void create_requiresPatientId() {
        assertThatThrownBy(() -> service.create(null, 200L, null, List.of(line(501L, "Drug A", BigDecimal.TEN, "ml", null, null)), BigDecimal.TEN, null, null, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void create_requiresLines() {
        assertThatThrownBy(() -> service.create(100L, 200L, null, List.of(), BigDecimal.TEN, null, null, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .hasMessageContaining("compound line");
    }

    @Test
    void create_requiresEachLineFields() {
        assertThatThrownBy(() -> service.create(100L, 200L, null, List.of(
                new IvCompoundService.CompoundLineInput(null, null, BigDecimal.TEN, null, null, null)),
                BigDecimal.TEN, null, null, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .hasMessageContaining("itemId, itemName, quantity, uom");
    }

    @Test
    void compound_illegalTransition() {
        IvCompound c = new IvCompound();
        c.setCompoundId(1L);
        c.setStatusCode(IvCompound.STATUS_COMPOUNDED);
        when(compoundRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.compound(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    @Test
    void verify_illegalTransition() {
        IvCompound c = new IvCompound();
        c.setCompoundId(1L);
        c.setStatusCode(IvCompound.STATUS_PENDING);
        when(compoundRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.verify(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    @Test
    void administer_requiresVerifiedStatus() {
        IvCompound c = new IvCompound();
        c.setCompoundId(1L);
        c.setStatusCode(IvCompound.STATUS_COMPOUNDED);
        when(compoundRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.administer(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    @Test
    void cancel_onlyFromPendingOrCompounded() {
        IvCompound c = new IvCompound();
        c.setCompoundId(1L);
        c.setStatusCode(IvCompound.STATUS_ADMINISTERED);
        when(compoundRepository.findById(1L)).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.cancel(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    private static IvCompoundService.CompoundLineInput line(Long itemId, String itemName, BigDecimal qty, String uom, String batch, java.sql.Date exp) {
        return new IvCompoundService.CompoundLineInput(itemId, itemName, qty, uom, batch, exp);
    }
}

class HomeMedicationServiceTest {

    private HomeMedicationRepository repository;
    private HomeMedicationService service;

    @BeforeEach
    void setUp() {
        repository = mock(HomeMedicationRepository.class);
        service = new HomeMedicationService(repository);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Inject mocked EntityManager for nextId()
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seqQuery = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seq = new java.util.concurrent.atomic.AtomicLong(0);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seqQuery);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "em", em);
    }

    @Test
    void prescribe_requiresPatientId() {
        assertThatThrownBy(() -> service.prescribe(null, 200L, null, 501L, "Aspirin", null, null, null, BigDecimal.TEN, "mg", null, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void prescribe_requiresEncounterId() {
        assertThatThrownBy(() -> service.prescribe(100L, null, null, 501L, "Aspirin", "100mg", "OD", 7, BigDecimal.TEN, "mg", null, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void prescribe_setsDefaults() {
        HomeMedication hm = service.prescribe(100L, 200L, null, 501L, "Aspirin", "100mg", "OD", 7, BigDecimal.TEN, "mg", null, 1L);
        assertThat(hm.getPatientId()).isEqualTo(100L);
        assertThat(hm.getItemName()).isEqualTo("Aspirin");
        assertThat(hm.getDispensed()).isFalse();
    }

    @Test
    void markDispensed_rejectsAlreadyDispensed() {
        HomeMedication hm = new HomeMedication();
        hm.setHomeMedId(1L);
        hm.setDispensed(true);
        when(repository.findById(1L)).thenReturn(Optional.of(hm));

        assertThatThrownBy(() -> service.markDispensed(1L, 1L))
                .isInstanceOf(BcmaVerificationException.class)
                .extracting(e -> ((BcmaVerificationException) e).getErrorCode())
                .isEqualTo("PH_ILLEGAL_TRANSITION");
    }

    @Test
    void get_unknown_mapsTo404() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(HomeMedicationNotFoundException.class)
                .hasMessageContaining("Home medication not found");
    }
}
