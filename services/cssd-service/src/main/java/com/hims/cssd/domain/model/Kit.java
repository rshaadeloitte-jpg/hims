package com.hims.cssd.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Sterilization kit (register WP-R3, Catalog #15).
 *
 * Lifecycle status machine (SMALLINT):
 *   1=ASSEMBLED -&gt; (start cycle) -&gt; 2=STERILIZING -&gt; (complete PASS) -&gt; 3=STERILE
 *       -&gt; (issue) -&gt; 4=ISSUED
 *   2=STERILIZING -&gt; (complete FAIL) -&gt; 5=REJECTED (terminal, kit unusable: issue -&gt; 400)
 *   3=STERILE may re-enter a cycle (reprocess) -&gt; 2=STERILIZING.
 *   Cycle may only be STARTED from 1=ASSEMBLED or 3=STERILE; issue ONLY from 3=STERILE
 *   (one active issue per kit is enforced structurally: issuing moves the kit to 4=ISSUED,
 *   and no transition returns it to 3=STERILE without a new PASS cycle).
 */
@Entity
@Table(name = "t_kit")
public class Kit extends CssdBase {

    public static final short STATUS_ASSEMBLED = 1;
    public static final short STATUS_STERILIZING = 2;
    public static final short STATUS_STERILE = 3;
    public static final short STATUS_ISSUED = 4;
    public static final short STATUS_REJECTED = 5;

    public static final int DEFAULT_EXPIRY_DAYS = 14;

    @Id
    @Column(name = "kit_id")
    private Long kitId;

    @Column(name = "kit_code", nullable = false, length = 30)
    private String kitCode;

    @Column(name = "procedure_type", length = 50)
    private String procedureType;

    @Column(name = "status", nullable = false)
    private Short status = STATUS_ASSEMBLED;

    @Column(name = "sterilized_at")
    private Timestamp sterilizedAt;

    @Column(name = "expiry_days", nullable = false)
    private Integer expiryDays = DEFAULT_EXPIRY_DAYS;

    /** Not persisted: contents loaded via repository in the service layer. */
    @Transient
    private List<KitItem> items = new ArrayList<>();

    public Long getKitId() { return kitId; }
    public void setKitId(Long kitId) { this.kitId = kitId; }
    @Override public Long getId() { return kitId; }
    @Override public void setId(Long id) { this.kitId = id; }
    public String getKitCode() { return kitCode; }
    public void setKitCode(String kitCode) { this.kitCode = kitCode; }
    public String getProcedureType() { return procedureType; }
    public void setProcedureType(String procedureType) { this.procedureType = procedureType; }
    public Short getStatus() { return status; }
    public void setStatus(Short status) { this.status = status; }
    public Timestamp getSterilizedAt() { return sterilizedAt; }
    public void setSterilizedAt(Timestamp sterilizedAt) { this.sterilizedAt = sterilizedAt; }
    public Integer getExpiryDays() { return expiryDays; }
    public void setExpiryDays(Integer expiryDays) { this.expiryDays = expiryDays; }
    public List<KitItem> getItems() { return items; }
    public void setItems(List<KitItem> items) { this.items = items; }

    public boolean isAssembled() { return status == STATUS_ASSEMBLED; }
    public boolean isSterilizing() { return status == STATUS_STERILIZING; }
    public boolean isSterile() { return status == STATUS_STERILE; }
    public boolean isIssued() { return status == STATUS_ISSUED; }
    public boolean isRejected() { return status == STATUS_REJECTED; }

    /** True when a sterilization cycle may be STARTED from this status (assemble / sterile-reprocess). */
    public boolean canStartCycle() { return status == STATUS_ASSEMBLED || status == STATUS_STERILE; }
}
