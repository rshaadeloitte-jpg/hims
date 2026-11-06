package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.FuelStationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface FuelStationDetailsRepository extends JpaRepository<FuelStationDetails, Long> {

    /**
     * Legacy getFuelStationDetails: unit-scoped active records whose dateTime
     * falls in [fromDate, toDateInclusive]; optional vehicleType / vehicle
     * filters. Newest first. Same null-sentinel gating as the request search.
     */
    @Query("""
            SELECT f FROM FuelStationDetails f
            WHERE f.status = true
              AND (:hasUnit = false OR f.unitId = :unitId)
              AND (:hasFrom = false OR f.dateTime >= :fromDate)
              AND (:hasTo = false OR f.dateTime <= :toDateInclusive)
              AND (:hasVehicleType = false OR f.vehicleTypeId = :vehicleTypeId)
              AND (:hasVehicle = false OR f.vehicleId = :vehicleId)
            ORDER BY f.id DESC
            """)
    List<FuelStationDetails> search(@Param("unitId") Long unitId,
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
