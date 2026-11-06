package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceCategoryRepository extends JpaRepository<GrievanceCategory, Long> {

    List<GrievanceCategory> findByStatusTrueOrderByIdAsc();
}
