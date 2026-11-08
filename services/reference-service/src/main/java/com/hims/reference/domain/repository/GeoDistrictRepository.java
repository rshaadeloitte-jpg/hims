package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.GeoDistrict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GeoDistrictRepository extends JpaRepository<GeoDistrict, Long> {
    List<GeoDistrict> findByStateIdOrderByNameAsc(Long stateId);
}
