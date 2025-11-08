package domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Класс для хранения информации о короткой ссылке
 */
public class ShortLink {
    public String originalUrl;   // Исходный URL
    public String shortUrl;      // Короткая ссылка
    public int maxClicks;        // Максимальное число переходов
    public int currentClicks;    // Счётчик переходов
    public LocalDateTime expiryTime; // Время истечения TTL
    public UUID userId;          // UUID создателя ссылки

    public ShortLink(String originalUrl, String shortUrl, int maxClicks, LocalDateTime expiryTime, UUID userId) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.maxClicks = maxClicks;
        this.currentClicks = 0;
        this.expiryTime = expiryTime;
        this.userId = userId;
    }
}