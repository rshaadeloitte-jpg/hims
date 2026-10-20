package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceCommentRepository extends JpaRepository<GrievanceComment, Long> {

    List<GrievanceComment> findByGrievanceIdOrderByIdDesc(Long grievanceId);
}
