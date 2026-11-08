package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.GeoBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeoBlockRepository extends JpaRepository<GeoBlock, Long> {
    List<GeoBlock> findByDistrictIdOrderByNameAsc(Long districtId);
}
