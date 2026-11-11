package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_notification", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppNotification extends AbhaBase {
    private String mobile;
    private String templateId;
    private String status;
    private String failureReason;
}