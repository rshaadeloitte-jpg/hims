package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_notification")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppNotification extends AbhaBase {
    private String mobile;
    private String templateId;
    private String status;
    private String failureReason;
}