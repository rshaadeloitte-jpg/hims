package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.VehicleMaintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
public interface VehicleMaintenanceRepository extends JpaRepository<VehicleMaintenance, Long> {

    /**
     * Legacy getVehicleMaintenanceDetails: unit-scoped active records whose
     * requestDate falls in [fromDate, toDateInclusive]; optional vehicleType /
     * vehicle filters. Newest first. Same null-sentinel gating as the request
     * search.
     */
    @Query("""
            SELECT m FROM VehicleMaintenance m
            WHERE m.status = true
              AND (:hasUnit = false OR m.unitId = :unitId)
              AND (:hasFrom = false OR m.requestDate >= :fromDate)
              AND (:hasTo = false OR m.requestDate <= :toDateInclusive)
              AND (:hasVehicleType = false OR m.vehicleTypeId = :vehicleTypeId)
              AND (:hasVehicle = false OR m.vehicleId = :vehicleId)
            ORDER BY m.id DESC
            """)
    List<VehicleMaintenance> search(@Param("unitId") Long unitId,
                                    @Param("hasUnit") boolean hasUnit,
                                    @Param("fromDate") Timestamp fromDate,
                                    @Param("hasFrom") boolean hasFrom,
                                    @Param("toDateInclusive") Timestamp toDateInclusive,
                                    @Param("hasTo") boolean hasTo,
                                    @Param("vehicleTypeId") Long vehicleTypeId,
                                    @Param("hasVehicleType") boolean hasVehicleType,
                                    @Param("vehicleId") Long vehicleId,
                                    @Param("hasVehicle") boolean hasVehicle);
}
