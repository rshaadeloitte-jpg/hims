package com.hims.interop.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * e-Aushadhi drug catalogue sync queue row (register WP-T1 extension).
 * Forward-only status machine (pinned by tests):
 *   0=PENDING -> 1=SYNCED -> 2=FAILED
 * The actual e-Aushadhi API call is OUT OF SCOPE (comment-only per register);
 * the queue row + status machine ARE the deliverable.
 */
@Entity
@Table(name = "t_eaushadhi_sync_queue")
public class EAushadhiSyncQueue extends InteropBase {

    public static final short STATUS_PENDING = 0;
    public static final short STATUS_SYNCED = 1;
    public static final short STATUS_FAILED = 2;

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "drug_code", nullable = false, length = 64)
    private String drugCode;

    @Column(name = "drug_name", nullable = false, length = 255)
    private String drugName;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "sync_status", nullable = false)
    private Short syncStatus = STATUS_PENDING;

    @Column(name = "sync_attempts", nullable = false)
    private Integer syncAttempts = 0;

    @Column(name = "last_sync_at")
    private Timestamp lastSyncAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    /** Not persisted: transition history loaded via repository in the service layer. */
    @Transient
    private List<EAushadhiSyncStatus> history = new ArrayList<>();

    @Override public Long getId() { return id; }
    @Override public void setId(Long id) { this.id = id; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Short getSyncStatus() { return syncStatus; }
    public void setSyncStatus(Short syncStatus) { this.syncStatus = syncStatus; }
    public Integer getSyncAttempts() { return syncAttempts; }
    public void setSyncAttempts(Integer syncAttempts) { this.syncAttempts = syncAttempts; }
    public Timestamp getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Timestamp lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public List<EAushadhiSyncStatus> getHistory() { return history; }
    public void setHistory(List<EAushadhiSyncStatus> history) { this.history = history; }

    /** Forward-only machine: the single legal successor on the sync path. */
    public static Short nextForward(short status) {
        return switch (status) {
            case STATUS_PENDING -> STATUS_SYNCED;
            default -> null;
        };
    }

    /** FAILED (2) is legal ONLY from PENDING — never after SYNCED. */
    public boolean canFail() {
        return syncStatus == STATUS_PENDING;
    }
}
