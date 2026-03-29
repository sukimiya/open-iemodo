package com.iemodo.notification.service;

import com.iemodo.notification.channel.ChannelSender;
import com.iemodo.notification.config.ChannelSenderRegistry;
import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationRecord;
import com.iemodo.notification.domain.NotificationTemplate;
import com.iemodo.notification.domain.NotificationType;
import com.iemodo.notification.dto.NotificationDTO;
import com.iemodo.notification.dto.SendNotificationRequest;
import com.iemodo.notification.repository.NotificationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private TemplateService templateService;
    @Mock private ChannelSenderRegistry senderRegistry;
    @Mock private NotificationRecordRepository recordRepository;
    @Mock private ChannelSender emailSender;

    @InjectMocks
    private NotificationService notificationService;

    private NotificationTemplate renderedTemplate;
    private SendNotificationRequest request;

    @BeforeEach
    void setUp() {
        renderedTemplate = NotificationTemplate.builder()
                .type(NotificationType.ORDER_CREATED)
                .channel(NotificationChannel.EMAIL)
                .language("zh-CN")
                .subject("订单 ORD001 已创建")
                .body("<p>您好，张三</p>")
                .active(true)
                .build();

        request = new SendNotificationRequest();
        request.setUserId(1L);
        request.setTenantId("tenant_001");
        request.setChannel(NotificationChannel.EMAIL);
        request.setType(NotificationType.ORDER_CREATED);
        request.setRecipient("user@example.com");
        request.setLanguage("zh-CN");
        request.setVariables(Map.of("orderNo", "ORD001", "userName", "张三"));
    }

    private NotificationRecord savedRecord(String status) {
        NotificationRecord r = new NotificationRecord();
        r.setId(1L);
        r.setTenantId("tenant_001");
        r.setUserId(1L);
        r.setChannel(NotificationChannel.EMAIL);
        r.setType(NotificationType.ORDER_CREATED);
        r.setRecipient("user@example.com");
        r.setSendStatus(status);
        r.setRetryCount(0);
        return r;
    }

    @Test
    void send_success_persistsRecordAsSent() {
        NotificationRecord pending = savedRecord("PENDING");
        NotificationRecord sent = savedRecord("SENT");
        sent.setSubject("订单 ORD001 已创建");

        when(recordRepository.save(any())).thenReturn(Mono.just(pending), Mono.just(sent));
        when(templateService.resolve(any(), any(), any(), any())).thenReturn(Mono.just(renderedTemplate));
        when(senderRegistry.get(NotificationChannel.EMAIL)).thenReturn(emailSender);
        when(emailSender.send(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(notificationService.send(request))
                .assertNext(dto -> {
                    assertThat(dto.getSendStatus()).isEqualTo("SENT");
                    assertThat(dto.getSubject()).isEqualTo("订单 ORD001 已创建");
                })
                .verifyComplete();

        verify(emailSender).send("user@example.com", "订单 ORD001 已创建", "<p>您好，张三</p>");
        verify(recordRepository, times(2)).save(any());
    }

    @Test
    void send_channelFails_persistsRecordAsFailed() {
        NotificationRecord pending = savedRecord("PENDING");
        NotificationRecord failed = savedRecord("FAILED");
        failed.setRetryCount(1);
        failed.setErrorMessage("SMTP connection refused");

        when(recordRepository.save(any())).thenReturn(Mono.just(pending), Mono.just(failed));
        when(templateService.resolve(any(), any(), any(), any())).thenReturn(Mono.just(renderedTemplate));
        when(senderRegistry.get(NotificationChannel.EMAIL)).thenReturn(emailSender);
        when(emailSender.send(any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("SMTP connection refused")));

        StepVerifier.create(notificationService.send(request))
                .assertNext(dto -> {
                    assertThat(dto.getSendStatus()).isEqualTo("FAILED");
                    assertThat(dto.getRetryCount()).isEqualTo(1);
                })
                .verifyComplete();  // error is absorbed — pipeline does not propagate it
    }

    @Test
    void send_defaultLanguage_usedWhenNull() {
        request.setLanguage(null);

        NotificationRecord pending = savedRecord("PENDING");
        NotificationRecord sent = savedRecord("SENT");

        when(recordRepository.save(any())).thenReturn(Mono.just(pending), Mono.just(sent));
        when(senderRegistry.get(any())).thenReturn(emailSender);
        when(emailSender.send(any(), any(), any())).thenReturn(Mono.empty());

        // Capture the language argument passed to templateService
        ArgumentCaptor<String> langCaptor = ArgumentCaptor.forClass(String.class);
        when(templateService.resolve(any(), any(), langCaptor.capture(), any()))
                .thenReturn(Mono.just(renderedTemplate));

        StepVerifier.create(notificationService.send(request))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(langCaptor.getValue()).isEqualTo("zh-CN");
    }

    @Test
    void send_templateResolveFails_propagatesError() {
        NotificationRecord pending = savedRecord("PENDING");
        when(recordRepository.save(any())).thenReturn(Mono.just(pending));
        when(templateService.resolve(any(), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("DB down")));

        StepVerifier.create(notificationService.send(request))
                .expectError(RuntimeException.class)
                .verify();
    }
}
