package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of M_STORE (AGT-02 §5.4 / V1 ihms_item) — stock ownership root. MIG-012. */
@Entity
@Table(name = "store")
public class Store extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "code", nullable = false) private String code;
    @Column(name = "description", nullable = false) private String description;
    @Column(name = "is_main_store") private Boolean isMainStore = false;
    @Column(name = "is_central_store") private Boolean isCentralStore = false;
    @Column(name = "is_pharmacy_store") private Boolean isPharmacyStore = false;
    @Column(name = "is_cssd_store") private Boolean isCssdStore = false;
    @Column(name = "is_implant_store") private Boolean isImplantStore = false;
    @Column(name = "is_asset_store") private Boolean isAssetStore = false;
    @Column(name = "is_sub_store") private Boolean isSubStore = false;
    @Column(name = "is_patient_requisition") private Boolean isPatientRequisition = false;
    @Column(name = "item_group_id") private Long itemGroupId;
    @Column(name = "wing_id") private Long wingId;
    @Column(name = "floor_id") private Long floorId;
    @Column(name = "prefix") private String prefix;
    @Column(name = "sequence_name") private String sequenceName;
    @Column(name = "gst_no") private String gstNo;
    @Column(name = "pharmacy_license_no") private String pharmacyLicenseNo;
    @Column(name = "phone_no") private String phoneNo;
    @Column(name = "email") private String email;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
