package com.iemodo.notification.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Persistent log of every notification attempt.
 * Enables deduplication, retry tracking, and audit.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("notification_records")
public class NotificationRecord extends BaseEntity {

    private String tenantId;
    private Long userId;

    private NotificationChannel channel;
    private NotificationType type;

    /** Email address / phone number / device token */
    private String recipient;

    private String subject;

    /** Rendered notification body (after template variable substitution). */
    private String body;

    /** PENDING | SENT | FAILED */
    private String sendStatus;

    private Integer retryCount;
    private String errorMessage;
    private Instant sentAt;

    public Instant getCreatedAt() { return getCreateTime(); }
}
