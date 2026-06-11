package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.RefreshToken;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh-токены с ротацией (ТЗ 3.1): при каждом обновлении старый токен
 * инвалидируется и выдаётся новый. В БД хранится только SHA-256 хэш.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshExpirationMs;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.repository = repository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000));
        repository.save(entity);
        return token;
    }

    /** Валидирует и инвалидирует токен (ротация). Возвращает владельца. */
    @Transactional
    public User rotate(String token) {
        RefreshToken entity = repository.findByTokenHash(hash(token))
                .orElseThrow(() -> ApiException.unauthorized("Недействительный refresh-токен"));
        if (entity.isRevoked() || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.unauthorized("Refresh-токен истёк или отозван");
        }
        entity.setRevoked(true);
        repository.save(entity);
        return entity.getUser();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllForUser(userId);
    }

    @Scheduled(fixedDelayString = "PT6H")
    @Transactional
    public void cleanupExpired() {
        repository.deleteExpired(LocalDateTime.now());
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
