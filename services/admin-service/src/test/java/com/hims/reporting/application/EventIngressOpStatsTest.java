package com.hims.reporting.application;

import com.hims.reporting.domain.model.RmOpStats;
import com.hims.reporting.domain.repository.IngestedEventRepository;
import com.hims.reporting.domain.repository.RmEncounterStatsRepository;
import com.hims.reporting.domain.repository.RmOpStatsRepository;
import com.hims.reporting.domain.repository.RmPatientRegistryRepository;
import com.hims.reporting.domain.repository.RmRevenueDailyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventIngressOpStatsTest {
    private RmPatientRegistryRepository patientRepository;
    private RmEncounterStatsRepository encounterRepository;
    private RmRevenueDailyRepository revenueRepository;
    private RmOpStatsRepository opStatsRepository;
    private IngestedEventRepository ledgerRepository;
    private EventIngressService service;
    private final Set<String> ledger = new HashSet<>();
    private final Map<String, RmOpStats> store = new HashMap<>();

    @BeforeEach void setUp() {
        patientRepository = mock(RmPatientRegistryRepository.class);
        encounterRepository = mock(RmEncounterStatsRepository.class);
        revenueRepository = mock(RmRevenueDailyRepository.class);
        opStatsRepository = mock(RmOpStatsRepository.class);
        ledgerRepository = mock(IngestedEventRepository.class);
        service = new EventIngressService(patientRepository, encounterRepository,
                revenueRepository, opStatsRepository, ledgerRepository);
        EntityManager em = mock(EntityManager.class);
        ReflectionTestUtils.setField(service, "em", em);
        Query seq = mock(Query.class);
        when(seq.getSingleResult()).thenAnswer(inv -> 1L);
        when(em.createNativeQuery(contains("nextval"))).thenReturn(seq);
        when(ledgerRepository.existsByEventTypeAndEventKey(any(), any()))
                .thenAnswer(inv -> !ledger.add(inv.getArgument(0) + "|" + inv.getArgument(1)));
        when(opStatsRepository.findById(any())).thenAnswer(
                inv -> java.util.Optional.ofNullable(store.get(((RmOpStats.Key) inv.getArgument(0)).getOpType() + "|" + ((RmOpStats.Key) inv.getArgument(0)).getStatDate())));
        when(opStatsRepository.save(any())).thenAnswer(inv -> {
            RmOpStats s = inv.getArgument(0);
            store.put(s.getId().getOpType() + "|" + s.getId().getStatDate(), s);
            return s;
        });
    }

    @Test void otCompleted_incrementsAndIsIdempotent() {
        var r1 = service.ingest("OtCompleted", Map.of("opId", "OP-1"));
        assertThat(r1.status()).isEqualTo("APPLIED");
        var r2 = service.ingest("OtCompleted", Map.of("opId", "OP-1"));
        assertThat(r2.status()).isEqualTo("DUPLICATE");
        assertThat(store.values().iterator().next().getCnt()).isEqualTo(1);
    }

    @Test void labVerified_defaultsToLabOpType() {
        var r = service.ingest("LabVerified", Map.of("opId", "L-1"));
        assertThat(r.status()).isEqualTo("APPLIED");
        var saved = store.values().iterator().next();
        assertThat(saved.getId().getOpType()).isEqualTo("LAB");
    }

    @Test void bloodIssued_defaultsToBloodOpType() {
        var r = service.ingest("BloodBagIssued", Map.of("opId", "B-1"));
        assertThat(r.status()).isEqualTo("APPLIED");
        var saved = store.values().iterator().next();
        assertThat(saved.getId().getOpType()).isEqualTo("BLOOD");
    }
}
