package com.iemodo.notification.config;

import com.iemodo.notification.channel.ChannelSender;
import com.iemodo.notification.domain.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates all {@link ChannelSender} implementations into a lookup map.
 * New channels are registered automatically when their Spring bean is added.
 */
@Component
public class ChannelSenderRegistry {

    private final Map<NotificationChannel, ChannelSender> senders;

    public ChannelSenderRegistry(List<ChannelSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(ChannelSender::channel, Function.identity()));
    }

    public ChannelSender get(NotificationChannel channel) {
        ChannelSender sender = senders.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("No ChannelSender registered for channel: " + channel);
        }
        return sender;
    }
}
