package ci.ashamaz.languageflash.service.oauth;

import ci.ashamaz.languageflash.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Верификация Google ID-токена через официальный tokeninfo-эндпоинт.
 * Google сам проверяет подпись; мы дополнительно сверяем audience (client_id).
 */
@Component
@Slf4j
public class GoogleOAuthVerifier implements OAuthVerifier {

    private final String clientId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GoogleOAuthVerifier(@Value("${app.oauth.google.client-id}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String provider() {
        return "GOOGLE";
    }

    @Override
    public boolean isConfigured() {
        return !clientId.isBlank();
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token="
                            + URLEncoder.encode(identityToken, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw ApiException.unauthorized("Google отклонил identity-токен");
            }
            JsonNode node = mapper.readTree(response.body());
            if (!clientId.equals(node.path("aud").asText())) {
                throw ApiException.unauthorized("Токен выдан для другого приложения");
            }
            return new OAuthUserInfo(
                    node.path("sub").asText(),
                    node.path("email").asText(null),
                    node.path("given_name").asText(null),
                    node.path("family_name").asText(null));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка верификации Google-токена: {}", e.getMessage());
            throw ApiException.unauthorized("Не удалось верифицировать Google-токен");
        }
    }
}
