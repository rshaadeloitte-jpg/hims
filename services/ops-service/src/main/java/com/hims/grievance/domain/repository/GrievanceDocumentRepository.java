package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceDocumentRepository extends JpaRepository<GrievanceDocument, Long> {

    List<GrievanceDocument> findByGrievanceIdOrderByIdAsc(Long grievanceId);
}
