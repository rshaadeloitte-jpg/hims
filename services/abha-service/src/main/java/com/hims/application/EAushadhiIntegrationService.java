package com.hims.application;

import com.hims.domain.model.EAushadhiDrug;
import com.hims.domain.repository.EAushadhiDrugRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EAushadhiIntegrationService {
    private final EAushadhiDrugRepository repository;

    @Transactional(readOnly = true)
    public EAushadhiDrug findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("EAushadhiDrug not found: " + id));
    }

    public EAushadhiDrug addDrug(EAushadhiDrug drug) {
        log.info("Adding drug: {}", drug.getDrugCode());
        return repository.save(drug);
    }

    public Iterable<EAushadhiDrug> findAll() {
        return repository.findAll();
    }
}