package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.AuthDtos.UserDto;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserDictionaryRepository dictionaryRepository;
    private final StudyActivityRepository activityRepository;
    private final OAuthAccountRepository oauthRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public UserService(UserRepository userRepository,
                       UserDictionaryRepository dictionaryRepository,
                       StudyActivityRepository activityRepository,
                       OAuthAccountRepository oauthRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.activityRepository = activityRepository;
        this.oauthRepository = oauthRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Пользователь не найден"));
    }

    @Transactional
    public UserDto updateProfile(Long userId, String firstName, String lastName, String interfaceLanguage) {
        User user = getById(userId);
        if (firstName != null) user.setFirstName(firstName);
        if (lastName != null) user.setLastName(lastName);
        if (interfaceLanguage != null) {
            if (!interfaceLanguage.matches("ru|en")) {
                throw ApiException.badRequest("Поддерживаемые языки интерфейса: ru, en");
            }
            user.setInterfaceLanguage(interfaceLanguage);
        }
        userRepository.save(user);
        return UserDto.from(user);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Неверный текущий пароль");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /** Полное удаление аккаунта и всех персональных данных (GDPR, ТЗ 3.1). */
    @Transactional
    public void deleteAccount(Long userId) {
        User user = getById(userId);
        refreshTokenRepository.deleteByUserId(userId);
        oauthRepository.deleteByUserId(userId);
        dictionaryRepository.deleteByUserId(userId);
        activityRepository.deleteByUserId(userId);
        userRepository.delete(user);
        log.info("Аккаунт пользователя {} полностью удалён (GDPR)", userId);
    }

    public Map<String, Object> getSettings(Long userId) {
        User user = getById(userId);
        Map<String, Object> defaults = defaultSettings();
        if (user.getSettings() == null || user.getSettings().isBlank()) {
            return defaults;
        }
        try {
            Map<String, Object> settings = objectMapper.readValue(user.getSettings(), new TypeReference<>() {});
            defaults.forEach(settings::putIfAbsent);
            return settings;
        } catch (Exception e) {
            log.warn("Не удалось разобрать настройки пользователя {}: {}", userId, e.getMessage());
            return defaults;
        }
    }

    @Transactional
    public Map<String, Object> updateSettings(Long userId, Map<String, Object> updates) {
        User user = getById(userId);
        Map<String, Object> settings = getSettings(userId);
        settings.putAll(updates);
        try {
            user.setSettings(objectMapper.writeValueAsString(settings));
        } catch (Exception e) {
            throw ApiException.badRequest("Не удалось сохранить настройки");
        }
        userRepository.save(user);
        return settings;
    }

    private Map<String, Object> defaultSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("activeWordsCount", 50);
        defaults.put("flashSpeedMs", 1000);
        defaults.put("flashDurationMin", 3);
        defaults.put("flashMode", "MIXED"); // DIRECT / REVERSE / MIXED
        defaults.put("language", null);
        defaults.put("minLevel", "A1");
        defaults.put("tags", java.util.List.of());
        return defaults;
    }
}
