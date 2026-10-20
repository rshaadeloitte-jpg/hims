package com.hims.equipment.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Core asset register — clean target of t_equipmentdetails (unit-scoped stock
 * row) + M_EQUIPMENTDETAIL (asset identity: accession no, serial/model,
 * commissioning, custodian). {@code workingStatus} holds an
 * equipment_working_status_master id (see {@link EquipmentWorkingStatus},
 * default 1 = WORKING). Accession number is unique per unit (partial unique
 * index uq_equipment_unit_accession; NULL unit_id rows are distinct by PG
 * default — the service always stamps unit_id). Server-owned id from
 * ihms_equipment.equipment_seq (IMP-DEC-003). {@code documents} maps the
 * TEXT column (structured attachments live in equipment_detail_docs, Task 14).
 */
@Entity
@Table(name = "equipment")
public class Equipment extends EquipmentBase {

    @Id
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "accession_no")
    private String accessionNo;

    @Column(name = "serial_no")
    private String serialNo;

    @Column(name = "model")
    private String model;

    @Column(name = "make")
    private String make;

    @Column(name = "purchase_date")
    private Timestamp purchaseDate;

    @Column(name = "installation_date")
    private Timestamp installationDate;

    @Column(name = "warranty_expiry")
    private Timestamp warrantyExpiry;

    @Column(name = "working_status", nullable = false)
    private Long workingStatus = EquipmentWorkingStatus.WORKING;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "location")
    private String location;

    /** NUMERIC(19,2) in PostgreSQL. */
    @Column(name = "cost", precision = 19, scale = 2)
    private BigDecimal cost;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "custodian_name")
    private String custodianName;

    /** TEXT column — free-form document references. */
    @Column(name = "documents")
    private String documents;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public String getAccessionNo() { return accessionNo; }
    public void setAccessionNo(String accessionNo) { this.accessionNo = accessionNo; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public Timestamp getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Timestamp purchaseDate) { this.purchaseDate = purchaseDate; }
    public Timestamp getInstallationDate() { return installationDate; }
    public void setInstallationDate(Timestamp installationDate) { this.installationDate = installationDate; }
    public Timestamp getWarrantyExpiry() { return warrantyExpiry; }
    public void setWarrantyExpiry(Timestamp warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }
    public Long getWorkingStatus() { return workingStatus; }
    public void setWorkingStatus(Long workingStatus) { this.workingStatus = workingStatus; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getCustodianName() { return custodianName; }
    public void setCustodianName(String custodianName) { this.custodianName = custodianName; }
    public String getDocuments() { return documents; }
    public void setDocuments(String documents) { this.documents = documents; }
}
