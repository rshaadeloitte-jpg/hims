package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.VehicleRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRequestTypeRepository extends JpaRepository<VehicleRequestType, Long> {

    /** Legacy getTripRequestCount dictionary lookup (emergency/normal/scheduled). */
    List<VehicleRequestType> findByStatusTrue();
}
