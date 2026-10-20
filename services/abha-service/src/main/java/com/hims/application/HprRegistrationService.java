package com.hims.application;

import com.hims.domain.model.HprRegistration;
import com.hims.domain.repository.HprRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HprRegistrationService {
    private final HprRegistrationRepository repository;

    @Transactional(readOnly = true)
    public HprRegistration findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("HprRegistration not found: " + id));
    }

    public HprRegistration createRegistration(HprRegistration registration) {
        log.info("Creating HprRegistration");
        return repository.save(registration);
    }

    public Iterable<HprRegistration> findAll() {
        return repository.findAll();
    }
}