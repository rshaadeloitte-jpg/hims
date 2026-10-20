package com.hims.pharmacy.application;

import com.hims.pharmacy.application.exceptions.BcmaVerificationException;
import com.hims.pharmacy.domain.model.DrugMapping;
import com.hims.pharmacy.domain.model.HomeMedication;
import com.hims.pharmacy.domain.model.IvCompound;
import com.hims.pharmacy.domain.repository.DrugMappingRepository;
import com.hims.pharmacy.domain.repository.HomeMedicationRepository;
import com.hims.pharmacy.domain.repository.IvCompoundRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PharmacyM07GapsTest {

    private DrugMappingService drugService;
    private HomeMedicationService homeService;
    private IvCompoundEnhancementService ivEnhance;
    private DrugMappingRepository drugRepo;
    private HomeMedicationRepository homeRepo;
    private IvCompoundRepository ivRepo;

    @BeforeEach
    void setUp() {
        drugRepo = mock(DrugMappingRepository.class);
        homeRepo = mock(HomeMedicationRepository.class);
        ivRepo = mock(IvCompoundRepository.class);
        drugService = new DrugMappingService(drugRepo);
        homeService = new HomeMedicationService(homeRepo);
        ivEnhance = new IvCompoundEnhancementService(ivRepo, mock(com.hims.pharmacy.domain.repository.IvCompoundLineRepository.class));

        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seq = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seqVal = new java.util.concurrent.atomic.AtomicLong(0);
        when(seq.getSingleResult()).thenAnswer(inv -> seqVal.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seq);
        ReflectionTestUtils.setField(drugService, "em", em);
        ReflectionTestUtils.setField(homeService, "em", em);
        ReflectionTestUtils.setField(ivEnhance, "em", em);

        when(drugRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(homeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ivRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void drugMapping_createRequiresItemId() {
        assertThatThrownBy(() -> drugService.create(null, "A", "A", "B", "C", "D", "E", BigDecimal.valueOf(5), BigDecimal.valueOf(5), 1L))
                .isInstanceOf(BcmaVerificationException.class).extracting(e -> ((BcmaVerificationException)e).getErrorCode()).isEqualTo("BEXA003");
    }

    @Test
    void drugMapping_createAndRetrieve() {
        DrugMapping m = drugService.create(101L, "Paracetamol", "Paracetamol", "Panadol", "Analgesic", "500mg", "Tab", BigDecimal.valueOf(5), BigDecimal.valueOf(5), 1L);
        assertThat(m.getItemId()).isEqualTo(101L);
        assertThat(m.getIsActive()).isTrue();
    }

    @Test
    void drugMapping_searchGeneric() {
        when(drugRepo.findByGenericNameContainingIgnoreCase("para")).thenReturn(Collections.emptyList());
        assertThat(drugService.searchGeneric("para")).isEmpty();
    }

    @Test
    void drugMapping_updateActiveFlag() {
        DrugMapping m = new DrugMapping(); m.setMappingId(1L); m.setItemId(102L); m.setIsActive(true);
        when(drugRepo.findById(1L)).thenReturn(Optional.of(m));
        when(drugRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DrugMapping updated = drugService.update(1L, null, null, null, false, 2L);
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    void ivEnhance_createRequiresLines() {
        assertThatThrownBy(() -> ivEnhance.createCompoundWithBilling(1L, 10L, 100L, Collections.emptyList(), BigDecimal.ZERO, Timestamp.from(Instant.now()), "test", BigDecimal.ONE, 1L))
                .isInstanceOf(BcmaVerificationException.class);
    }

    @Test
    void ivEnhance_finalizeBillingSetsAmount() {
        IvCompound c = new IvCompound(); c.setCompoundId(99L); c.setStatusCode(IvCompound.STATUS_VERIFIED);
        when(ivRepo.findById(99L)).thenReturn(Optional.of(c));
        when(ivRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IvCompound result = ivEnhance.finalizeForBilling(99L, BigDecimal.valueOf(1500), 5L);
        assertThat(result.getBilledAmount()).isEqualTo(BigDecimal.valueOf(1500));
    }

    @Test
    void ivEnhance_notFoundThrows() {
        when(ivRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ivEnhance.finalizeForBilling(999L, BigDecimal.TEN, 1L))
                .isInstanceOf(BcmaVerificationException.class).extracting(e -> ((BcmaVerificationException)e).getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void homeMedication_prescribeRequiresPatientAndEncounter() {
        assertThatThrownBy(() -> homeService.prescribe(null, 10L, 20L, 1L, "Amlodipine", "5mg", "OD", 30, BigDecimal.ONE, "Tab", "After food", 1L))
                .isInstanceOf(BcmaVerificationException.class).extracting(e -> ((BcmaVerificationException)e).getErrorCode()).isEqualTo("BEXA003");
    }

    @Test
    void homeMedication_prescribeSuccess() {
        when(homeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        HomeMedication hm = homeService.prescribe(1L, 10L, 20L, 1L, "Amlodipine", "5mg", "OD", 30, BigDecimal.ONE, "Tab", "After food", 1L);
        assertThat(hm.getItemName()).isEqualTo("Amlodipine");
        assertThat(hm.getDispensed()).isFalse();
    }

    @Test
    void homeMedication_markDispensed() {
        HomeMedication hm = new HomeMedication(); hm.setHomeMedId(7L); hm.setDispensed(false);
        when(homeRepo.findById(7L)).thenReturn(Optional.of(hm));
        when(homeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        HomeMedication updated = homeService.markDispensed(7L, 3L);
        assertThat(updated.getDispensed()).isTrue();
    }

    @Test
    void homeMedication_listForEncounter() {
        when(homeRepo.findByEncounterId(50L)).thenReturn(Collections.emptyList());
        assertThat(homeService.listForEncounter(50L)).isEmpty();
    }

    @Test
    void pharmacyBilling_integrationSmoke() {
        assertThat(drugService).isNotNull();
        assertThat(ivEnhance).isNotNull();
        assertThat(homeService).isNotNull();
    }
}
