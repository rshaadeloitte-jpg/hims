package com.hims.appointment.domain.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.sql.Timestamp;

/**
 * Telemedicine consultation room (stub behind app.telemedicine.enabled=false).
 * roomStatus: 1=CREATED, 2=ACTIVE, 3=ENDED, 4=EXPIRED
 * Real video provider (Jitsi/Twilio) integration deferred.
 */
@Entity
@Table(name = "t_telemedicine_room")
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemedicineRoom extends AppointmentBase {

    @Id
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "encounter_id")
    private Long encounterId;

    @Column(name = "room_id", nullable = false, unique = true, length = 64)
    private String roomId;

    @Column(name = "video_link", length = 512)
    private String videoLink;

    /** Room status: 1=CREATED, 2=ACTIVE, 3=ENDED, 4=EXPIRED */
    @Column(name = "room_status", nullable = false)
    private Short roomStatus = 1;

    @Column(name = "started_at")
    private Timestamp startedAt;

    @Column(name = "ended_at")
    private Timestamp endedAt;

    @Column(name = "expires_at")
    private Timestamp expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getEncounterId() { return encounterId; }
    public void setEncounterId(Long encounterId) { this.encounterId = encounterId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getVideoLink() { return videoLink; }
    public void setVideoLink(String videoLink) { this.videoLink = videoLink; }
    public Short getRoomStatus() { return roomStatus; }
    public void setRoomStatus(Short roomStatus) { this.roomStatus = roomStatus; }
    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }
    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }
    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
