package com.iemodo.notification.channel;

import com.iemodo.notification.domain.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Email delivery via Spring Mail (JavaMail).
 *
 * <p>JavaMail is blocking — we push it to {@code Schedulers.boundedElastic()}
 * so it never blocks the Reactor event-loop thread.
 *
 * <p>Local dev: point MAIL_HOST/PORT at a Mailhog container.
 * Production: swap SMTP credentials to SendGrid/AWS SES — zero code change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailChannelSender implements ChannelSender {

    private final JavaMailSender mailSender;

    @Value("${iemodo.notification.from-address:noreply@iemodo.com}")
    private String fromAddress;

    @Value("${iemodo.notification.from-name:iemodo}")
    private String fromName;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public Mono<Void> send(String recipient, String subject, String body) {
        return Mono.<Void>fromRunnable(() -> {
            try {
                var message = mailSender.createMimeMessage();
                var helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress, fromName);
                helper.setTo(recipient);
                helper.setSubject(subject != null ? subject : "(no subject)");
                // Treat body as HTML if it starts with '<', otherwise plain text
                boolean isHtml = body != null && body.stripLeading().startsWith("<");
                helper.setText(body != null ? body : "", isHtml);
                mailSender.send(message);
                log.info("Email sent to={} subject={}", recipient, subject);
            } catch (Exception e) {
                throw new RuntimeException("Email send failed to " + recipient, e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
