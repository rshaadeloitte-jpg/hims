package com.hims.appointment.api.dto;

public class TelemedicineRoomRequest {
    private Integer expiresInSeconds = 3600;
    public Integer getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(Integer expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
}
