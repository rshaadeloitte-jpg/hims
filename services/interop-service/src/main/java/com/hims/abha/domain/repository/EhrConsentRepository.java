package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.EhrConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EhrConsentRepository extends JpaRepository<EhrConsent, Long> {
}