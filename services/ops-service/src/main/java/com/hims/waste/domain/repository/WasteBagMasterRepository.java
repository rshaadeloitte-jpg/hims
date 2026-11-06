package com.hims.waste.domain.repository;

import com.hims.waste.domain.model.WasteBagMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WasteBagMasterRepository extends JpaRepository<WasteBagMaster, Long> {

    List<WasteBagMaster> findByStatusTrueOrderByIdAsc();

    List<WasteBagMaster> findByUnitIdAndStatusTrueOrderByIdAsc(Long unitId);

    boolean existsByUnitIdAndCodeIgnoreCase(Long unitId, String code);
}
