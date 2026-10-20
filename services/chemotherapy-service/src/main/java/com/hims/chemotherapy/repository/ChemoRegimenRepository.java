package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.ChemoRegimen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface ChemoRegimenRepository extends JpaRepository<ChemoRegimen, Long> {
    List<ChemoRegimen> findByPlanId(Long planId);
}
