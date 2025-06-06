package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_success() {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setEmailConfirmed(true);
        user.setRoles(Collections.singleton("USER"));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertEquals(email, result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(session, times(1)).setAttribute("user", user);
    }

    @Test
    void loadUserByUsername_notFound() {
        String email = "user@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertEquals("Пользователь с email " + email + " не найден", exception.getMessage());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void loadUserByUsername_emailNotConfirmed() {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        user.setEmailConfirmed(false);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertEquals("Email не подтвержден", exception.getMessage());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void loadUserByUsername_userWithMultipleRoles() {
        String email = "admin@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setEmailConfirmed(true);
        user.setRoles(new HashSet<>(Collections.singletonList("USER")));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertEquals(email, result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        assertTrue(result.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(session, times(1)).setAttribute("user", user);
    }

    @Test
    void loadUserByUsername_userWithNoRoles() {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("hashedPassword");
        user.setEmailConfirmed(true);
        user.setRoles(Collections.emptySet());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertEquals(email, result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        assertTrue(result.getAuthorities().isEmpty());
        verify(session, times(1)).setAttribute("user", user);
    }

    @Test
    void loadUserByUsername_nullEmail() {
        String email = null;

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertEquals("Email не может быть пустым", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void loadUserByUsername_emptyEmail() {
        String email = "";

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertEquals("Email не может быть пустым", exception.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void loadUserByUsername_nullPasswordHash() {
        String email = "user@example.com";
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(null);
        user.setEmailConfirmed(true);
        user.setRoles(Collections.singleton("USER"));
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(email);
        });

        assertEquals("Пароль не установлен", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(email);
        verify(session, never()).setAttribute(anyString(), any());
    }
}