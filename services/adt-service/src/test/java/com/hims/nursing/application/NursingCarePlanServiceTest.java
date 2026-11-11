package com.hims.nursing.application;

import com.hims.nursing.domain.model.CarePlan;
import com.hims.nursing.domain.repository.CarePlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NursingCarePlanServiceTest {

    private NursingCarePlanService service;
    private CarePlanRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(CarePlanRepository.class);
        service = new NursingCarePlanService(repo);
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
        assertThatThrownBy(() -> service.create(null, "diag", null, 1L)).hasMessageContaining("admissionId");
    }

    @Test
    void createSuccess() {
        CarePlan cp = service.create(101L, "Pain", "Reduce pain", 1L);
        assertThat(cp.getAdmissionId()).isEqualTo(101L);
        assertThat(cp.getStatusCode()).isEqualTo(CarePlan.STATUS_ACTIVE);
    }

    @Test
    void updateStatusNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus(99L, (short) 2)).hasMessageContaining("NOT_FOUND");
    }

    @Test
    void addIntervention() {
        CarePlan cp = new CarePlan(); cp.setCarePlanId(1L); cp.setInterventions("[]");
        when(repo.findById(1L)).thenReturn(Optional.of(cp));
        CarePlan updated = service.addIntervention(1L, "Administer analgesic");
        assertThat(updated.getInterventions()).contains("Administer analgesic");
    }
}
