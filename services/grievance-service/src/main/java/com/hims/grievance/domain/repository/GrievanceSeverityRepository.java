package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceSeverityRepository extends JpaRepository<GrievanceSeverity, Long> {

    List<GrievanceSeverity> findByStatusTrueOrderByIdAsc();
}
