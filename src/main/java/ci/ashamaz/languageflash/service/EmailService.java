package ci.ashamaz.languageflash.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Отправка писем. В режиме app.mail.mock=true письма пишутся в лог —
 * это позволяет запускать проект локально без SMTP-кредов.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean mock;
    private final String fromEmail;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.mock}") boolean mock,
                        @Value("${spring.mail.username:}") String fromEmail) {
        this.mailSender = mailSender;
        this.mock = mock;
        this.fromEmail = fromEmail;
    }

    public void send(String to, String subject, String htmlContent) {
        if (mock || fromEmail.isBlank()) {
            log.info("[MOCK EMAIL] to={}, subject={}\n{}", to, subject, htmlContent);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom(fromEmail);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Не удалось отправить письмо на {}: {}", to, e.getMessage());
            throw new RuntimeException("Не удалось отправить письмо", e);
        }
    }

    public void sendConfirmation(String to, String firstName, String code, String baseUrl) {
        String name = firstName != null ? firstName : "Пользователь";
        String link = baseUrl + "/confirm-email?email=" + to + "&code=" + code;
        send(to, "Подтверждение регистрации — Language Flash", """
                <h2>Здравствуйте, %s!</h2>
                <p>Подтвердите ваш email, перейдя по ссылке:</p>
                <p><a href="%s">%s</a></p>
                <p>Или введите код подтверждения: <b>%s</b></p>
                """.formatted(name, link, link, code));
    }

    public void sendResetCode(String to, String firstName, String code) {
        String name = firstName != null ? firstName : "Пользователь";
        send(to, "Восстановление пароля — Language Flash", """
                <h2>Здравствуйте, %s!</h2>
                <p>Ваш код для сброса пароля: <b>%s</b></p>
                <p>Код действителен 15 минут. Если вы не запрашивали сброс — проигнорируйте письмо.</p>
                """.formatted(name, code));
    }
}
