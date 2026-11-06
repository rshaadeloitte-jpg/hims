package com.hims.waste.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Clean target of T_BIOMEDICALWASTEDETAILS — one row per physical waste bag.
 * Legacy raise expands a requested quantity into qty=1 rows, each carrying a
 * unique barcode_no ({yyyyMM} + zero-padded monthly counter, 13 chars).
 * bag_status shares the {@link WasteStatus} dictionary; firm_name /
 * contact_person / contact_no are stamped at process (legacy raw-SQL columns).
 * Server-owned id from bio_medical_waste_details_seq (IMP-DEC-003).
 */
@Entity
@Table(name = "bio_medical_waste_details")
public class BioMedicalWasteDetails extends WasteBase {

    @Id
    private Long id;

    @Column(name = "bio_medical_waste_id", nullable = false)
    private Long bioMedicalWasteId;

    @Column(name = "bag_id")
    private Long bagId;

    @Column(name = "quantity", nullable = false)
    private Long quantity = 1L;

    @Column(name = "remark")
    private String remark;

    @Column(name = "weight")
    private BigDecimal weight;

    @Column(name = "barcode_no")
    private String barcodeNo;

    @Column(name = "bag_status", nullable = false)
    private Long bagStatus = WasteStatus.PENDING;

    @Column(name = "handed_over_to")
    private String handedOverTo;

    @Column(name = "firm_name")
    private String firmName;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_no")
    private Long contactNo;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getBioMedicalWasteId() { return bioMedicalWasteId; }
    public void setBioMedicalWasteId(Long bioMedicalWasteId) { this.bioMedicalWasteId = bioMedicalWasteId; }
    public Long getBagId() { return bagId; }
    public void setBagId(Long bagId) { this.bagId = bagId; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getBarcodeNo() { return barcodeNo; }
    public void setBarcodeNo(String barcodeNo) { this.barcodeNo = barcodeNo; }
    public Long getBagStatus() { return bagStatus; }
    public void setBagStatus(Long bagStatus) { this.bagStatus = bagStatus; }
    public String getHandedOverTo() { return handedOverTo; }
    public void setHandedOverTo(String handedOverTo) { this.handedOverTo = handedOverTo; }
    public String getFirmName() { return firmName; }
    public void setFirmName(String firmName) { this.firmName = firmName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public Long getContactNo() { return contactNo; }
    public void setContactNo(Long contactNo) { this.contactNo = contactNo; }
}
