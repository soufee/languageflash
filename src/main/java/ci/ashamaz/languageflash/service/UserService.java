package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.RegisterRequest;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
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

    public User registerUser(RegisterRequest request) {
        String email = request.getEmail().trim(); // Удаляем пробелы
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Неверный формат email");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setEmailConfirmed(false);
        user.setConfirmationCode(UUID.randomUUID().toString());

        User savedUser = userRepository.save(user);
        try {
            sendConfirmationEmail(savedUser);
        } catch (RuntimeException e) {
            System.err.println("Не удалось отправить письмо подтверждения: " + e.getMessage());
        }
        return savedUser;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public boolean checkPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public void sendResetCode(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String resetCode = generateResetCode();
            user.setResetCode(resetCode);
            user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(5)); // Уже правильно
            userRepository.save(user);

            String htmlContent = createResetCodeEmail(user.getFirstName(), resetCode);
            try {
                emailService.sendHtmlEmail(email, "Восстановление пароля - Language Flash", htmlContent);
            } catch (Exception e) {
                throw new RuntimeException("Ошибка отправки письма: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Пользователь с таким email не найден");
        }
    }

    public boolean verifyResetCode(String email, String code) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getResetCode() != null && user.getResetCode().equals(code) &&
                    user.getResetCodeExpiry() != null && user.getResetCodeExpiry().isAfter(LocalDateTime.now())) {
                return true;
            }
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

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean confirmEmail(String email, String code) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (user.getConfirmationCode() != null && user.getConfirmationCode().equals(code)) {
                user.setEmailConfirmed(true);
                user.setConfirmationCode(null); // Удаляем код после подтверждения
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    private String generateResetCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String createResetCodeEmail(String firstName, String resetCode) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/reset-password-template.txt");
            String template = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            return template.replace("${firstName}", firstName).replace("${resetCode}", resetCode);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения шаблона письма восстановления: " + e.getMessage());
        }
    }

    private void sendConfirmationEmail(User user) {
        String confirmationLink = baseUrl + "/auth/confirm-email?email=" + user.getEmail() +
                "&code=" + user.getConfirmationCode();
        String htmlContent = createConfirmationEmail(user.getFirstName(), confirmationLink);
        try {
            emailService.sendHtmlEmail(user.getEmail(), "Подтверждение email - Language Flash", htmlContent);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки письма подтверждения: " + e.getMessage());
        }
    }

    private String createConfirmationEmail(String firstName, String confirmationLink) {
        try {
            Resource resource = resourceLoader.getResource("classpath:templates/email/confirmation-email-template.txt");
            String template = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            return template.replace("${firstName}", firstName).replace("${confirmationLink}", confirmationLink);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения шаблона письма подтверждения: " + e.getMessage());
        }
    }
}