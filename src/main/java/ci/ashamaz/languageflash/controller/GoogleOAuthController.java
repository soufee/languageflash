package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.AuthDtos.AuthResponse;
import ci.ashamaz.languageflash.dto.AuthDtos.OAuth2Request;
import ci.ashamaz.languageflash.service.AuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@RestController
@Slf4j
public class GoogleOAuthController {

    private final AuthService authService;
    private final String clientId;
    private final String clientSecret;
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleOAuthController(
            AuthService authService,
            @Value("${app.oauth.google.client-id}") String clientId,
            @Value("${app.oauth.google.client-secret}") String clientSecret,
            @Value("${app.base-url}") String baseUrl) {
        this.authService = authService;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseUrl = baseUrl;
    }

    @GetMapping("/api/v1/auth/google")
    public ResponseEntity<Void> loginGoogle() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            log.error("Google OAuth client-id or client-secret is not configured");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        String state = "lf." + UUID.randomUUID().toString();
        String redirectUri = "https://rps-battles.com/auth/google/callback";
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("openid email profile", StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) +
                "&access_type=online" +
                "&prompt=select_account";

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleAuthUrl)
                .build();
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) {
        
        if (error != null) {
            log.error("Google OAuth error parameter: {}", error);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, baseUrl + "/?error=" + URLEncoder.encode(error, StandardCharsets.UTF_8))
                    .build();
        }

        if (code == null || code.isBlank()) {
            log.error("Google OAuth authorization code is missing");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, baseUrl + "/?error=missing_code")
                    .build();
        }

        try {
            String redirectUri = "https://rps-battles.com/auth/google/callback";
            String requestBody = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                    "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                    "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8) +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                    "&grant_type=authorization_code";

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Google token exchange failed: status={}, body={}", response.statusCode(), response.body());
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, baseUrl + "/?error=token_exchange_failed")
                        .build();
            }

            JsonNode tokenData = objectMapper.readTree(response.body());
            String identityToken = tokenData.path("id_token").asText();
            if (identityToken == null || identityToken.isBlank()) {
                log.error("Google response is missing id_token: {}", response.body());
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, baseUrl + "/?error=missing_id_token")
                        .build();
            }

            AuthResponse authResponse = authService.oauth2(new OAuth2Request("GOOGLE", identityToken));
            
            String targetUrl = baseUrl + "/?token=" + URLEncoder.encode(authResponse.accessToken(), StandardCharsets.UTF_8) +
                    "&refreshToken=" + URLEncoder.encode(authResponse.refreshToken(), StandardCharsets.UTF_8);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, targetUrl)
                    .build();

        } catch (Exception e) {
            log.error("Google callback error", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, baseUrl + "/?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8))
                    .build();
        }
    }
}
