package com.hims.chemotherapy.repository;
import com.hims.chemotherapy.domain.model.ChemoRegimenDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public interface ChemoRegimenDetailRepository extends JpaRepository<ChemoRegimenDetail, Long> {
    List<ChemoRegimenDetail> findByPlanId(Long planId);
}
