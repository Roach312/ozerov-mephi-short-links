package ru.mephi.ozerov.shortlinks.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.ozerov.shortlinks.entity.Notification;
import ru.mephi.ozerov.shortlinks.entity.NotificationType;
import ru.mephi.ozerov.shortlinks.repository.NotificationRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(UUID userId, Long linkId, String shortCode, NotificationType type, String message) {
        Notification n = Notification.builder()
                .userId(userId)
                .linkId(linkId)
                .shortCode(shortCode)
                .type(type)
                .message(message)
                .createdAt(Instant.now())
                .readFlag(false)
                .build();
        return notificationRepository.save(n);
    }

    public List<Notification> findByUserId(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, UUID userId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .ifPresent(n -> {
                    n.setReadFlag(true);
                    notificationRepository.save(n);
                });
    }
}
