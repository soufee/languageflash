package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.RegisterRequest;
import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.mail.MessagingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private ResourceLoader resourceLoader;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        userService.setBaseUrl("http://localhost:8080");
        Resource resource = mock(Resource.class);
        when(resourceLoader.getResource("classpath:templates/email/reset-password-template.html"))
                .thenReturn(resource);
        when(resourceLoader.getResource("classpath:templates/email/confirmation-email-template.html"))
                .thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("<p>${firstName}, ${resetCode}</p>".getBytes()));
    }

    @Test
    void registerUser_success() throws MessagingException {
        RegisterRequest request = new RegisterRequest("user@example.com", "password", "John", "Doe");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser(request);

        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendHtmlEmail(eq(request.getEmail()), anyString(), anyString());
    }

    @Test
    void registerUser_emailExists() {
        RegisterRequest request = new RegisterRequest("user@example.com", "password", "John", "Doe");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(request);
        });

        assertEquals("Email уже зарегистрирован", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void checkPassword_success() {
        User user = new User();
        user.setPasswordHash("hashedPassword");
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);

        boolean result = userService.checkPassword(user, "password");

        assertTrue(result);
        verify(passwordEncoder, times(1)).matches("password", "hashedPassword");
    }

    @Test
    void sendResetCode_success() throws MessagingException {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        user.setFirstName("John");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.sendResetCode(email);

        verify(userRepository, times(1)).save(user);
        verify(emailService, times(1)).sendHtmlEmail(eq(email), anyString(), anyString());
    }

    @Test
    void verifyResetCode_success() {
        String email = "user@example.com";
        User user = new User();
        user.setResetCode("123456");
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(1));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        boolean result = userService.verifyResetCode(email, "123456");

        assertTrue(result);
    }

    @Test
    void resetPassword_success() {
        String email = "user@example.com";
        String code = "123456";
        String newPassword = "newPassword";
        User user = new User();
        user.setResetCode(code);
        user.setResetCodeExpiry(LocalDateTime.now().plusMinutes(1));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("hashedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.resetPassword(email, code, newPassword);

        assertEquals("hashedNewPassword", user.getPasswordHash());
        assertNull(user.getResetCode());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void confirmEmail_success() {
        String email = "user@example.com";
        String code = "123456";
        User user = new User();
        user.setConfirmationCode(code);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = userService.confirmEmail(email, code);

        assertTrue(result);
        assertTrue(user.isEmailConfirmed());
        assertNull(user.getConfirmationCode());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void getAllUsers_success() {
        PageRequest pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(new User(), new User());
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findAll(pageable)).thenReturn(page);

        Page<User> result = userService.getAllUsers(pageable);

        assertEquals(2, result.getTotalElements());
        verify(userRepository, times(1)).findAll(pageable);
    }

    @Test
    void findById_success() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void findByEmail_success() {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        Optional<User> result = userService.findByEmail(email);

        assertTrue(result.isPresent());
        assertEquals(email, result.get().getEmail());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void searchUsersByEmail_success() {
        String email = "user";
        PageRequest pageable = PageRequest.of(0, 10);
        List<User> users = Arrays.asList(new User(), new User());
        Page<User> page = new PageImpl<>(users);
        when(userRepository.findByEmailContainingIgnoreCase(email, pageable)).thenReturn(page);

        Page<User> result = userService.searchUsersByEmail(email, pageable);

        assertEquals(2, result.getTotalElements());
        verify(userRepository, times(1)).findByEmailContainingIgnoreCase(email, pageable);
    }

    @Test
    void save_success() {
        User user = new User();
        userService.save(user);

        verify(userRepository, times(1)).save(user);
    }

    @Test
    void getUserById_success() {
        Long id = 1L;
        User user = new User();
        user.setId(id);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        User result = userService.getUserById(id);

        assertEquals(id, result.getId());
        verify(userRepository, times(1)).findById(id);
    }

    @Test
    void updateSettings_success() throws IOException {
        Long userId = 1L;
        Map<String, Object> settings = new HashMap<>();
        settings.put("key", "value");
        User user = new User();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.updateSettings(userId, settings);

        verify(userRepository, times(1)).save(user);
        assertEquals("{\"key\":\"value\"}", user.getSettings());
    }

    @Test
    void getSettings_default() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setSettings(null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Map<String, Object> result = userService.getSettings(userId);

        assertEquals(0.1, (Double) result.get("knowThreshold"), 0.001);
        assertEquals(1000, result.get("flashSpeed"));
        assertTrue(((List<?>) result.get("tags")).isEmpty());
    }
}