package com.hims.nursing.application;

import com.hims.nursing.domain.model.NurseRoster;
import com.hims.nursing.domain.repository.NurseRosterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NurseRosterServiceTest {

    private NurseRosterService service;
    private NurseRosterRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(NurseRosterRepository.class);
        service = new NurseRosterService(repo);
        jakarta.persistence.EntityManager em = mock(jakarta.persistence.EntityManager.class);
        jakarta.persistence.Query seq = mock(jakarta.persistence.Query.class);
        java.util.concurrent.atomic.AtomicLong seqVal = new java.util.concurrent.atomic.AtomicLong(0);
        when(seq.getSingleResult()).thenAnswer(inv -> seqVal.incrementAndGet());
        when(em.createNativeQuery(org.mockito.ArgumentMatchers.contains("nextval"))).thenReturn(seq);
        ReflectionTestUtils.setField(service, "em", em);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assignRequiresRosterDate() {
        assertThatThrownBy(() -> service.assign(null, 1L, 1L, "MORNING", null, false)).hasMessageContaining("rosterDate");
    }

    @Test
    void assignSuccess() {
        when(repo.findByRosterDateAndWardIdAndShift(any(), any(), any())).thenReturn(Collections.emptyList());
        NurseRoster r = service.assign(Date.valueOf("2026-09-10"), 5L, 101L, "MORNING", 10L, false);
        assertThat(r.getNurseId()).isEqualTo(101L);
        assertThat(r.getShift()).isEqualTo("MORNING");
    }

    @Test
    void assignAlreadyAssigned() {
        NurseRoster existing = new NurseRoster(); existing.setNurseId(101L);
        when(repo.findByRosterDateAndWardIdAndShift(any(), any(), any())).thenReturn(java.util.List.of(existing));
        assertThatThrownBy(() -> service.assign(Date.valueOf("2026-09-10"), 5L, 101L, "MORNING", null, false)).hasMessageContaining("ALREADY_ASSIGNED");
    }

    @Test
    void removeAssignmentNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeAssignment(99L)).hasMessageContaining("NOT_FOUND");
    }

    @Test
    void listByNurseAndDateRequiresParams() {
        assertThatThrownBy(() -> service.listByNurseAndDate(null, Date.valueOf("2026-09-10"))).hasMessageContaining("nurseId");
    }
}
