package ru.mephi.ozerov.shortlinks.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.mephi.ozerov.shortlinks.entity.Notification;
import ru.mephi.ozerov.shortlinks.entity.NotificationType;
import ru.mephi.ozerov.shortlinks.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private NotificationService notificationService;

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void create_savesNotificationWithCorrectFields() {
        Notification saved = Notification.builder().id(1L).build();
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        Notification result =
                notificationService.create(
                        USER_ID,
                        10L,
                        "abc123",
                        NotificationType.CLICK_LIMIT_REACHED,
                        "Лимит исчерпан");

        verify(notificationRepository).save(captor.capture());
        Notification captured = captor.getValue();
        assertEquals(USER_ID, captured.getUserId());
        assertEquals(10L, captured.getLinkId());
        assertEquals("abc123", captured.getShortCode());
        assertEquals(NotificationType.CLICK_LIMIT_REACHED, captured.getType());
        assertEquals("Лимит исчерпан", captured.getMessage());
        assertFalse(captured.getReadFlag());
        assertNotNull(captured.getCreatedAt());
        assertEquals(saved, result);
    }

    @Test
    void findByUserId_returnsListFromRepository() {
        List<Notification> list = List.of(Notification.builder().id(1L).userId(USER_ID).build());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(list);

        List<Notification> result = notificationService.findByUserId(USER_ID);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void markAsRead_whenNotificationExistsAndBelongsToUser_setsReadFlag() {
        Notification n = Notification.builder().id(1L).userId(USER_ID).readFlag(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAsRead(1L, USER_ID);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getReadFlag());
    }

    @Test
    void markAsRead_whenNotificationBelongsToOtherUser_doesNothing() {
        Notification n = Notification.builder().id(1L).userId(USER_ID).readFlag(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(1L, UUID.randomUUID());

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_whenNotificationNotFound_doesNothing() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        notificationService.markAsRead(999L, USER_ID);

        verify(notificationRepository, never()).save(any());
    }
}
