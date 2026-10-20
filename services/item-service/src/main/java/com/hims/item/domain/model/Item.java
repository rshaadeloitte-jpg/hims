package com.hims.item.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Clean target of M_ITEMMASTER (AGT-02 §5.3 / V1 ihms_item). MIG-012. */
@Entity
@Table(name = "item")
public class Item extends ItemBase {

    @Id
    @Column(name = "id")
    private Long id;

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }

    @Column(name = "item_code") private String itemCode;
    @Column(name = "item_name", nullable = false) private String itemName;
    @Column(name = "item_name_hindi") private String itemNameHindi;
    @Column(name = "item_name_ref_id") private Long itemNameRefId;
    @Column(name = "brand_name") private String brandName;
    @Column(name = "brand_id") private Long brandId;
    @Column(name = "strength") private String strength;
    @Column(name = "strength_uom_id") private Long strengthUomId;
    @Column(name = "molecule_id") private Long moleculeId;
    @Column(name = "item_group_id") private Long itemGroupId;
    @Column(name = "item_category_id") private Long itemCategoryId;
    @Column(name = "dispensing_type_id") private Long dispensingTypeId;
    @Column(name = "storage_type_id") private Long storageTypeId;
    @Column(name = "pregnancy_class_id") private Long pregnancyClassId;
    @Column(name = "therapeutic_class_id") private Long therapeuticClassId;
    @Column(name = "route_id") private Long routeId;
    @Column(name = "code_type_id") private Long codeTypeId;
    @Column(name = "manufacturer_id") private Long manufacturerId;
    @Column(name = "marketer_id") private Long marketerId;
    @Column(name = "purchase_rate") private java.math.BigDecimal purchaseRate;
    @Column(name = "mrp") private java.math.BigDecimal mrp;
    @Column(name = "sale_price") private java.math.BigDecimal salePrice;
    @Column(name = "vat_percent") private java.math.BigDecimal vatPercent;
    @Column(name = "vat_applicable_on") private Short vatApplicableOn;
    @Column(name = "inclusive_of_tax") private Boolean inclusiveOfTax = false;
    @Column(name = "discount_on_sale") private java.math.BigDecimal discountOnSale;
    @Column(name = "guide_cost") private java.math.BigDecimal guideCost;
    @Column(name = "packing_uom_id") private Long packingUomId;
    @Column(name = "stocking_uom_id") private Long stockingUomId;
    @Column(name = "conversion_factor") private java.math.BigDecimal conversionFactor;
    @Column(name = "batches_required") private Boolean batchesRequired = false;
    @Column(name = "reorder_qty") private Integer reorderQty;
    @Column(name = "avg_daily_demand") private Long avgDailyDemand;
    @Column(name = "safety_stock") private Long safetyStock;
    @Column(name = "is_reorder") private Boolean isReorder = false;
    @Column(name = "otc") private Boolean otc = false;
    @Column(name = "nursing_orderable") private Boolean nursingOrderable = true;
    @Column(name = "doctor_orderable") private Boolean doctorOrderable = true;
    @Column(name = "used_for_kit") private Boolean usedForKit = false;
    @Column(name = "used_for_cssd") private Boolean usedForCssd = false;
    @Column(name = "decimal_consumption") private Boolean decimalConsumption = false;
    @Column(name = "is_reusable") private Boolean isReusable = false;
    @Column(name = "is_kit") private Boolean isKit = false;
    @Column(name = "partial_dispensing") private Boolean partialDispensing = false;
    @Column(name = "is_compound") private Boolean isCompound = false;
    @Column(name = "is_custom_order") private Boolean isCustomOrder = false;
    @Column(name = "is_non_stockable") private Boolean isNonStockable = false;
    @Column(name = "controlled_substance") private Boolean controlledSubstance = false;
    @Column(name = "is_life_saving") private Boolean isLifeSaving = false;
    @Column(name = "is_high_risk") private Boolean isHighRisk = false;
    @Column(name = "is_high_cost") private Boolean isHighCost = false;
    @Column(name = "is_asset") private Boolean isAsset = false;
    @Column(name = "ved_class") private Short vedClass;
    @Column(name = "inventory_code") private String inventoryCode;
    @Column(name = "unique_country_drug_code") private String uniqueCountryDrugCode;
    @Column(name = "hsn_code_id") private String hsnCodeId;
    @Column(name = "snomed_code") private String snomedCode;
    @Column(name = "snomed_display") private String snomedDisplay;
    @Column(name = "edl_number") private String edlNumber;
    @Column(name = "order_ien") private String orderIen;
    @Column(name = "eaushadhi_item_id") private Long eaushadhiItemId;
    @Column(name = "document_name") private String documentName;
    @Column(name = "source_url") private String sourceUrl;
    @Column(name = "document") private byte[] document;
    @Column(name = "suggestions") private String suggestions;
    @Column(name = "remarks") private String remarks;

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public Long getItemGroupId() { return itemGroupId; }
    public void setItemGroupId(Long itemGroupId) { this.itemGroupId = itemGroupId; }
    public Boolean getBatchesRequired() { return batchesRequired; }
    public void setBatchesRequired(Boolean batchesRequired) { this.batchesRequired = batchesRequired; }
}
