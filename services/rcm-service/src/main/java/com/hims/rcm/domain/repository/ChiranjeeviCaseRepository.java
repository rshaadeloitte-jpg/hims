package com.hims.rcm.domain.repository;

import com.hims.rcm.domain.model.ChiranjeeviCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChiranjeeviCaseRepository extends JpaRepository<ChiranjeeviCase, Long> {
    List<ChiranjeeviCase> findByPatientIdOrderByRequestedAtDesc(Long patientId);
    List<ChiranjeeviCase> findByDocStatusOrderByRequestedAtDesc(Short docStatus);
    @Query(value = "SELECT nextval('ihms_rcm.t_chiranjeevi_case_seq')", nativeQuery = true)
    long nextId();
}
