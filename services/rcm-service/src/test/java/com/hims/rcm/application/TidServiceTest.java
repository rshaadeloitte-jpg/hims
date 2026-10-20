package com.hims.rcm.application;

import com.hims.rcm.application.exceptions.InvalidRcmRequestException;
import com.hims.rcm.application.exceptions.RcmNotFoundException;
import com.hims.rcm.application.exceptions.TidConflictException;
import com.hims.rcm.domain.model.AdmissionTid;
import com.hims.rcm.domain.repository.AdmissionTidRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * L0 — admission TID registry guards (register WP-R4): tid required / max 60,
 * one TID per admission (duplicate -> 409 TID_EXISTS), unknown admission -> 404.
 */
class TidServiceTest {

    private AdmissionTidRepository repository;
    private TidService service;

    @BeforeEach
    void setUp() {
        repository = mock(AdmissionTidRepository.class);
        EntityManager em = mock(EntityManager.class);
        service = new TidService(repository);
        ReflectionTestUtils.setField(service, "em", em);

        Query seqQuery = mock(Query.class);
        AtomicLong seq = new AtomicLong(500);
        when(seqQuery.getSingleResult()).thenAnswer(inv -> seq.incrementAndGet());
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seqQuery);

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assign_registersTidForAdmission() {
        when(repository.existsByAdmissionId(77L)).thenReturn(false);

        AdmissionTid saved = service.assign(77L, "TID-2026-0001");

        assertThat(saved.getAdmissionId()).isEqualTo(77L);
        assertThat(saved.getTid()).isEqualTo("TID-2026-0001");
        assertThat(saved.getTidId()).isPositive();
    }

    @Test
    void assign_blankTid_bexa003() {
        assertThatThrownBy(() -> service.assign(77L, null))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("tid is required");
        assertThatThrownBy(() -> service.assign(77L, "   "))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("tid is required");
    }

    @Test
    void assign_overlongTid_bexa003() {
        assertThatThrownBy(() -> service.assign(77L, "x".repeat(NhcxService.MAX_TID_LENGTH + 1)))
                .isInstanceOf(InvalidRcmRequestException.class)
                .hasMessageContaining("60");
    }

    @Test
    void assign_duplicateAdmission_mapsTo409TidExists() {
        when(repository.existsByAdmissionId(77L)).thenReturn(true);

        assertThatThrownBy(() -> service.assign(77L, "TID-OTHER"))
                .isInstanceOf(TidConflictException.class)
                .extracting(e -> ((TidConflictException) e).getErrorCode())
                .isEqualTo("TID_EXISTS");
    }

    @Test
    void get_unknownAdmission_mapsTo404Code() {
        when(repository.findByAdmissionId(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(RcmNotFoundException.class)
                .extracting(e -> ((RcmNotFoundException) e).getErrorCode())
                .isEqualTo("NOT_FOUND");
    }

    @Test
    void get_knownAdmission_returnsRegisteredTid() {
        AdmissionTid stored = new AdmissionTid();
        stored.setTidId(1L);
        stored.setAdmissionId(77L);
        stored.setTid("TID-2026-0001");
        when(repository.findByAdmissionId(77L)).thenReturn(Optional.of(stored));

        assertThat(service.get(77L).getTid()).isEqualTo("TID-2026-0001");
    }
}
