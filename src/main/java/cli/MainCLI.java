package cli; // Пакет для консольного интерфейса CLI

import domain.ShortLink;          // Доменный класс для хранения информации о короткой ссылке
import service.LinkService;       // Сервис для работы с ссылками (создание, открытие, редактирование)
import storage.InMemoryStorage;   // Хранилище ссылок в памяти
import util.Util;                 // Утилиты (например, проверка корректности URL)

import java.time.format.DateTimeFormatter; // Форматирование даты/времени
import java.util.List;                      // Для работы со списками
import java.util.Scanner;                   // Для считывания пользовательского ввода
import java.util.UUID;                      // Для идентификации пользователя

/**
 * Консольный интерфейс CLI для сервиса сокращения ссылок
 */
public class MainCLI {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);        // Создаем сканер для чтения ввода пользователя
        InMemoryStorage storage = new InMemoryStorage(); // Создаем хранилище ссылок в памяти
        LinkService service = new LinkService(storage);  // Создаем сервис для работы со ссылками, используя хранилище

        UUID currentUserId = null; // UUID текущего пользователя (будет создан при первой ссылке)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // Формат для вывода даты/времени
        boolean running = true; // Флаг, показывающий, что программа работает

        printHelp(); // Печатаем справку при старте программы

        while (running) { // Главный цикл CLI
            System.out.print("\nВыберите действие (1-6, 0 - выход): ");
            String option = scanner.nextLine().trim(); // Считываем команду пользователя и убираем лишние пробелы

            switch (option) {
                case "1": // Создание ссылки
                    System.out.print("Введите длинный URL: ");
                    String url = scanner.nextLine().trim(); // Считываем длинный URL
                    if (!Util.isValidUrl(url)) { // Проверяем корректность URL
                        System.out.println("Неверный URL.");
                        break; // Выходим из кейса, если URL некорректен
                    }

                    System.out.print("Введите лимит переходов (целое число): ");
                    int maxClicks;
                    try {
                        maxClicks = Integer.parseInt(scanner.nextLine()); // Преобразуем ввод в число
                        if (maxClicks <=0) throw new NumberFormatException(); // Проверяем на положительное число
                    } catch (NumberFormatException e){
                        System.out.println("Неверный ввод числа.");
                        break;
                    }

                    System.out.print("Введите TTL в часах (например, 0.0083=30 секунд, 1=1 час): ");
                    double ttlHours;
                    try {
                        ttlHours = Double.parseDouble(scanner.nextLine()); // Преобразуем TTL в double
                        if (ttlHours<=0) throw new NumberFormatException(); // Проверяем на положительное число
                    } catch (NumberFormatException e){
                        System.out.println("Неверный ввод TTL.");
                        break;
                    }

                    ShortLink link = service.create(url, maxClicks, ttlHours, currentUserId); // Создаем короткую ссылку
                    currentUserId = link.userId; // Сохраняем UUID пользователя

                    // Вывод информации о созданной ссылке
                    System.out.println("\nСсылка создана!");
                    System.out.println("Короткая ссылка: " + link.shortUrl);
                    System.out.println("Ваш UUID: " + link.userId);
                    break;

                case "2": // Переход по ссылке
                    if (currentUserId==null){ // Если пользователь еще не создавал ссылку
                        System.out.println("Сначала создайте ссылку.");
                        break;
                    }
                    System.out.print("Введите короткую ссылку: ");
                    String shortUrl = scanner.nextLine().trim(); // Считываем короткую ссылку

                    ShortLink linkToOpen = service.get(shortUrl); // Получаем объект ссылки по короткому URL
                    if (linkToOpen==null){ // Если ссылка не найдена
                        System.out.println("Ссылка не найдена.");
                        break;
                    }
                    if (service.isExpired(linkToOpen)){ // Проверяем, не истек ли TTL
                        System.out.println("Срок ссылки истёк.");
                        break;
                    }
                    if (linkToOpen.currentClicks>=linkToOpen.maxClicks){ // Проверяем лимит переходов
                        System.out.println("Лимит переходов исчерпан.");
                        break;
                    }

                    service.open(shortUrl, currentUserId); // Увеличиваем счетчик кликов и открываем ссылку
                    String formattedExpiry = linkToOpen.expiryTime.format(formatter); // Форматируем дату истечения
                    // Выводим информацию о переходе
                    System.out.println("Переход на: " + linkToOpen.originalUrl +
                            " | Переходов: " + linkToOpen.currentClicks + "/" + linkToOpen.maxClicks +
                            " | Истекает: " + formattedExpiry);
                    break;

                case "3": // Показ списка своих ссылок
                    if (currentUserId==null){
                        System.out.println("Сначала создайте ссылку.");
                        break;
                    }
                    List<ShortLink> userLinks = service.listFor(currentUserId); // Получаем все ссылки пользователя
                    if (userLinks.isEmpty()){
                        System.out.println("У вас нет ссылок.");
                        break;
                    }
                    System.out.println("Ваши ссылки:");
                    for (ShortLink l:userLinks){ // Вывод информации о каждой ссылке
                        System.out.println(l.shortUrl + " -> " + l.originalUrl +
                                " | Переходов: " + l.currentClicks + "/" + l.maxClicks +
                                " | Истекает: " + l.expiryTime.format(formatter));
                    }
                    break;

                case "4": // Очистка просроченных ссылок
                    service.cleanupExpired(); // Удаляем все ссылки, срок действия которых истек
                    System.out.println("Просроченные ссылки удалены.");
                    break;

                case "5": // Редактирование лимита и TTL
                    if (currentUserId==null){
                        System.out.println("Сначала создайте ссылку.");
                        break;
                    }
                    System.out.print("Введите короткую ссылку для редактирования: ");
                    String editUrl = scanner.nextLine().trim(); // Считываем короткую ссылку для редактирования

                    System.out.print("Введите новый лимит переходов: ");
                    int newMaxClicks;
                    try {
                        newMaxClicks = Integer.parseInt(scanner.nextLine());
                        if (newMaxClicks<=0) throw new NumberFormatException();
                    }
                    catch (NumberFormatException e){
                        System.out.println("Неверный ввод числа.");
                        break;
                    }

                    System.out.print("Введите новый TTL в часах: ");
                    double newTTL;
                    try {
                        newTTL = Double.parseDouble(scanner.nextLine());
                        if (newTTL<=0) throw new NumberFormatException();
                    }
                    catch (NumberFormatException e){
                        System.out.println("Неверный ввод TTL.");
                        break;
                    }

                    try {
                        service.edit(editUrl, currentUserId, newMaxClicks, newTTL); // Редактируем ссылку
                        System.out.println("Ссылка обновлена.");
                    }
                    catch (Exception e){
                        System.out.println("Ошибка: "+e.getMessage());
                    }
                    break;

                case "6": printHelp(); break; // Печать справки
                case "0": running=false; System.out.println("Выход."); break; // Выход из программы
                default: System.out.println("Неизвестная команда. Введите 6 для помощи."); break; // Неизвестная команда
            }
        }
        scanner.close(); // Закрываем сканер
    }

    // Метод для печати справки / Help
    private static void printHelp() {
        System.out.println("=== CLI Link Shortener ===");
        System.out.println("1 - Создать ссылку");
        System.out.println("2 - Перейти по ссылке (клики и TTL)");
        System.out.println("3 - Показать свои ссылки");
        System.out.println("4 - Очистка просроченных ссылок");
        System.out.println("5 - Редактировать ссылку (лимит и TTL)");
        System.out.println("6 - Help");
        System.out.println("0 - Выход");
    }
}