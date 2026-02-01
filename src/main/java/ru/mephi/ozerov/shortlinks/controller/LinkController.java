package ru.mephi.ozerov.shortlinks.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mephi.ozerov.shortlinks.dto.*;
import ru.mephi.ozerov.shortlinks.entity.Link;
import ru.mephi.ozerov.shortlinks.service.LinkService;

@RestController
@RequestMapping("/api/links")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    @Value("${shortlinks.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * Создание короткой ссылки. Если заголовок X-User-Id отсутствует — генерируется новый UUID и
     * возвращается в ответе (и в заголовке X-User-Id).
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId,
            @Valid @RequestBody CreateLinkRequest request,
            HttpServletResponse response) {
        UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();
        Link link =
                linkService.create(
                        request.getOriginalUrl(), request.getClickLimit(), effectiveUserId);
        response.setHeader(USER_ID_HEADER, effectiveUserId.toString());
        CreateLinkResponse body =
                new CreateLinkResponse(LinkResponse.from(link, baseUrl), effectiveUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Список ссылок текущего пользователя. X-User-Id обязателен. */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Заголовок X-User-Id обязателен для просмотра списка ссылок");
        }
        List<LinkResponse> list =
                linkService.findByUserId(userId).stream()
                        .map(l -> LinkResponse.from(l, baseUrl))
                        .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** Получить одну ссылку по id. Только создатель. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Заголовок X-User-Id обязателен");
        }
        return linkService
                .findByIdAndUserId(id, userId)
                .map(l -> ResponseEntity.ok(LinkResponse.from(l, baseUrl)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Обновить ссылку (URL и/или лимит). Только создатель. */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId,
            @Valid @RequestBody UpdateLinkRequest request) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Заголовок X-User-Id обязателен");
        }
        Optional<Link> updated =
                linkService.update(id, userId, request.getOriginalUrl(), request.getClickLimit());
        return updated.map(l -> ResponseEntity.ok(LinkResponse.from(l, baseUrl)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Удалить ссылку. Только создатель. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @RequestHeader(value = USER_ID_HEADER, required = false) UUID userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Заголовок X-User-Id обязателен");
        }
        return linkService.delete(id, userId)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
