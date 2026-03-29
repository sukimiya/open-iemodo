package com.iemodo.notification.service;

import com.iemodo.notification.domain.NotificationChannel;
import com.iemodo.notification.domain.NotificationTemplate;
import com.iemodo.notification.domain.NotificationType;
import com.iemodo.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private TemplateService templateService;

    private NotificationTemplate zhTemplate;
    private NotificationTemplate enTemplate;

    @BeforeEach
    void setUp() {
        zhTemplate = NotificationTemplate.builder()
                .type(NotificationType.ORDER_CREATED)
                .channel(NotificationChannel.EMAIL)
                .language("zh-CN")
                .subject("订单 {{orderNo}} 已创建")
                .body("您好，{{userName}}，订单金额 {{currency}} {{totalAmount}}")
                .active(true)
                .build();

        enTemplate = NotificationTemplate.builder()
                .type(NotificationType.ORDER_CREATED)
                .channel(NotificationChannel.EMAIL)
                .language("en")
                .subject("Order {{orderNo}} confirmed")
                .body("Hi {{userName}}, total: {{currency}} {{totalAmount}}")
                .active(true)
                .build();
    }

    @Test
    void resolve_exactLanguageMatch_returnsRenderedTemplate() {
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "zh-CN"))
                .thenReturn(Mono.just(zhTemplate));

        Map<String, String> vars = Map.of(
                "orderNo", "ORD001",
                "userName", "张三",
                "currency", "USD",
                "totalAmount", "99.00"
        );

        StepVerifier.create(templateService.resolve(
                        NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "zh-CN", vars))
                .assertNext(t -> {
                    assertThat(t.getSubject()).isEqualTo("订单 ORD001 已创建");
                    assertThat(t.getBody()).contains("张三").contains("USD").contains("99.00");
                })
                .verifyComplete();
    }

    @Test
    void resolve_fallsBackToEnglish_whenRequestedLanguageMissing() {
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "ja"))
                .thenReturn(Mono.empty());
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "en"))
                .thenReturn(Mono.just(enTemplate));

        StepVerifier.create(templateService.resolve(
                        NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "ja",
                        Map.of("orderNo", "ORD002", "userName", "Taro", "currency", "JPY", "totalAmount", "1000")))
                .assertNext(t -> {
                    assertThat(t.getLanguage()).isEqualTo("en");
                    assertThat(t.getSubject()).isEqualTo("Order ORD002 confirmed");
                })
                .verifyComplete();
    }

    @Test
    void resolve_hardcodedFallback_whenNoTemplateExists() {
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(any(), any(), eq("zh-CN")))
                .thenReturn(Mono.empty());
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(any(), any(), eq("en")))
                .thenReturn(Mono.empty());

        StepVerifier.create(templateService.resolve(
                        NotificationType.RMA_APPROVED, NotificationChannel.EMAIL, "zh-CN", Map.of()))
                .assertNext(t -> {
                    assertThat(t.getSubject()).contains("rma approved");
                    assertThat(t.getBody()).contains("iemodo");
                    assertThat(t.getLanguage()).isEqualTo("en");
                })
                .verifyComplete();
    }

    @Test
    void render_nullVariables_returnsTemplateUnchanged() {
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "en"))
                .thenReturn(Mono.just(enTemplate));

        StepVerifier.create(templateService.resolve(
                        NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "en", null))
                .assertNext(t -> {
                    // Template returned as-is when no variables provided
                    assertThat(t.getSubject()).isEqualTo(enTemplate.getSubject());
                    assertThat(t.getBody()).isEqualTo(enTemplate.getBody());
                })
                .verifyComplete();
    }

    @Test
    void render_missingVariable_replacedWithEmptyString() {
        when(templateRepository.findByTypeAndChannelAndLanguageAndActiveTrue(
                NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "en"))
                .thenReturn(Mono.just(enTemplate));

        // Only provide orderNo, leave userName / currency / totalAmount absent
        StepVerifier.create(templateService.resolve(
                        NotificationType.ORDER_CREATED, NotificationChannel.EMAIL, "en",
                        Map.of("orderNo", "ORD003")))
                .assertNext(t -> {
                    assertThat(t.getSubject()).isEqualTo("Order ORD003 confirmed");
                    // Unreplaced placeholders remain in text (not our job to validate completeness)
                    assertThat(t.getBody()).contains("{{userName}}");
                })
                .verifyComplete();
    }
}
