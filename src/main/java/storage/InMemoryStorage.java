package storage;

import domain.ShortLink;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Хранилище ссылок в памяти
 */
public class InMemoryStorage {
    private final Map<String, ShortLink> storage = new HashMap<>();

    public void save(ShortLink link) {
        storage.put(link.shortUrl, link);
    }

    public ShortLink get(String shortUrl) {
        return storage.get(shortUrl);
    }

    public void delete(String shortUrl) {
        storage.remove(shortUrl);
    }

    public Collection<ShortLink> getAll() {
        return storage.values();
    }
}