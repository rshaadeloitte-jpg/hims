package com.hims.application;

import com.hims.domain.model.FhirResource;
import com.hims.domain.repository.FhirResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FhirR4Service {
    private final FhirResourceRepository repository;

    @Transactional(readOnly = true)
    public FhirResource findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("FhirResource not found: " + id));
    }

    public FhirResource createResource(FhirResource resource) {
        log.info("Creating FHIR resource: {}", resource.getResourceType());
        return repository.save(resource);
    }

    public Iterable<FhirResource> findAll() {
        return repository.findAll();
    }
}