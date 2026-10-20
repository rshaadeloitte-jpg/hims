package com.hims.adt.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Planned-admission bed hold. One ACTIVE reservation per bed is enforced by a
 * partial unique index in the DB; the service layer mirrors the status machine:
 * ACTIVE -> CONSUMED (admission of the reserved patient) | CANCELLED | EXPIRED.
 */
@Entity
@Table(name = "t_bed_reservation")
public class BedReservation {

    public enum ResStatus { ACTIVE, CONSUMED, CANCELLED, EXPIRED }

    @Id
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "bed_id", nullable = false)
    private Long bedId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "reserved_by")
    private Long reservedBy;

    @Column(name = "reserved_at", nullable = false)
    private Timestamp reservedAt;

    @Column(name = "expires_at", nullable = false)
    private Timestamp expiresAt;

    @Column(name = "released_at")
    private Timestamp releasedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "res_status", nullable = false)
    private ResStatus resStatus = ResStatus.ACTIVE;

    public boolean isActiveAt(Timestamp now) {
        return resStatus == ResStatus.ACTIVE && expiresAt.after(now);
    }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getBedId() { return bedId; }
    public void setBedId(Long bedId) { this.bedId = bedId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getReservedBy() { return reservedBy; }
    public void setReservedBy(Long reservedBy) { this.reservedBy = reservedBy; }
    public Timestamp getReservedAt() { return reservedAt; }
    public void setReservedAt(Timestamp reservedAt) { this.reservedAt = reservedAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
    public Timestamp getReleasedAt() { return releasedAt; }
    public void setReleasedAt(Timestamp releasedAt) { this.releasedAt = releasedAt; }
    public ResStatus getResStatus() { return resStatus; }
    public void setResStatus(ResStatus resStatus) { this.resStatus = resStatus; }
}
