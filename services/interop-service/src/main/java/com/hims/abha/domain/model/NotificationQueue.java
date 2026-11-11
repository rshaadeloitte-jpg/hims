package com.hims.abha.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_queue", schema = "ihms_abha")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationQueue extends AbhaBase {
    @Column(name = "notification_type")
    private String notificationType;

    @Lob
    @Column(name = "payload")
    private String payload;

    @Column(name = "status")
    private String status;

    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;
}