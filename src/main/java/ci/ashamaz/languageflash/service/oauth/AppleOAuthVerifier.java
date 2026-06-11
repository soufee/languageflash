package ci.ashamaz.languageflash.service.oauth;

import ci.ashamaz.languageflash.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Sign in with Apple: JWT-валидация подписи Apple на бэкенде (ТЗ 3.1).
 * Публичные ключи берутся из JWKS Apple (https://appleid.apple.com/auth/keys).
 */
@Component
@Slf4j
public class AppleOAuthVerifier implements OAuthVerifier {

    private final String clientId;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public AppleOAuthVerifier(@Value("${app.oauth.apple.client-id}") String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String provider() {
        return "APPLE";
    }

    @Override
    public boolean isConfigured() {
        return !clientId.isBlank();
    }

    @Override
    public OAuthUserInfo verify(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) {
                throw ApiException.unauthorized("Некорректный формат Apple-токена");
            }
            JsonNode header = mapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode payload = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));

            verifySignature(parts, header.path("kid").asText());

            if (!"https://appleid.apple.com".equals(payload.path("iss").asText())) {
                throw ApiException.unauthorized("Некорректный издатель токена");
            }
            if (!clientId.equals(payload.path("aud").asText())) {
                throw ApiException.unauthorized("Токен выдан для другого приложения");
            }
            if (payload.path("exp").asLong() < Instant.now().getEpochSecond()) {
                throw ApiException.unauthorized("Apple-токен истёк");
            }
            return new OAuthUserInfo(payload.path("sub").asText(),
                    payload.path("email").asText(null), null, null);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка верификации Apple-токена: {}", e.getMessage());
            throw ApiException.unauthorized("Не удалось верифицировать Apple-токен");
        }
    }

    private void verifySignature(String[] parts, String kid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://appleid.apple.com/auth/keys"))
                .timeout(Duration.ofSeconds(10)).GET().build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode keys = mapper.readTree(response.body()).path("keys");

        for (JsonNode key : keys) {
            if (kid.equals(key.path("kid").asText())) {
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("e").asText()));
                PublicKey publicKey = KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));

                Signature sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(publicKey);
                sig.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
                if (sig.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                    return;
                }
                throw ApiException.unauthorized("Подпись Apple-токена недействительна");
            }
        }
        throw ApiException.unauthorized("Ключ подписи Apple не найден");
    }
}
