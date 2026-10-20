package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.ChemoCoversheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface ChemoCoversheetRepository extends JpaRepository<ChemoCoversheet, Long> {
    Optional<ChemoCoversheet> findByPlanId(Long planId);
}
