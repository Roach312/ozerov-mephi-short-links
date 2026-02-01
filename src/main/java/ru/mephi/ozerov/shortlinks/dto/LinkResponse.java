package ru.mephi.ozerov.shortlinks.dto;

import lombok.Builder;
import lombok.Data;
import ru.mephi.ozerov.shortlinks.entity.Link;

import java.time.Instant;

@Data
@Builder
public class LinkResponse {

    private Long id;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Integer clickLimit;
    private Integer clicksCount;
    private Instant expiresAt;
    private Instant createdAt;
    private boolean available;

    public static LinkResponse from(Link link, String baseUrl) {
        String shortUrl = baseUrl.endsWith("/") ? baseUrl + link.getShortCode() : baseUrl + "/" + link.getShortCode();
        return LinkResponse.builder()
                .id(link.getId())
                .shortCode(link.getShortCode())
                .shortUrl(shortUrl)
                .originalUrl(link.getOriginalUrl())
                .clickLimit(link.getClickLimit())
                .clicksCount(link.getClicksCount())
                .expiresAt(link.getExpiresAt())
                .createdAt(link.getCreatedAt())
                .available(link.isAvailable())
                .build();
    }
}
