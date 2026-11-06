package com.hims.bloodbank.application;

import com.hims.bloodbank.application.exceptions.BloodBankEntityNotFoundException;
import com.hims.bloodbank.application.exceptions.InvalidBloodBankRequestException;
import com.hims.bloodbank.domain.model.BloodBagDiscard;
import com.hims.bloodbank.domain.model.BloodBagReturn;
import com.hims.bloodbank.domain.model.BloodBankCrossUnit;
import com.hims.bloodbank.domain.model.BloodBankIndent;
import com.hims.bloodbank.domain.repository.BloodBagDiscardRepository;
import com.hims.bloodbank.domain.repository.BloodBagRepository;
import com.hims.bloodbank.domain.repository.BloodBagReturnRepository;
import com.hims.bloodbank.domain.repository.BloodBagTrailRepository;
import com.hims.bloodbank.domain.repository.BloodBankCrossUnitRepository;
import com.hims.bloodbank.domain.repository.BloodBankIndentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BloodBankOperationsServiceTest {

    @Mock private BloodBagRepository bagRepository;
    @Mock private BloodBagTrailRepository trailRepository;
    @Mock private BloodBagReturnRepository returnRepository;
    @Mock private BloodBagDiscardRepository discardRepository;
    @Mock private BloodBankIndentRepository indentRepository;
    @Mock private BloodBankCrossUnitRepository crossUnitRepository;
    private BloodBankOperationsService service;

    @BeforeEach void setUp() {
        service = new BloodBankOperationsService(bagRepository, trailRepository,
                returnRepository, discardRepository, indentRepository, crossUnitRepository);
    }

    @Test void recordTrail_appendsRowAndWritesToRepository() {
        when(bagRepository.existsById(11L)).thenReturn(true);
        when(trailRepository.nextId()).thenReturn(101L);
        when(trailRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.recordTrail(11L, (short) 1, null, null, null, "collected");
        ArgumentCaptor<com.hims.bloodbank.domain.model.BloodBagTrail> captor =
                ArgumentCaptor.forClass(com.hims.bloodbank.domain.model.BloodBagTrail.class);
        org.mockito.Mockito.verify(trailRepository).save(captor.capture());
        assertThat(captor.getValue().getBagId()).isEqualTo(11L);
        assertThat(captor.getValue().getEventType()).isEqualTo((short) 1);
    }

    @Test void raiseReturn_unknownBag_isNotFound() {
        assertThatThrownBy(() -> service.raiseReturn(99L, "reason", null))
                .isInstanceOf(BloodBankEntityNotFoundException.class);
    }

    @Test void raiseReturn_blankReason_isBadRequest() {
        when(bagRepository.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.raiseReturn(1L, "  ", null))
                .isInstanceOf(InvalidBloodBankRequestException.class);
    }

    @Test void completeReturn_onlyFromPending() {
        BloodBagReturn ret = new BloodBagReturn(); ret.setId(7L); ret.setDocStatus(BloodBagReturn.STATUS_PENDING);
        when(returnRepository.findById(7L)).thenReturn(Optional.of(ret));
        when(returnRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BloodBagReturn out = service.completeReturn(7L, null);
        assertThat(out.getDocStatus()).isEqualTo(BloodBagReturn.STATUS_RETURNED);
    }

    @Test void raiseDiscard_blankReason_isBadRequest() {
        when(bagRepository.existsById(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.raiseDiscard(1L, "")).isInstanceOf(InvalidBloodBankRequestException.class);
    }

    @Test void raiseIndent_sameStore_isBadRequest() {
        assertThatThrownBy(() -> service.raiseIndent(1L, 1L, "A+", "PCV", 1, "2026-09-01", null))
                .isInstanceOf(InvalidBloodBankRequestException.class);
    }

    @Test void raiseIndent_approvesIssues() {
        when(indentRepository.nextId()).thenReturn(50L);
        when(indentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BloodBankIndent ind = service.raiseIndent(1L, 2L, "O+", "FFP", 4, "2026-09-01", null);
        assertThat(ind.getIndentNo()).isEqualTo("BNI-50");
        assertThat(ind.getDocStatus()).isEqualTo(BloodBankIndent.STATUS_NEW);
        when(indentRepository.findById(50L)).thenReturn(Optional.of(ind));
        BloodBankIndent approved = service.approveIndent(50L, null);
        assertThat(approved.getDocStatus()).isEqualTo(BloodBankIndent.STATUS_APPROVED);
        BloodBankIndent issued = service.issueIndent(50L);
        assertThat(issued.getDocStatus()).isEqualTo(BloodBankIndent.STATUS_ISSUED);
    }

    @Test void cancelIndent_issued_isBadRequest() {
        BloodBankIndent ind = new BloodBankIndent(); ind.setId(2L); ind.setDocStatus(BloodBankIndent.STATUS_ISSUED);
        when(indentRepository.findById(2L)).thenReturn(Optional.of(ind));
        assertThatThrownBy(() -> service.cancelIndent(2L)).isInstanceOf(InvalidBloodBankRequestException.class);
    }

    @Test void crossUnit_fullFlow() {
        when(crossUnitRepository.nextId()).thenReturn(700L);
        when(crossUnitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        BloodBankCrossUnit cu = service.raiseCrossUnit(1L, 2L, "AB-", "Platelets", 2, null);
        assertThat(cu.getRequestNo()).isEqualTo("BCU-700");
        when(crossUnitRepository.findById(700L)).thenReturn(Optional.of(cu));
        assertThat(service.acknowledgeCrossUnit(700L, null).getDocStatus())
                .isEqualTo(BloodBankCrossUnit.STATUS_ACKNOWLEDGED);
        assertThat(service.receiveCrossUnit(700L, null).getDocStatus())
                .isEqualTo(BloodBankCrossUnit.STATUS_RECEIVED);
    }

    @Test void crossUnit_sameBank_isBadRequest() {
        assertThatThrownBy(() -> service.raiseCrossUnit(1L, 1L, "A+", "PCV", 1, null))
                .isInstanceOf(InvalidBloodBankRequestException.class);
    }

    @Test void discard_completeFromPending() {
        BloodBagDiscard d = new BloodBagDiscard(); d.setId(11L); d.setDocStatus(BloodBagDiscard.STATUS_PENDING);
        when(discardRepository.findById(11L)).thenReturn(Optional.of(d));
        when(discardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThat(service.completeDiscard(11L, null).getDocStatus())
                .isEqualTo(BloodBagDiscard.STATUS_DISCARDED);
    }
}
