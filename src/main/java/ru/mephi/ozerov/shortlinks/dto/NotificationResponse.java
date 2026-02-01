package ru.mephi.ozerov.shortlinks.dto;

import lombok.Builder;
import lombok.Data;
import ru.mephi.ozerov.shortlinks.entity.Notification;
import ru.mephi.ozerov.shortlinks.entity.NotificationType;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long linkId;
    private String shortCode;
    private NotificationType type;
    private String message;
    private Instant createdAt;
    private boolean read;

    public static NotificationResponse from(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .linkId(n.getLinkId())
                .shortCode(n.getShortCode())
                .type(n.getType())
                .message(n.getMessage())
                .createdAt(n.getCreatedAt())
                .read(n.getReadFlag())
                .build();
    }
}
