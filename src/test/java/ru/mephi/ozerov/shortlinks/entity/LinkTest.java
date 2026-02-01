package ru.mephi.ozerov.shortlinks.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void isExpired_whenExpiresAtInPast_returnsTrue() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .expiresAt(Instant.now().minusSeconds(60))
                        .createdAt(Instant.now().minusSeconds(3600))
                        .active(true)
                        .build();
        assertTrue(link.isExpired());
    }

    @Test
    void isExpired_whenExpiresAtInFuture_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertFalse(link.isExpired());
    }

    @Test
    void isLimitReached_whenNoLimit_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(null)
                        .clicksCount(100)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertFalse(link.isLimitReached());
    }

    @Test
    void isLimitReached_whenClicksBelowLimit_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(5)
                        .clicksCount(3)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertFalse(link.isLimitReached());
    }

    @Test
    void isLimitReached_whenClicksEqualsLimit_returnsTrue() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(5)
                        .clicksCount(5)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertTrue(link.isLimitReached());
    }

    @Test
    void isAvailable_whenActiveNotExpiredUnderLimit_returnsTrue() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(5)
                        .clicksCount(0)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertTrue(link.isAvailable());
    }

    @Test
    void isAvailable_whenInactive_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(5)
                        .clicksCount(0)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(false)
                        .build();
        assertFalse(link.isAvailable());
    }

    @Test
    void isAvailable_whenExpired_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(5)
                        .clicksCount(0)
                        .expiresAt(Instant.now().minusSeconds(60))
                        .createdAt(Instant.now().minusSeconds(3600))
                        .active(true)
                        .build();
        assertFalse(link.isAvailable());
    }

    @Test
    void isAvailable_whenLimitReached_returnsFalse() {
        Link link =
                Link.builder()
                        .shortCode("abc123")
                        .originalUrl("https://example.com")
                        .userId(USER_ID)
                        .clickLimit(2)
                        .clicksCount(2)
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .createdAt(Instant.now())
                        .active(true)
                        .build();
        assertFalse(link.isAvailable());
    }
}
