package com.hims.nursing.application;

import com.hims.nursing.domain.model.IncidentReport;
import com.hims.nursing.domain.repository.IncidentReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentReportServiceTest {

    private IncidentReportService service;
    private IncidentReportRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(IncidentReportRepository.class);
        service = new IncidentReportService(repo);
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seq = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seqVal = new java.util.concurrent.atomic.AtomicLong(0);
        when(seq.getSingleResult()).thenAnswer(inv -> seqVal.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seq);
        ReflectionTestUtils.setField(service, "em", em);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reportRequiresPatient() {
        assertThatThrownBy(() -> service.report(null, "Fall", "LOW", "desc", null, null, 1L)).hasMessageContaining("patientId");
    }

    @Test
    void reportSuccess() {
        IncidentReport ir = service.report(101L, "Fall", "MEDIUM", "Patient fell", "Ward A", null, 1L);
        assertThat(ir.getPatientId()).isEqualTo(101L);
        assertThat(ir.getReviewStatus()).isEqualTo(IncidentReport.REVIEW_PENDING);
    }

    @Test
    void reviewNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reviewStatus(99L, (short) 2, 1L, "action")).hasMessageContaining("NOT_FOUND");
    }
}
