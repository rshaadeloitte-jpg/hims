package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Clean target of legacy t_fuelstationdetails (FuelStationDetails.java). Fuel
 * fill-up record for a vehicle.
 */
@Entity
@Table(name = "fuel_station_details")
public class FuelStationDetails extends TransportBase {

    @Id
    private Long id;

    @Column(name = "station_details")
    private String stationDetails;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "qty")
    private BigDecimal qty;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "fuel_type")
    private Long fuelType;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "date_time")
    private Timestamp dateTime;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getStationDetails() { return stationDetails; }
    public void setStationDetails(String stationDetails) { this.stationDetails = stationDetails; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }
    public BigDecimal getQty() { return qty; }
    public void setQty(BigDecimal qty) { this.qty = qty; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public Long getFuelType() { return fuelType; }
    public void setFuelType(Long fuelType) { this.fuelType = fuelType; }
    public String getReceiptNo() { return receiptNo; }
    public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
    public Timestamp getDateTime() { return dateTime; }
    public void setDateTime(Timestamp dateTime) { this.dateTime = dateTime; }
}
