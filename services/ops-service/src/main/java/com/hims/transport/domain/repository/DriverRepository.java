package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    /** Legacy getDriverDetails: unit-scoped active drivers. */
    List<Driver> findByUnitIdAndStatusTrueOrderByIdAsc(Long unitId);
}
