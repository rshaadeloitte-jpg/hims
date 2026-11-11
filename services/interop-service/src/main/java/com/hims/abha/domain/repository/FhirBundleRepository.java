package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.FhirBundle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FhirBundleRepository extends JpaRepository<FhirBundle, Long> {
}