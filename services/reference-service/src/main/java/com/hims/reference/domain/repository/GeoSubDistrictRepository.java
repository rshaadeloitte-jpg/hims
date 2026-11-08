package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.GeoSubDistrict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeoSubDistrictRepository extends JpaRepository<GeoSubDistrict, Long> {
    List<GeoSubDistrict> findByDistrictIdOrderByNameAsc(Long districtId);
}
