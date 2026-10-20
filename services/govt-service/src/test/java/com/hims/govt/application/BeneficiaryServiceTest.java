package com.hims.govt.application;

import com.hims.govt.application.exceptions.GovtConflictException;
import com.hims.govt.application.exceptions.GovtNotFoundException;
import com.hims.govt.application.exceptions.InvalidGovtRequestException;
import com.hims.govt.domain.model.BeneficiaryEnrolment;
import com.hims.govt.domain.repository.BeneficiaryEnrolmentRepository;
import com.hims.govt.domain.repository.ReferenceTidRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L0 — enrolment + TID linkage guards (register WP-T4), no Spring context.
 * EVENT POLICY note (cssd-service convention): the service constructor graph holds
 * NO outbox emitter dependency at all, so no code path here can write an outbox
 * row — the L2 integration test asserts the same on real PG16.
 */
class BeneficiaryServiceTest {

    private BeneficiaryEnrolmentRepository enrolmentRepository;
    private ReferenceTidRepository tidRepository;
    private BeneficiaryService service;
    private final AtomicLong seq = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        enrolmentRepository = mock(BeneficiaryEnrolmentRepository.class);
        tidRepository = mock(ReferenceTidRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new BeneficiaryService(enrolmentRepository, tidRepository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(enrolmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tidRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(enrolmentRepository.existsByEnrolmentNo(any())).thenReturn(false);
        when(tidRepository.existsByTid(any())).thenReturn(false);
    }

    // ------------------------------------------------------------- enrol guards

    @Test
    void enrol_requiresPatientId_bexa003() {
        assertThatThrownBy(() -> service.enrol(null, "CHIRANJEEVI", "CH-001", LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .extracting(e -> ((InvalidGovtRequestException) e).getErrorCode())
                .isEqualTo("BEXA003");
    }

    @Test
    void enrol_rejectsNonChiranjeeviScheme_bexa003() {
        assertThatThrownBy(() -> service.enrol(11L, "AASMAAN", "CH-001", LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("scheme must be CHIRANJEEVI");
        assertThatThrownBy(() -> service.enrol(11L, null, "CH-001", LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("scheme is required");
    }

    @Test
    void enrol_requiresEnrolmentNo_andEnrolledOn_bexa003() {
        assertThatThrownBy(() -> service.enrol(11L, "CHIRANJEEVI", " ", LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("enrolmentNo is required");
        assertThatThrownBy(() -> service.enrol(11L, "CHIRANJEEVI", "x".repeat(41), LocalDate.now()))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("at most 40 characters");
        assertThatThrownBy(() -> service.enrol(11L, "CHIRANJEEVI", "CH-001", null))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("enrolledOn is required");
    }

    @Test
    void enrol_duplicateEnrolmentNo_mapsTo409() {
        when(enrolmentRepository.existsByEnrolmentNo("CH-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.enrol(11L, "CHIRANJEEVI", "CH-DUP", LocalDate.now()))
                .isInstanceOf(GovtConflictException.class)
                .extracting(e -> ((GovtConflictException) e).getErrorCode())
                .isEqualTo("GOVT_DUPLICATE_ENROLMENT_NO");
    }

    @Test
    void enrol_happyPath_pinsChiranjeeviScheme_activeTrue() {
        BeneficiaryEnrolment e = service.enrol(11L, "chiranjeevi", "CH-001", LocalDate.of(2025, 1, 15));

        assertThat(e.getEnrolmentId()).isNotNull();
        assertThat(e.getScheme()).isEqualTo("CHIRANJEEVI");   // normalized to the CHECK value
        assertThat(e.getIsActive()).isTrue();
        assertThat(e.getPatientId()).isEqualTo(11L);
        verify(enrolmentRepository).save(any());
    }

    // ------------------------------------------------------------- tid guards

    @Test
    void linkTid_unknownEnrolmentNo_mapsTo404() {
        when(enrolmentRepository.findByEnrolmentNo("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.linkTid("GHOST", "TID-1"))
                .isInstanceOf(GovtNotFoundException.class)
                .extracting(e -> ((GovtNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void linkTid_blankTid_bexa003() {
        when(enrolmentRepository.findByEnrolmentNo("CH-001"))
                .thenReturn(Optional.of(new BeneficiaryEnrolment()));

        assertThatThrownBy(() -> service.linkTid("CH-001", " "))
                .isInstanceOf(InvalidGovtRequestException.class)
                .hasMessageContaining("tid is required");
    }

    @Test
    void linkTid_duplicateTid_mapsTo409() {
        BeneficiaryEnrolment e = new BeneficiaryEnrolment();
        e.setEnrolmentId(7L);
        when(enrolmentRepository.findByEnrolmentNo("CH-001")).thenReturn(Optional.of(e));
        when(tidRepository.existsByTid("TID-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.linkTid("CH-001", "TID-DUP"))
                .isInstanceOf(GovtConflictException.class)
                .extracting(x -> ((GovtConflictException) x).getErrorCode())
                .isEqualTo("GOVT_DUPLICATE_TID");
    }

    @Test
    void linkTid_happyPath_linksToLoadedEnrolment() {
        BeneficiaryEnrolment e = new BeneficiaryEnrolment();
        e.setEnrolmentId(7L);
        when(enrolmentRepository.findByEnrolmentNo("CH-001")).thenReturn(Optional.of(e));

        var link = service.linkTid("CH-001", "TID-9");

        assertThat(link.getEnrolmentId()).isEqualTo(7L);
        assertThat(link.getTid()).isEqualTo("TID-9");
        verify(tidRepository).save(any());
    }
}
