package com.hims.nursing.application;

import com.hims.nursing.domain.model.ShiftHandover;
import com.hims.nursing.domain.repository.ShiftHandoverRepository;
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

class ShiftHandoverServiceTest {

    private ShiftHandoverService service;
    private ShiftHandoverRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(ShiftHandoverRepository.class);
        service = new ShiftHandoverService(repo);
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seq = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seqVal = new java.util.concurrent.atomic.AtomicLong(0);
        when(seq.getSingleResult()).thenAnswer(inv -> seqVal.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seq);
        ReflectionTestUtils.setField(service, "em", em);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRequiresAdmission() {
        assertThatThrownBy(() -> service.create(null, 1L, "MORNING", null, null, null, null, null, 1L)).hasMessageContaining("admissionId");
    }

    @Test
    void createSuccess() {
        ShiftHandover h = service.create(101L, 5L, "MORNING", "Sit", "Bg", "Asmt", "Rec", "Tasks", 1L);
        assertThat(h.getAdmissionId()).isEqualTo(101L);
        assertThat(h.getShift()).isEqualTo("MORNING");
    }

    @Test
    void completeSignOffNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.completeSignOff(99L, 2L)).hasMessageContaining("NOT_FOUND");
    }
}
