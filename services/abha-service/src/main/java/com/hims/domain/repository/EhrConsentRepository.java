package com.hims.domain.repository;

import com.hims.domain.model.EhrConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EhrConsentRepository extends JpaRepository<EhrConsent, Long> {
}