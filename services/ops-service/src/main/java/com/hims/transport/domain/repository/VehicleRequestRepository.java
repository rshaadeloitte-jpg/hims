package com.hims.transport.domain.repository;

import com.hims.transport.domain.model.VehicleRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;

public interface VehicleRequestRepository extends JpaRepository<VehicleRequest, Long> {

    /**
     * Legacy getTripRequestDetails search semantics: unit-scoped, active
     * (status=true) requests whose requestDateTime falls in [fromDate,
     * toDateExclusive); optional requestType filter; optional case-insensitive
     * LIKE on requisitionBy (legacy also matched doctor/staff names via
     * cross-domain lookups — deferred); optional mobileNo filter; optional
     * isDoctorRequest filter. Newest first.
     *
     * <p>All filter params are gated by booleans and never bound as null:
     * PostgreSQL cannot infer the type of an untyped null parameter (binds as
     * bytea and breaks UPPER()/= comparisons), so the service passes sentinel
     * values (0L / "" / epoch) that are ignored when the matching gate is false.
     */
    @Query("""
            SELECT r FROM VehicleRequest r
            WHERE r.status = true
              AND (:hasUnit = false OR r.unitId = :unitId)
              AND (:hasFrom = false OR r.requestDateTime >= :fromDate)
              AND (:hasTo = false OR r.requestDateTime < :toDateExclusive)
              AND (:hasType = false OR r.requestType = :requestType)
              AND (:hasSearch = false
                   OR UPPER(r.requisitionBy) LIKE CONCAT('%', UPPER(:search), '%'))
              AND (:hasMobile = false OR r.mobileNo = :mobileNo)
              AND (:hasDoctorFlag = false OR r.isDoctorRequest = :isDoctorRequest)
            ORDER BY r.id DESC
            """)
    List<VehicleRequest> search(@Param("unitId") Long unitId,
                                @Param("hasUnit") boolean hasUnit,
                                @Param("fromDate") Timestamp fromDate,
                                @Param("hasFrom") boolean hasFrom,
                                @Param("toDateExclusive") Timestamp toDateExclusive,
                                @Param("hasTo") boolean hasTo,
                                @Param("requestType") Long requestType,
                                @Param("hasType") boolean hasType,
                                @Param("search") String search,
                                @Param("hasSearch") boolean hasSearch,
                                @Param("mobileNo") Long mobileNo,
                                @Param("hasMobile") boolean hasMobile,
                                @Param("isDoctorRequest") Boolean isDoctorRequest,
                                @Param("hasDoctorFlag") boolean hasDoctorFlag);

    /**
     * Legacy getTripRequestCount: count active requests in [fromDate,
     * toDateExclusive) for a unit, optionally narrowed to one requestType.
     * Same null-sentinel gating as {@link #search}.
     */
    @Query("""
            SELECT COUNT(r) FROM VehicleRequest r
            WHERE r.status = true
              AND (:hasUnit = false OR r.unitId = :unitId)
              AND (:hasFrom = false OR r.requestDateTime >= :fromDate)
              AND (:hasTo = false OR r.requestDateTime < :toDateExclusive)
              AND (:hasType = false OR r.requestType = :requestType)
            """)
    long countScoped(@Param("unitId") Long unitId,
                     @Param("hasUnit") boolean hasUnit,
                     @Param("fromDate") Timestamp fromDate,
                     @Param("hasFrom") boolean hasFrom,
                     @Param("toDateExclusive") Timestamp toDateExclusive,
                     @Param("hasTo") boolean hasTo,
                     @Param("requestType") Long requestType,
                     @Param("hasType") boolean hasType);

    /**
     * Legacy getVehicleOutDetails: unit-scoped, active, non-cancelled requests
     * that have a vehicle allocated (vehicleId not null). When isVehicleInReq
     * the list shows trips that are out and not yet completed; otherwise it
     * shows trips not yet out. Optional vehicleType / vehicle / mobileNo
     * filters. Newest first.
     */
    @Query("""
            SELECT r FROM VehicleRequest r
            WHERE r.status = true
              AND r.isCancelled = false
              AND r.vehicleId IS NOT NULL
              AND (:hasUnit = false OR r.unitId = :unitId)
              AND (:hasVehicleType = false OR r.vehicleTypeId = :vehicleTypeId)
              AND (:hasVehicle = false OR r.vehicleId = :vehicleId)
              AND (:hasMobile = false OR r.mobileNo = :mobileNo)
              AND ((:vehicleInReq = true AND r.isVehicleOut = true AND r.completed = false)
                   OR (:vehicleInReq = false AND r.isVehicleOut = false))
            ORDER BY r.id DESC
            """)
    List<VehicleRequest> vehicleOutList(@Param("unitId") Long unitId,
                                        @Param("hasUnit") boolean hasUnit,
                                        @Param("vehicleTypeId") Long vehicleTypeId,
                                        @Param("hasVehicleType") boolean hasVehicleType,
                                        @Param("vehicleId") Long vehicleId,
                                        @Param("hasVehicle") boolean hasVehicle,
                                        @Param("mobileNo") Long mobileNo,
                                        @Param("hasMobile") boolean hasMobile,
                                        @Param("vehicleInReq") boolean vehicleInReq);
}
