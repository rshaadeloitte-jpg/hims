package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "whatsapp_notification", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WhatsAppNotification extends AbhaBase {
    @Column(name = "mobile")
    private String mobile;

    @Column(name = "template_id")
    private String templateId;

    @Column(name = "status")
    private String status;

    @Column(name = "failure_reason")
    private String failureReason;
}