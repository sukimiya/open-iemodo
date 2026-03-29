package com.iemodo.notification.dto;

import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * Payload sent by other microservices to trigger a notification.
 *
 * <p>Example — order-service fires this after a successful order creation:
 * <pre>{@code
 * {
 *   "userId": 99,
 *   "tenantId": "tenant_001",
 *   "channel": "EMAIL",
 *   "type": "ORDER_CREATED",
 *   "recipient": "user@example.com",
 *   "language": "zh-CN",
 *   "variables": {
 *     "orderNo": "ORD20260329000001",
 *     "totalAmount": "99.00",
 *     "currency": "USD"
 *   }
 * }
 * }</pre>
 */
@Data
public class SendNotificationRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String tenantId;

    @NotNull
    private NotificationChannel channel;

    @NotNull
    private NotificationType type;

    /** Email address / phone number / FCM device token */
    @NotBlank
    private String recipient;

    /** BCP-47 language tag. Defaults to "zh-CN" if omitted. */
    private String language = "zh-CN";

    /**
     * Template variables substituted into {{key}} placeholders.
     * Callers provide exactly the variables the template expects.
     */
    private Map<String, String> variables;
}
