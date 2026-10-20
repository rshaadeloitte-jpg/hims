package com.hims.domain.repository;

import com.hims.domain.model.FhirBundle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FhirBundleRepository extends JpaRepository<FhirBundle, Long> {
}