package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_queue", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationQueue extends AbhaBase {
    private String notificationType;
    @Lob
    private String payload;
    private String status;
    @Builder.Default
    private Integer retryCount = 0;
}