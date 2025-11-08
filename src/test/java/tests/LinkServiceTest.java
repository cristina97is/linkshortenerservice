package tests;

import domain.ShortLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import service.LinkService;
import storage.InMemoryStorage;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class LinkServiceTest {

    private InMemoryStorage storage;
    private LinkService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        storage = new InMemoryStorage();
        service = new LinkService(storage);
        userId = UUID.randomUUID();
    }

    @Test
    void testCreateLink() {
        ShortLink link = service.create("https://example.com", 2, 0.01, userId);
        assertNotNull(link);
        assertEquals("https://example.com", link.originalUrl);
        assertEquals(0, link.currentClicks);
        assertEquals(2, link.maxClicks);
        assertEquals(userId, link.userId);
    }

    @Test
    void testOpenLinkIncrementsClicks() {
        ShortLink link = service.create("https://example.com", 2, 0.01, userId);
        service.open(link.shortUrl, userId);
        assertEquals(1, link.currentClicks);
        service.open(link.shortUrl, userId);
        assertEquals(2, link.currentClicks);
        service.open(link.shortUrl, userId);
        assertEquals(2, link.currentClicks); // лимит не превышен
    }

    @Test
    void testListForUser() {
        ShortLink link1 = service.create("https://example.com/1", 2, 0.01, userId);
        ShortLink link2 = service.create("https://example.com/2", 3, 0.01, userId);
        List<ShortLink> links = service.listFor(userId);
        assertEquals(2, links.size());
    }

    @Test
    void testEditLink() throws Exception {
        ShortLink link = service.create("https://example.com", 2, 0.01, userId);
        service.edit(link.shortUrl, userId, 5, 0.02);
        assertEquals(5, link.maxClicks);
    }

    @Test
    void testCleanupExpired() throws InterruptedException {
        ShortLink link = service.create("https://example.com", 2, 0.0001, userId); // очень короткий TTL
        Thread.sleep(200);
        assertTrue(service.isExpired(link));
        service.cleanupExpired();
        assertNull(service.get(link.shortUrl));
    }
}