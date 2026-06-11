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
 * VK ID: проверка access-токена через метод secure.checkToken / users.get
 * с сервисным ключом приложения.
 */
@Component
@Slf4j
public class VkOAuthVerifier implements OAuthVerifier {

    private final String clientId;
    private final String serviceToken;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public VkOAuthVerifier(@Value("${app.oauth.vk.client-id}") String clientId,
                           @Value("${app.oauth.vk.service-token}") String serviceToken) {
        this.clientId = clientId;
        this.serviceToken = serviceToken;
    }

    @Override
    public String provider() {
        return "VK";
    }

    @Override
    public boolean isConfigured() {
        return !clientId.isBlank() && !serviceToken.isBlank();
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        try {
            String url = "https://api.vk.com/method/secure.checkToken?v=5.199"
                    + "&token=" + URLEncoder.encode(identityToken, StandardCharsets.UTF_8)
                    + "&access_token=" + URLEncoder.encode(serviceToken, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode node = mapper.readTree(response.body());
            if (node.has("error")) {
                throw ApiException.unauthorized("VK отклонил токен: "
                        + node.path("error").path("error_msg").asText());
            }
            String userId = node.path("response").path("user_id").asText();
            return new OAuthUserInfo(userId, null, null, null);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка верификации VK-токена: {}", e.getMessage());
            throw ApiException.unauthorized("Не удалось верифицировать VK-токен");
        }
    }
}
