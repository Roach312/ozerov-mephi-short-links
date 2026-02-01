package ru.mephi.ozerov.shortlinks.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "links", indexes = @Index(columnList = "short_code", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Максимальное количество переходов. null = без лимита.
     */
    @Column(name = "click_limit")
    private Integer clickLimit;

    @Column(name = "clicks_count", nullable = false)
    @Builder.Default
    private Integer clicksCount = 0;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isLimitReached() {
        return clickLimit != null && clicksCount >= clickLimit;
    }

    public boolean isAvailable() {
        return active && !isExpired() && !isLimitReached();
    }
}
