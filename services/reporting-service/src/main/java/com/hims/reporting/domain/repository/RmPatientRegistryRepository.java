package com.hims.reporting.domain.repository;

import com.hims.reporting.domain.model.RmPatientRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read model access for the patient registry (PK is the SOURCE patient id — upsert semantics). */
public interface RmPatientRegistryRepository extends JpaRepository<RmPatientRegistry, Long> {
}
