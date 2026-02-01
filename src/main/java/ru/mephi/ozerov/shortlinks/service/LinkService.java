package ru.mephi.ozerov.shortlinks.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mephi.ozerov.shortlinks.entity.Link;
import ru.mephi.ozerov.shortlinks.entity.NotificationType;
import ru.mephi.ozerov.shortlinks.repository.LinkRepository;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final NotificationService notificationService;

    @Value("${shortlinks.ttl-hours:24}")
    private int ttlHours;

    /**
     * Создаёт короткую ссылку. Уникальный shortCode для каждого вызова (разные пользователи —
     * разные ссылки).
     */
    @Transactional
    public Link create(String originalUrl, Integer clickLimit, UUID userId) {
        String shortCode;
        do {
            shortCode = shortCodeGenerator.generate();
        } while (linkRepository.existsByShortCode(shortCode));

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofHours(ttlHours));

        Link link =
                Link.builder()
                        .shortCode(shortCode)
                        .originalUrl(originalUrl)
                        .userId(userId)
                        .clickLimit(clickLimit)
                        .clicksCount(0)
                        .expiresAt(expiresAt)
                        .createdAt(now)
                        .active(true)
                        .build();
        return linkRepository.save(link);
    }

    /**
     * Находит ссылку по shortCode и увеличивает счётчик переходов. Если лимит исчерпан после
     * перехода — создаёт уведомление и деактивирует ссылку.
     */
    @Transactional
    public Optional<Link> resolveAndIncrementClicks(String shortCode) {
        Optional<Link> opt = linkRepository.findByShortCode(shortCode);
        if (opt.isEmpty()) return Optional.empty();

        Link link = opt.get();
        if (!link.isAvailable()) return Optional.empty();

        link.setClicksCount(link.getClicksCount() + 1);
        linkRepository.save(link);

        if (link.isLimitReached()) {
            link.setActive(false);
            linkRepository.save(link);
            notificationService.create(
                    link.getUserId(),
                    link.getId(),
                    link.getShortCode(),
                    NotificationType.CLICK_LIMIT_REACHED,
                    "Лимит переходов по ссылке " + link.getShortCode() + " исчерпан.");
        }
        return Optional.of(link);
    }

    /** Только получить ссылку по shortCode (без инкремента). Для проверки доступности. */
    public Optional<Link> findByShortCode(String shortCode) {
        return linkRepository.findByShortCode(shortCode);
    }

    public List<Link> findByUserId(UUID userId) {
        return linkRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Optional<Link> findByIdAndUserId(Long id, UUID userId) {
        return linkRepository.findById(id).filter(l -> l.getUserId().equals(userId));
    }

    @Transactional
    public Optional<Link> update(Long id, UUID userId, String originalUrl, Integer clickLimit) {
        return findByIdAndUserId(id, userId)
                .map(
                        link -> {
                            if (originalUrl != null && !originalUrl.isBlank())
                                link.setOriginalUrl(originalUrl);
                            if (clickLimit != null) link.setClickLimit(clickLimit);
                            return linkRepository.save(link);
                        });
    }

    @Transactional
    public boolean delete(Long id, UUID userId) {
        return findByIdAndUserId(id, userId)
                .map(
                        link -> {
                            linkRepository.delete(link);
                            return true;
                        })
                .orElse(false);
    }

    /**
     * Планируемая задача: уведомить пользователей и автоматически удалить истёкшие ссылки (по ТЗ).
     */
    @Transactional
    public int deleteExpiredAndNotify() {
        Instant now = Instant.now();
        List<Link> expired = linkRepository.findExpiredActiveLinks(now);
        for (Link link : expired) {
            notificationService.create(
                    link.getUserId(),
                    link.getId(),
                    link.getShortCode(),
                    NotificationType.LINK_EXPIRED,
                    "Время жизни ссылки " + link.getShortCode() + " истекло.");
            linkRepository.delete(link);
        }
        return expired.size();
    }
}
