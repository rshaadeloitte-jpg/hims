package com.hims.reference.domain.repository;

import com.hims.reference.domain.model.GeoVillage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GeoVillageRepository extends JpaRepository<GeoVillage, Long> {

    Page<GeoVillage> findBySubDistrictId(Long subDistrictId, Pageable pageable);

    Page<GeoVillage> findBySubDistrictIdAndNameContainingIgnoreCase(Long subDistrictId, String q, Pageable pageable);

    @Query("SELECT v FROM GeoVillage v WHERE v.subDistrictId = :subDistrictId AND LOWER(v.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<GeoVillage> searchByName(@Param("subDistrictId") Long subDistrictId, @Param("q") String q, Pageable pageable);
}
