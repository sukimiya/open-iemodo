package com.iemodo.notification.domain;

import com.iemodo.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.relational.core.mapping.Table;

/**
 * DB-driven notification template.
 *
 * <p>Subject and body support simple {@code {{variable}}} placeholders
 * resolved at send time by {@link com.iemodo.notification.service.TemplateService}.
 *
 * <p>Unique on (type, channel, language) so that every combination has
 * exactly one template, and updating content requires no redeployment.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Table("notification_templates")
public class NotificationTemplate extends BaseEntity {

    private NotificationType type;
    private NotificationChannel channel;

    /** BCP-47 language tag: zh-CN, en, ja, … */
    private String language;

    /** Email subject line or SMS/Push title. Supports {{variables}}. */
    private String subject;

    /** Full message body. Supports {{variables}}. */
    private String body;

    private Boolean active;
}
