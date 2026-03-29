package com.iemodo.notification.dto;

import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationDTO {
    private Long id;
    private String tenantId;
    private Long userId;
    private NotificationChannel channel;
    private NotificationType type;
    private String recipient;
    private String subject;
    private String sendStatus;
    private Integer retryCount;
    private Instant sentAt;
    private Instant createdAt;
}
