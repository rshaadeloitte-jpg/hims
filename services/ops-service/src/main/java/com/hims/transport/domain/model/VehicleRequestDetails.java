package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Clean target of legacy t_vehiclerequestdetails (TransportRequestDetails.java).
 * Vehicle-out / vehicle-in trip readings + fare for a vehicle request.
 */
@Entity
@Table(name = "vehicle_request_details")
public class VehicleRequestDetails extends TransportBase {

    @Id
    private Long id;

    @Column(name = "vehicle_request_id", nullable = false)
    private Long vehicleRequestId;

    @Column(name = "start_odm_reading")
    private Long startOdmReading;

    @Column(name = "end_odm_reading")
    private Long endOdmReading;

    @Column(name = "trip_distance_estimated")
    private Long tripDistanceEstimated;

    @Column(name = "trip_distance")
    private Long tripDistance;

    @Column(name = "calculated_trip_fare")
    private Long calculatedTripFare;

    @Column(name = "actual_fare")
    private Long actualFare;

    @Column(name = "charge_type")
    private Long chargeType;

    @Column(name = "out_time")
    private Timestamp outTime;

    @Column(name = "in_time")
    private Timestamp inTime;

    @Column(name = "fare_per_km")
    private BigDecimal farePerKm;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public Long getVehicleRequestId() { return vehicleRequestId; }
    public void setVehicleRequestId(Long vehicleRequestId) { this.vehicleRequestId = vehicleRequestId; }
    public Long getStartOdmReading() { return startOdmReading; }
    public void setStartOdmReading(Long startOdmReading) { this.startOdmReading = startOdmReading; }
    public Long getEndOdmReading() { return endOdmReading; }
    public void setEndOdmReading(Long endOdmReading) { this.endOdmReading = endOdmReading; }
    public Long getTripDistanceEstimated() { return tripDistanceEstimated; }
    public void setTripDistanceEstimated(Long tripDistanceEstimated) { this.tripDistanceEstimated = tripDistanceEstimated; }
    public Long getTripDistance() { return tripDistance; }
    public void setTripDistance(Long tripDistance) { this.tripDistance = tripDistance; }
    public Long getCalculatedTripFare() { return calculatedTripFare; }
    public void setCalculatedTripFare(Long calculatedTripFare) { this.calculatedTripFare = calculatedTripFare; }
    public Long getActualFare() { return actualFare; }
    public void setActualFare(Long actualFare) { this.actualFare = actualFare; }
    public Long getChargeType() { return chargeType; }
    public void setChargeType(Long chargeType) { this.chargeType = chargeType; }
    public Timestamp getOutTime() { return outTime; }
    public void setOutTime(Timestamp outTime) { this.outTime = outTime; }
    public Timestamp getInTime() { return inTime; }
    public void setInTime(Timestamp inTime) { this.inTime = inTime; }
    public BigDecimal getFarePerKm() { return farePerKm; }
    public void setFarePerKm(BigDecimal farePerKm) { this.farePerKm = farePerKm; }
}
