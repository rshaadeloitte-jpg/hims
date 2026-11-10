package com.hims.abha.domain.repository;

import com.hims.abha.domain.model.FhirResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FhirResourceRepository extends JpaRepository<FhirResource, Long> {
}