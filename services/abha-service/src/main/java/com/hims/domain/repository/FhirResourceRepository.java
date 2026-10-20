package com.hims.domain.repository;

import com.hims.domain.model.FhirResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FhirResourceRepository extends JpaRepository<FhirResource, Long> {
}