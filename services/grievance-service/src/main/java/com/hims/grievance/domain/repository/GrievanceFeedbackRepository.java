package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceFeedbackRepository extends JpaRepository<GrievanceFeedback, Long> {

    List<GrievanceFeedback> findByGrievanceIdOrderByIdDesc(Long grievanceId);
}
