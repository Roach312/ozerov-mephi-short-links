package ru.mephi.ozerov.shortlinks.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.mephi.ozerov.shortlinks.service.LinkService;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredLinksScheduler {

    private final LinkService linkService;

    /**
     * Каждые 10 минут проверяем истёкшие ссылки, уведомляем пользователей и удаляем ссылки (по ТЗ).
     */
    @Scheduled(fixedRate = 600_000)
    public void deleteExpiredLinks() {
        int count = linkService.deleteExpiredAndNotify();
        if (count > 0) {
            log.info("Удалено истёкших ссылок: {}", count);
        }
    }
}
