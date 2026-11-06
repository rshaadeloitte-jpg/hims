package com.hims.transport.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Clean target of legacy t_vehiclemaintenance (VehicleMaintenance.java).
 * Maintenance request lifecycle: raised (vehicle flips isAvailable=false),
 * approved, completed (stamps actualCost + maintenanceDate, vehicle flips
 * isAvailable=true).
 */
@Entity
@Table(name = "vehicle_maintenance")
public class VehicleMaintenance extends TransportBase {

    @Id
    private Long id;

    @Column(name = "workshop_name")
    private String workshopName;

    @Column(name = "vehicle_id")
    private Long vehicleId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "request_date")
    private Timestamp requestDate;

    @Column(name = "reason")
    private String reason;

    @Column(name = "maintenance_date")
    private Timestamp maintenanceDate;

    @Column(name = "is_approved")
    private Boolean isApproved = false;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    @Column(name = "is_accidental_case")
    private Boolean isAccidentalCase = false;

    @Column(name = "date_of_accident")
    private Timestamp dateOfAccident;

    @Column(name = "accidental_cause")
    private String accidentalCause;

    @Column(name = "accidental_type")
    private String accidentalType;

    @Column(name = "accidental_type_id")
    private Long accidentalTypeId;

    @Column(name = "workshop_id")
    private Long workshopId;

    @Column(name = "repair_works")
    private String repairWorks;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getWorkshopName() { return workshopName; }
    public void setWorkshopName(String workshopName) { this.workshopName = workshopName; }
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }
    public Long getVehicleTypeId() { return vehicleTypeId; }
    public void setVehicleTypeId(Long vehicleTypeId) { this.vehicleTypeId = vehicleTypeId; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public Timestamp getRequestDate() { return requestDate; }
    public void setRequestDate(Timestamp requestDate) { this.requestDate = requestDate; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Timestamp getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(Timestamp maintenanceDate) { this.maintenanceDate = maintenanceDate; }
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }
    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public Boolean getIsAccidentalCase() { return isAccidentalCase; }
    public void setIsAccidentalCase(Boolean isAccidentalCase) { this.isAccidentalCase = isAccidentalCase; }
    public Timestamp getDateOfAccident() { return dateOfAccident; }
    public void setDateOfAccident(Timestamp dateOfAccident) { this.dateOfAccident = dateOfAccident; }
    public String getAccidentalCause() { return accidentalCause; }
    public void setAccidentalCause(String accidentalCause) { this.accidentalCause = accidentalCause; }
    public String getAccidentalType() { return accidentalType; }
    public void setAccidentalType(String accidentalType) { this.accidentalType = accidentalType; }
    public Long getAccidentalTypeId() { return accidentalTypeId; }
    public void setAccidentalTypeId(Long accidentalTypeId) { this.accidentalTypeId = accidentalTypeId; }
    public Long getWorkshopId() { return workshopId; }
    public void setWorkshopId(Long workshopId) { this.workshopId = workshopId; }
    public String getRepairWorks() { return repairWorks; }
    public void setRepairWorks(String repairWorks) { this.repairWorks = repairWorks; }
}
