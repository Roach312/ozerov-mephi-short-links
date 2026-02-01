package ru.mephi.ozerov.shortlinks.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.mephi.ozerov.shortlinks.entity.Link;
import ru.mephi.ozerov.shortlinks.service.LinkService;

/**
 * Обрабатывает переход по короткой ссылке: редирект на исходный URL. Доступ только у владельца
 * ссылки — обязателен заголовок X-User-Id, совпадающий с создателем ссылки.
 */
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class RedirectController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final LinkService linkService;

    @GetMapping("/{shortCode}")
    public void redirect(
            @PathVariable String shortCode,
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId,
            HttpServletResponse response)
            throws IOException {
        if (userId == null) {
            response.sendError(
                    HttpStatus.BAD_REQUEST.value(),
                    "Заголовок X-User-Id обязателен для перехода по ссылке");
            return;
        }

        Optional<Link> opt = linkService.findByShortCode(shortCode);
        if (opt.isEmpty()) {
            response.sendError(HttpStatus.NOT_FOUND.value(), "Ссылка не найдена");
            return;
        }

        Link link = opt.get();
        if (!link.getUserId().equals(userId)) {
            response.sendError(
                    HttpStatus.FORBIDDEN.value(),
                    "Доступ запрещён: ссылка принадлежит другому пользователю");
            return;
        }

        if (!link.isAvailable()) {
            if (link.isExpired()) {
                response.sendError(HttpStatus.GONE.value(), "Время жизни ссылки истекло");
            } else if (link.isLimitReached()) {
                response.sendError(HttpStatus.GONE.value(), "Лимит переходов исчерпан");
            } else {
                response.sendError(HttpStatus.GONE.value(), "Ссылка недоступна");
            }
            return;
        }

        Optional<Link> afterIncrement = linkService.resolveAndIncrementClicks(shortCode);
        if (afterIncrement.isEmpty()) {
            response.sendError(HttpStatus.GONE.value(), "Ссылка недоступна");
            return;
        }

        String targetUrl = afterIncrement.get().getOriginalUrl();
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "https://" + targetUrl;
        }
        response.sendRedirect(targetUrl);
    }
}
