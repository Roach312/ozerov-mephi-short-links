package ru.mephi.ozerov.shortlinks.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.ozerov.shortlinks.dto.NotificationResponse;
import ru.mephi.ozerov.shortlinks.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    private static final String USER_ID_HEADER = "X-User-Id";

    /** Список уведомлений пользователя (лимит исчерпан, ссылка истекла). */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("Заголовок X-User-Id обязателен");
        }
        List<NotificationResponse> list =
                notificationService.findByUserId(userId).stream()
                        .map(NotificationResponse::from)
                        .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** Отметить уведомление как прочитанное. */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.badRequest().body("Заголовок X-User-Id обязателен");
        }
        notificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}
