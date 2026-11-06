package com.hims.equipment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Opening-balance register row (equipment_opening_balance, V1) — the legacy
 * T_EQUIPMENTOPENINGBALANCE stock take: how many units of an {@code item} a
 * store held when the system went live, at what rate and total value.
 *
 * <p>Column map: {@code itemId} is the NOT NULL item reference (cross-domain
 * FK to ihms_item deliberately deferred per V1 header), {@code openingBalance}
 * the BIGINT quantity (NOT NULL DEFAULT 0), {@code rate}/{@code amount} the
 * NUMERIC(19,2) unit value / extended value, {@code remarks} the TEXT column.
 * Server-owned id from ihms_equipment.equipment_opening_balance_seq
 * (IMP-DEC-003).
 *
 * <p>Deliberately NO Java initializer on {@code openingBalance}: the column
 * default lives in PG, and the service must detect key-absence to apply it —
 * an initializer would make the absent case indistinguishable from an explicit
 * null and Hibernate would send NULL straight into the NOT NULL column
 * (note #17 gotcha). Unlike asset-scoped children this row has NO equipment_id:
 * it is keyed by (unit_id, item_id) — see ix_equipment_opening_balance_unit_item.
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(name = "equipment_opening_balance")
public class EquipmentOpeningBalance extends EquipmentBase {

    @Id
    private Long id;

    /** Which stocked item this balance counts (NOT NULL — validated in service). */
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Quantity on hand at go-live; column DEFAULT 0 applied by service on key-absence. */
    @Column(name = "opening_balance", nullable = false)
    private Long openingBalance;

    /** NUMERIC(19,2) — per-unit valuation at go-live. */
    @Column(name = "rate", precision = 19, scale = 2)
    private BigDecimal rate;

    /** NUMERIC(19,2) — extended value (quantity x rate as counted by the store). */
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "accession_no")
    private String accessionNo;

    @Column(name = "serial_no")
    private String serialNo;

    @Column(name = "model")
    private String model;

    /** Store the balance was counted in (cross-domain ref deferred — no FK yet). */
    @Column(name = "store_id")
    private Long storeId;

    /** Supplier of the counted stock (cross-domain ref deferred — no FK yet). */
    @Column(name = "supplier_id")
    private Long supplierId;

    /** TEXT column — free-text stock-take remarks. */
    @Column(name = "remarks")
    private String remarks;

    @Override
    public Long getId() { return id; }
    @Override
    public void setId(Long id) { this.id = id; }
    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(Long openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getAccessionNo() { return accessionNo; }
    public void setAccessionNo(String accessionNo) { this.accessionNo = accessionNo; }
    public String getSerialNo() { return serialNo; }
    public void setSerialNo(String serialNo) { this.serialNo = serialNo; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
