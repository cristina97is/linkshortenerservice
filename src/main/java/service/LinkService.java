package service;

import domain.ShortLink;           // Доменный класс для короткой ссылки
import storage.InMemoryStorage;    // Хранилище ссылок в памяти
import util.Util;                  // Утилиты (например, генерация короткой ссылки)

import java.time.LocalDateTime;    // Для работы с датой и временем
import java.util.List;             // Для работы со списками
import java.util.UUID;             // Для идентификации пользователя
import java.util.stream.Collectors; // Для работы со Stream API

/**
 * Основная логика работы с короткими ссылками
 */
public class LinkService {

    private final InMemoryStorage storage; // Хранилище ссылок

    // Конструктор сервиса, принимает конкретное хранилище
    public LinkService(InMemoryStorage storage) {
        this.storage = storage;
    }

    /**
     * Создание новой короткой ссылки
     * @param originalUrl - исходный URL
     * @param maxClicks - максимальное количество переходов
     * @param ttlHours - время жизни ссылки в часах
     * @param userId - UUID пользователя (если null, создается новый)
     * @return созданный объект ShortLink
     */
    public ShortLink create(String originalUrl, int maxClicks, double ttlHours, UUID userId) {
        if (userId == null) userId = UUID.randomUUID(); // Генерируем UUID, если его нет
        LocalDateTime expiry = LocalDateTime.now().plusSeconds((long)(ttlHours * 3600)); // Вычисляем дату истечения
        String shortUrl = Util.generateShort(); // Генерация случайного короткого URL
        ShortLink link = new ShortLink(originalUrl, shortUrl, maxClicks, expiry, userId); // Создаем объект ссылки
        storage.save(link); // Сохраняем в хранилище
        return link; // Возвращаем объект
    }

    /**
     * Открытие ссылки (увеличивает счетчик кликов)
     * @param shortUrl - короткая ссылка
     * @param userId - UUID пользователя
     */
    public void open(String shortUrl, UUID userId) {
        ShortLink link = storage.get(shortUrl); // Получаем ссылку из хранилища
        if (link == null) { // Проверка на существование
            System.out.println("Ссылка не найдена.");
            return;
        }
        if (isExpired(link)) { // Проверка TTL
            System.out.println("Срок действия ссылки истёк.");
            return;
        }
        if (link.currentClicks >= link.maxClicks) { // Проверка лимита переходов
            System.out.println("Лимит переходов исчерпан.");
            return;
        }
        link.currentClicks++; // Увеличиваем счетчик кликов
        System.out.println("Открываем: " + link.originalUrl);
        // Desktop.getDesktop().browse(new URI(link.originalUrl)); // Можно раскомментировать для открытия в браузере
    }

    /**
     * Получение всех ссылок пользователя
     * @param userId - UUID пользователя
     * @return список ссылок пользователя
     */
    public List<ShortLink> listFor(UUID userId) {
        return storage.getAll().stream()
                .filter(link -> link.userId.equals(userId)) // Фильтруем по пользователю
                .collect(Collectors.toList()); // Собираем в список
    }

    /**
     * Редактирование параметров ссылки (лимит переходов и TTL)
     * @param shortUrl - короткая ссылка
     * @param userId - UUID пользователя
     * @param newMaxClicks - новый лимит переходов
     * @param ttlHours - новое время жизни в часах
     * @throws Exception если ссылка не найдена или пользователь не владелец
     */
    public void edit(String shortUrl, UUID userId, int newMaxClicks, double ttlHours) throws Exception {
        ShortLink link = storage.get(shortUrl); // Получаем ссылку
        if (link == null) throw new Exception("Ссылка не найдена");
        if (!link.userId.equals(userId)) throw new Exception("Нет прав редактирования"); // Проверка прав
        link.maxClicks = newMaxClicks; // Изменяем лимит кликов
        link.expiryTime = LocalDateTime.now().plusSeconds((long)(ttlHours * 3600)); // Пересчитываем TTL
    }

    /**
     * Проверка, истекла ли ссылка
     * @param link - объект ShortLink
     * @return true, если TTL истек, иначе false
     */
    public boolean isExpired(ShortLink link) {
        return LocalDateTime.now().isAfter(link.expiryTime);
    }

    /**
     * Очистка всех просроченных ссылок
     */
    public void cleanupExpired() {
        // Собираем список коротких URL просроченных ссылок
        List<String> expiredKeys = storage.getAll().stream()
                .filter(this::isExpired) // Проверяем TTL
                .map(link -> link.shortUrl) // Получаем ключи
                .toList(); // Преобразуем в список (Java 17)
        // Удаляем каждую просроченную ссылку
        for (String key : expiredKeys) {
            storage.delete(key);
        }
    }

    /**
     * Получение ссылки по короткому URL
     * @param shortUrl - короткая ссылка
     * @return объект ShortLink или null, если не найден
     */
    public ShortLink get(String shortUrl) {
        return storage.get(shortUrl);
    }
}