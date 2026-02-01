package ru.mephi.ozerov.shortlinks.console;

import java.awt.Desktop;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Консольное приложение по ТЗ:
 *
 * <ul>
 *   <li><b>Создание коротких ссылок:</b> система принимает длинный URL и преобразует его в короткую
 *       ссылку. Пример: при передаче https://www.baeldung.com/java-9-http-client вы получаете
 *       короткий вариант http://localhost:8080/3DZHeG
 *   <li><b>Переход по короткой ссылке:</b> при вводе короткой ссылки в консоль пользователь
 *       перенаправляется на исходный ресурс в браузере: {@code Desktop.getDesktop().browse(new
 *       URI(...));}
 * </ul>
 *
 * <p>Запуск (сервис должен быть запущен на http://localhost:8080):<br>
 * {@code java -cp target/classes ru.mephi.ozerov.shortlinks.console.ShortLinkBrowserLauncher}
 */
public class ShortLinkBrowserLauncher {

    private static final String BASE_URL_DEFAULT = "http://localhost:8080";
    private static final int TIMEOUT_SECONDS = 15;
    private static final Pattern SHORT_URL_PATTERN =
            Pattern.compile("\"shortUrl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SHORT_CODE_PATTERN =
            Pattern.compile("\"shortCode\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("\"userId\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * User-ID, полученный при создании короткой ссылки; используется при открытии ссылки в браузере
     * (заголовок X-User-Id).
     */
    private static volatile String lastUserId;

    public static void main(String[] args) {
        String baseUrl = System.getProperty("shortlinks.base-url", BASE_URL_DEFAULT);
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                String choice = scanner.nextLine().trim();
                if ("q".equalsIgnoreCase(choice) || "0".equals(choice)) {
                    System.out.println("Выход.");
                    break;
                }
                if ("1".equals(choice)) {
                    createShortLink(scanner, baseUrl);
                } else if ("2".equals(choice)) {
                    openShortLinkInBrowser(scanner, baseUrl);
                } else {
                    System.out.println("Введите 1, 2 или 0 (q) для выхода.");
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("--- Сервис коротких ссылок ---");
        System.out.println("1 — Создать короткую ссылку (введите длинный URL)");
        System.out.println(
                "2 — Открыть короткую ссылку в браузере (введите короткую ссылку или код)");
        System.out.println("0 или q — Выход");
        System.out.print("Выбор: ");
    }

    /** Создание короткой ссылки по ТЗ: принимаем длинный URL, возвращаем короткий вариант. */
    private static void createShortLink(Scanner scanner, String baseUrl) {
        System.out.print(
                "Введите длинный URL (например https://www.baeldung.com/java-9-http-client): ");
        String longUrl = scanner.nextLine().trim();
        if (longUrl.isBlank()) {
            System.out.println("URL не введён.");
            return;
        }

        Optional<String> shortUrl = createShortLinkViaApi(baseUrl, longUrl, null);
        if (shortUrl.isPresent()) {
            System.out.println("Короткая ссылка: " + shortUrl.get());
        } else {
            System.out.println(
                    "Не удалось создать короткую ссылку. Проверьте URL и что сервис запущен.");
        }
    }

    /** POST /api/links — система принимает длинный URL и возвращает короткую ссылку. */
    private static Optional<String> createShortLinkViaApi(
            String baseUrl, String originalUrl, String userId) {
        String jsonBody = "{\"originalUrl\":\"" + escapeJson(originalUrl) + "\"}";
        String uri = baseUrl.replaceAll("/$", "") + "/api/links";
        HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS)).build();
        try {
            HttpRequest.Builder requestBuilder =
                    HttpRequest.newBuilder()
                            .uri(URI.create(uri))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            if (userId != null && !userId.isBlank()) {
                requestBuilder.header("X-User-Id", userId);
            }
            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response =
                    client.send(
                            request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int statusCode = response.statusCode();
            String body = response.body();
            if (statusCode != 201) {
                System.err.println(
                        "Сервер вернул код "
                                + statusCode
                                + (body != null && !body.isBlank() ? ": " + body : ""));
                return Optional.empty();
            }
            if (body == null) {
                System.err.println("Пустой ответ сервера.");
                return Optional.empty();
            }
            Matcher mUserId = USER_ID_PATTERN.matcher(body);
            if (mUserId.find()) {
                lastUserId = mUserId.group(1);
            }
            Matcher mUrl = SHORT_URL_PATTERN.matcher(body);
            if (mUrl.find()) {
                return Optional.of(mUrl.group(1));
            }
            Matcher mCode = SHORT_CODE_PATTERN.matcher(body);
            if (mCode.find()) {
                String code = mCode.group(1);
                String shortUrl = baseUrl.replaceAll("/$", "") + "/" + code;
                return Optional.of(shortUrl);
            }
            System.err.println("В ответе сервера не найден shortUrl/shortCode. Ответ: " + body);
            return Optional.empty();
        } catch (Exception e) {
            String msg = e.getMessage();
            String detail =
                    (msg != null && !msg.isBlank())
                            ? msg
                            : (e.getCause() != null && e.getCause().getMessage() != null
                                    ? e.getCause().getMessage()
                                    : e.getClass().getSimpleName()
                                            + " (проверьте, запущен ли сервис на "
                                            + baseUrl
                                            + ")");
            System.err.println("Ошибка при создании короткой ссылки: " + detail);
            return Optional.empty();
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Открытие короткой ссылки в браузере по ТЗ: ввод короткой ссылки в консоль → перенаправление
     * на исходный ресурс в браузере. Для перехода по ссылке сервер требует заголовок X-User-Id
     * (владелец ссылки).
     */
    private static void openShortLinkInBrowser(Scanner scanner, String baseUrl) {
        String userId = lastUserId;
        if (userId == null || userId.isBlank()) {
            userId = System.getProperty("shortlinks.user-id");
        }
        if (userId == null || userId.isBlank()) {
            System.out.print(
                    "Введите ваш User-ID (UUID) или сначала создайте короткую ссылку (п.1): ");
            userId = scanner.nextLine().trim();
        }
        if (userId == null || userId.isBlank()) {
            System.out.println("User-ID обязателен для перехода по ссылке.");
            return;
        }

        System.out.print(
                "Введите короткую ссылку или код (например 3DZHeG или http://localhost:8080/3DZHeG): ");
        String input = scanner.nextLine().trim();
        if (input.isBlank()) {
            System.out.println("Ссылка не введена.");
            return;
        }

        String shortLinkUrl = toFullShortLinkUrl(input, baseUrl);
        Optional<String> originalUrl = resolveOriginalUrl(shortLinkUrl, userId);
        if (originalUrl.isEmpty()) {
            System.out.println(
                    "Ссылка недоступна: не найдена, истекла, исчерпан лимит или доступ запрещён (неверный User-ID).");
            return;
        }

        openOriginalResourceInBrowser(originalUrl.get());
    }

    private static String toFullShortLinkUrl(String input, String baseUrl) {
        if (input.startsWith("http://") || input.startsWith("https://")) {
            return input;
        }
        String path = input.startsWith("/") ? input : "/" + input;
        return baseUrl.replaceAll("/$", "") + path;
    }

    private static Optional<String> resolveOriginalUrl(String shortLinkUrl, String userId) {
        HttpClient client =
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(shortLinkUrl))
                            .header("X-User-Id", userId)
                            .GET()
                            .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                            .build();
            HttpResponse<Void> response =
                    client.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            if (code == 302 || code == 301) {
                return response.headers().firstValue("Location");
            }
            if (code == 400) {
                System.err.println("Заголовок X-User-Id обязателен (400).");
                return Optional.empty();
            }
            if (code == 403) {
                System.err.println(
                        "Доступ запрещён: ссылка принадлежит другому пользователю (403).");
                return Optional.empty();
            }
            if (code == 404) {
                System.err.println("Ссылка не найдена (404).");
                return Optional.empty();
            }
            if (code == 410) {
                System.err.println(
                        "Ссылка недоступна: истекла или исчерпан лимит переходов (410).");
                return Optional.empty();
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Ошибка при обращении к серверу: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Открывает исходный ресурс в браузере по ТЗ: Desktop.getDesktop().browse(new
     * URI(исходный_URL)).
     */
    private static void openOriginalResourceInBrowser(String originalUrl) {
        if (!Desktop.isDesktopSupported()) {
            System.out.println(
                    "Открытие браузера недоступно. Откройте ссылку вручную: " + originalUrl);
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            System.out.println(
                    "Действие BROWSE недоступно. Откройте ссылку вручную: " + originalUrl);
            return;
        }
        try {
            desktop.browse(new URI(originalUrl));
            System.out.println("Открыто в браузере: " + originalUrl);
        } catch (Exception e) {
            System.err.println("Не удалось открыть браузер: " + e.getMessage());
            System.out.println("Откройте ссылку вручную: " + originalUrl);
        }
    }
}
