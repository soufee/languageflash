package ci.ashamaz.languageflash.service.translation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/** Яндекс.Переводчик (Yandex Cloud Translate API v2). Требует API-ключ. */
@Component
@Slf4j
public class YandexProvider implements TranslationProvider {

    private final String apiKey;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public YandexProvider(@Value("${app.translation.yandex.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String name() {
        return "YANDEX";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    @Override
    public Optional<String> translate(String text, String sourceLanguage, String targetLanguage) {
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "sourceLanguageCode", sourceLanguage,
                    "targetLanguageCode", targetLanguage,
                    "texts", new String[]{text}));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://translate.api.cloud.yandex.net/translate/v2/translate"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Api-Key " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Yandex Translate вернул {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }
            JsonNode node = mapper.readTree(response.body());
            JsonNode translations = node.path("translations");
            if (translations.isArray() && translations.size() > 0) {
                return Optional.ofNullable(translations.get(0).path("text").asText(null));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Yandex Translate недоступен: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
