package ru.mephi.ozerov.shortlinks.service;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Генерирует короткий уникальный код для ссылки (Base62-подобный из URL-safe Base64). Разные
 * пользователи получают разные коды даже для одного URL.
 */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    /** Генерирует короткий код заданной длины. Использует случайные байты и алфавит A-Za-z0-9. */
    public String generate(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }
}
