package ci.ashamaz.languageflash.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/**
 * MyMemory — бесплатный API перевода без ключа (с дневной квотой).
 * Используется по умолчанию для локальной разработки; в продакшене
 * заменяется на Яндекс.Переводчик через app.translation.provider=YANDEX.
 */
@Component
@Slf4j
public class MyMemoryProvider implements TranslationProvider {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "MYMEMORY";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public Optional<String> translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            String url = "https://api.mymemory.translated.net/get?q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&langpair=" + URLEncoder.encode(sourceLanguage + "|" + targetLanguage, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode node = mapper.readTree(response.body());
            if (node.path("responseStatus").asInt() != 200) {
                return Optional.empty();
            }
            String translated = node.path("responseData").path("translatedText").asText(null);
            return Optional.ofNullable(translated).filter(t -> !t.isBlank());
        } catch (Exception e) {
            log.warn("MyMemory недоступен: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
