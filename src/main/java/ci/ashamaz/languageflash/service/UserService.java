package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.RegisterRequest;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void registerUser(RegisterRequest request) {
        if (!EMAIL_PATTERN.matcher(request.getEmail()).matches()) {
            throw new IllegalArgumentException("Неверный формат email");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email уже зарегистрирован");
        }
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(Set.of("USER"));
        String confirmationCode = generateCode(6);
        user.setConfirmationCode(confirmationCode);
        // Инициализация настроек по умолчанию
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("knowThreshold", 0.1);
        defaultSettings.put("flashSpeed", 1000);
        defaultSettings.put("tags", Collections.emptyList()); // Добавляем пустой список тегов
        try {
            user.setSettings(objectMapper.writeValueAsString(defaultSettings));
        } catch (IOException e) {
            log.error("Ошибка сериализации настроек: {}", e.getMessage());
            throw new RuntimeException("Ошибка инициализации настроек пользователя");
        }
        userRepository.save(user);

        String confirmationLink = baseUrl + "/auth/confirm-email?email=" + user.getEmail() + "&code=" + confirmationCode;
        String htmlContent = createConfirmationEmail(user.getFirstName(), confirmationLink);
        try {
            emailService.sendHtmlEmail(request.getEmail(), "Подтверждение регистрации - Language Flash", htmlContent);
        } catch (Exception e) {
            log.error("Ошибка отправки письма подтверждения для {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Не удалось отправить письмо подтверждения: " + e.getMessage());
        }
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public void sendResetCode(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String resetCode = generateCode(6);
            user.setResetCode(resetCode);
            user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            String htmlContent = createResetCodeEmail(user.getFirstName(), resetCode);
            try {
                emailService.sendHtmlEmail(email, "Восстановление пароля - Language Flash", htmlContent);
            } catch (Exception e) {
                log.error("Ошибка отправки письма восстановления для {}: {}", email, e.getMessage());
                throw new RuntimeException("Не удалось отправить письмо восстановления: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Пользователь с таким email не найден");
        }
    }

    public boolean verifyResetCode(String email, String code) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return user.getResetCode() != null && user.getResetCode().equals(code) &&
                    user.getResetCodeExpiry() != null && user.getResetCodeExpiry().isAfter(LocalDateTime.now());
        }
        return false;
    }

    public void resetPassword(String email, String code, String newPassword) {
        if (verifyResetCode(email, code)) {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                user.setPasswordHash(passwordEncoder.encode(newPassword));
                user.setResetCode(null);
                user.setResetCodeExpiry(null);
                userRepository.save(user);
            }
        } else {
            throw new IllegalArgumentException("Неверный или просроченный код");
        }
    }

    public boolean confirmEmail(String email, String code) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getConfirmationCode() != null && user.getConfirmationCode().equals(code)) {
                user.setEmailConfirmed(true);
                user.setConfirmationCode(null);
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Page<User> searchUsersByEmail(String email, Pageable pageable) {
        return userRepository.findByEmailContainingIgnoreCase(email, pageable);
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь с ID " + id + " не найден"));
    }

    @Transactional
    public void updateSettings(Long userId, Map<String, Object> settings) {
        User user = getUserById(userId);
        try {
            user.setSettings(objectMapper.writeValueAsString(settings));
            userRepository.save(user);
        } catch (IOException e) {
            log.error("Ошибка обновления настроек для пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка обновления настроек");
        }
    }

    public Map<String, Object> getSettings(Long userId) {
        User user = getUserById(userId);
        if (user.getSettings() == null || user.getSettings().isEmpty()) {
            Map<String, Object> defaultSettings = new HashMap<>();
            defaultSettings.put("knowThreshold", 0.1);
            defaultSettings.put("flashSpeed", 1000);
            defaultSettings.put("tags", Collections.emptyList());
            return defaultSettings;
        }
        try {
            Map<String, Object> settings = objectMapper.readValue(user.getSettings(), Map.class);
            if (!settings.containsKey("tags")) {
                settings.put("tags", Collections.emptyList()); // Добавляем tags, если его нет
            }
            return settings;
        } catch (IOException e) {
            log.error("Ошибка чтения настроек для пользователя {}: {}", userId, e.getMessage());
            throw new RuntimeException("Ошибка чтения настроек");
        }
    }
    private String generateCode(int length) {
        Random random = new Random();
        return String.format("%0" + length + "d", random.nextInt((int) Math.pow(10, length)));
    }

    private String createResetCodeEmail(String firstName, String resetCode) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/reset-password-template.html");
            String template = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            String safeFirstName = firstName != null ? firstName : "Пользователь";
            return template.replace("${firstName}", safeFirstName).replace("${resetCode}", resetCode);
        } catch (IOException e) {
            log.error("Ошибка чтения шаблона письма восстановления: {}", e.getMessage());
            throw new RuntimeException("Ошибка чтения шаблона письма восстановления: " + e.getMessage());
        }
    }

    private String createConfirmationEmail(String firstName, String confirmationLink) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/confirmation-email-template.html");
            String template = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            String safeFirstName = firstName != null ? firstName : "Пользователь";
            return template.replace("${firstName}", safeFirstName).replace("${confirmationLink}", confirmationLink);
        } catch (IOException e) {
            log.error("Ошибка чтения шаблона письма подтверждения: {}", e.getMessage());
            throw new RuntimeException("Ошибка чтения шаблона письма подтверждения: " + e.getMessage());
        }
    }
}