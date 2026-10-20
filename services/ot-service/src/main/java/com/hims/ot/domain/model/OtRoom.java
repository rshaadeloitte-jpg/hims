package com.hims.ot.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "t_ot_room")
public class OtRoom extends OtBase {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "room_name", nullable = false, length = 50)
    private String roomName;

    @Column(name = "room_type", length = 50)
    private String roomType;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getId() { return roomId; }
    public void setId(Long id) { this.roomId = id; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
