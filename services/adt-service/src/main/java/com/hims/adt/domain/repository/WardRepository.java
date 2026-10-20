package com.hims.adt.domain.repository;

import com.hims.adt.domain.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WardRepository extends JpaRepository<Ward, Long> {

    List<Ward> findByUnitIdAndStatusTrue(Long unitId);
}
