package ru.mephi.ozerov.shortlinks.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mephi.ozerov.shortlinks.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndReadFlagOrderByCreatedAtDesc(UUID userId, boolean readFlag);
}
