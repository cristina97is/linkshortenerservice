package util;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Утилиты для проверки URL
 */
public class Util {
    public static boolean isValidUrl(String url) {
        try {
            URL u = new URL(url);
            return u.getProtocol().equals("http") || u.getProtocol().equals("https");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String generateShort() {
        // Простая генерация короткой ссылки
        return "http://clck.ru/" + Long.toHexString(Double.doubleToLongBits(Math.random()));
    }
}