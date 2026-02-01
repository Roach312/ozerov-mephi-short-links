package ru.mephi.ozerov.shortlinks.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.mephi.ozerov.shortlinks.entity.Link;
import ru.mephi.ozerov.shortlinks.entity.NotificationType;
import ru.mephi.ozerov.shortlinks.repository.LinkRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ShortCodeGenerator shortCodeGenerator;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LinkService linkService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ORIGINAL_URL = "https://www.baeldung.com/java-9-http-client";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "ttlHours", 24);
    }

    @Test
    void create_generatesUniqueShortCodeAndSavesLink() {
        when(shortCodeGenerator.generate()).thenReturn("3DZHeG");
        when(linkRepository.existsByShortCode("3DZHeG")).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> {
            Link l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        Link result = linkService.create(ORIGINAL_URL, 10, USER_ID);

        assertNotNull(result);
        assertEquals("3DZHeG", result.getShortCode());
        assertEquals(ORIGINAL_URL, result.getOriginalUrl());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(10, result.getClickLimit());
        assertEquals(0, result.getClicksCount());
        assertTrue(result.getExpiresAt().isAfter(Instant.now()));
        assertTrue(result.getActive());
        verify(linkRepository).save(any(Link.class));
    }

    @Test
    void create_retriesWhenShortCodeExists() {
        when(shortCodeGenerator.generate())
                .thenReturn("exists1")
                .thenReturn("unique1");
        when(linkRepository.existsByShortCode("exists1")).thenReturn(true);
        when(linkRepository.existsByShortCode("unique1")).thenReturn(false);
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> {
            Link l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });

        Link result = linkService.create(ORIGINAL_URL, null, USER_ID);

        assertEquals("unique1", result.getShortCode());
        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void findByShortCode_returnsEmptyWhenNotFound() {
        when(linkRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        Optional<Link> result = linkService.findByShortCode("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByShortCode_returnsLinkWhenFound() {
        Link link = createActiveLink("abc123", 5, 0);
        when(linkRepository.findByShortCode("abc123")).thenReturn(Optional.of(link));

        Optional<Link> result = linkService.findByShortCode("abc123");

        assertTrue(result.isPresent());
        assertEquals("abc123", result.get().getShortCode());
    }

    @Test
    void resolveAndIncrementClicks_whenLinkNotFound_returnsEmpty() {
        when(linkRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        Optional<Link> result = linkService.resolveAndIncrementClicks("unknown");

        assertTrue(result.isEmpty());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void resolveAndIncrementClicks_whenLinkExpired_returnsEmpty() {
        Link expired = createActiveLink("exp1", null, 0);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(linkRepository.findByShortCode("exp1")).thenReturn(Optional.of(expired));

        Optional<Link> result = linkService.resolveAndIncrementClicks("exp1");

        assertTrue(result.isEmpty());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void resolveAndIncrementClicks_incrementsClicksAndReturnsLink() {
        Link link = createActiveLink("inc1", null, 2);
        when(linkRepository.findByShortCode("inc1")).thenReturn(Optional.of(link));
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Link> result = linkService.resolveAndIncrementClicks("inc1");

        assertTrue(result.isPresent());
        assertEquals(3, result.get().getClicksCount());
        verify(linkRepository).save(link);
        verify(notificationService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void resolveAndIncrementClicks_whenLimitReachedAfterClick_deactivatesAndNotifies() {
        Link link = createActiveLink("lim1", 2, 1);
        link.setId(10L);
        when(linkRepository.findByShortCode("lim1")).thenReturn(Optional.of(link));
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Link> result = linkService.resolveAndIncrementClicks("lim1");

        assertTrue(result.isPresent());
        assertEquals(2, result.get().getClicksCount());
        assertFalse(result.get().getActive());

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(
                eq(USER_ID),
                eq(10L),
                eq("lim1"),
                eq(NotificationType.CLICK_LIMIT_REACHED),
                msgCaptor.capture()
        );
        assertTrue(msgCaptor.getValue().contains("Лимит переходов"));
    }

    @Test
    void findByUserId_returnsLinksFromRepository() {
        List<Link> links = List.of(
                createActiveLink("a", null, 0),
                createActiveLink("b", null, 0)
        );
        when(linkRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(links);

        List<Link> result = linkService.findByUserId(USER_ID);

        assertEquals(2, result.size());
    }

    @Test
    void update_whenLinkBelongsToUser_updatesAndSaves() {
        Link link = createActiveLink("up1", 5, 0);
        link.setId(1L);
        when(linkRepository.findById(1L)).thenReturn(Optional.of(link));
        when(linkRepository.save(any(Link.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Link> result = linkService.update(1L, USER_ID, "https://new-url.com", 10);

        assertTrue(result.isPresent());
        assertEquals("https://new-url.com", result.get().getOriginalUrl());
        assertEquals(10, result.get().getClickLimit());
        verify(linkRepository).save(link);
    }

    @Test
    void update_whenLinkBelongsToOtherUser_returnsEmpty() {
        Link link = createActiveLink("up1", 5, 0);
        link.setId(1L);
        when(linkRepository.findById(1L)).thenReturn(Optional.of(link));
        UUID otherUser = UUID.randomUUID();

        Optional<Link> result = linkService.update(1L, otherUser, "https://new.com", null);

        assertTrue(result.isEmpty());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void delete_whenLinkBelongsToUser_deletesAndReturnsTrue() {
        Link link = createActiveLink("del1", null, 0);
        link.setId(1L);
        when(linkRepository.findById(1L)).thenReturn(Optional.of(link));

        boolean result = linkService.delete(1L, USER_ID);

        assertTrue(result);
        verify(linkRepository).delete(link);
    }

    @Test
    void delete_whenLinkBelongsToOtherUser_returnsFalse() {
        Link link = createActiveLink("del1", null, 0);
        link.setId(1L);
        when(linkRepository.findById(1L)).thenReturn(Optional.of(link));

        boolean result = linkService.delete(1L, UUID.randomUUID());

        assertFalse(result);
        verify(linkRepository, never()).delete(any());
    }

    @Test
    void deleteExpiredAndNotify_findsExpiredNotifiesAndDeletes() {
        Link expired = createActiveLink("ex1", null, 0);
        expired.setId(5L);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(linkRepository.findExpiredActiveLinks(any(Instant.class))).thenReturn(List.of(expired));

        int count = linkService.deleteExpiredAndNotify();

        assertEquals(1, count);
        verify(notificationService).create(
                eq(USER_ID),
                eq(5L),
                eq("ex1"),
                eq(NotificationType.LINK_EXPIRED),
                anyString()
        );
        verify(linkRepository).delete(expired);
    }

    private static Link createActiveLink(String shortCode, Integer clickLimit, int clicksCount) {
        return Link.builder()
                .shortCode(shortCode)
                .originalUrl("https://example.com")
                .userId(USER_ID)
                .clickLimit(clickLimit)
                .clicksCount(clicksCount)
                .expiresAt(Instant.now().plusSeconds(3600))
                .createdAt(Instant.now())
                .active(true)
                .build();
    }
}
