package ci.ashamaz.languageflash.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService();
        emailService.setMailSender(mailSender);
        emailService.setFromEmail("test@example.com");
    }

    @Test
    void sendHtmlEmail_success() throws MessagingException {
        String to = "user@example.com";
        String subject = "Test Subject";
        String htmlContent = "<p>Test</p>";
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail(to, subject, htmlContent);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}