package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.ChemoCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface ChemoCycleRepository extends JpaRepository<ChemoCycle, Long> {
    List<ChemoCycle> findByPlanId(Long planId);
    List<ChemoCycle> findByPlanIdAndStatus(Long planId, String status);
}
