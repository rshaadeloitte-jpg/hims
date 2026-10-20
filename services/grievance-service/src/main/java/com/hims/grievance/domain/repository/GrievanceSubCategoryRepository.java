package com.hims.grievance.domain.repository;

import com.hims.grievance.domain.model.GrievanceSubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GrievanceSubCategoryRepository extends JpaRepository<GrievanceSubCategory, Long> {

    List<GrievanceSubCategory> findByCategoryIdOrderByIdAsc(Long categoryId);
}
