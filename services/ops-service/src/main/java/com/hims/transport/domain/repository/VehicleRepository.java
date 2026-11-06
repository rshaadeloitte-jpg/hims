package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Legacy getAllVehicleDetails: unit-scoped active vehicles, optionally
     * narrowed by vehicleType (legacy matched the vehicletype string column
     * against the passed vehicleTypeId — here vehicleType is the type id as a
     * string; the gate keeps the param unbound when absent).
     */
    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status = true
              AND (:hasUnit = false OR v.unitId = :unitId)
              AND (:hasType = false OR v.vehicleType = :vehicleType)
            ORDER BY v.id ASC
            """)
    List<Vehicle> findScoped(@Param("unitId") Long unitId,
                             @Param("hasUnit") boolean hasUnit,
                             @Param("vehicleType") String vehicleType,
                             @Param("hasType") boolean hasType);
}
