package com.hims.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_queue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationQueue extends AbhaBase {
    private String notificationType;
    @Lob
    private String payload;
    private String status;
    @Builder.Default
    private Integer retryCount = 0;
}