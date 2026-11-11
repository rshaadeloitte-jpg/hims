package com.hims.abha.application;

import com.hims.abha.domain.model.HprVerification;
import com.hims.abha.domain.repository.HprVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HprVerificationService {
    private final HprVerificationRepository repository;

    public HprVerification verify(HprVerification verification) {
        log.info("Verifying HPR: {}", verification.getVerificationType());
        return repository.save(verification);
    }
}