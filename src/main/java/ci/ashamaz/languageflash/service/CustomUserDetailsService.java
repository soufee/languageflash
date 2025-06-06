package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.User;
import ci.ashamaz.languageflash.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Используем репозиторий напрямую, а не UserService

    @Autowired
    private HttpSession session;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.trim().isEmpty()) {
            throw new UsernameNotFoundException("Email не может быть пустым");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);

        if (!userOptional.isPresent()) {
            throw new UsernameNotFoundException("Пользователь с email " + email + " не найден");
        }

        User user = userOptional.get();

        if (!user.isEmailConfirmed()) {
            throw new UsernameNotFoundException("Email не подтвержден");
        }

        if (user.getPasswordHash() == null) {
            throw new UsernameNotFoundException("Пароль не установлен");
        }

        // Сохраняем пользовательский объект в сессии
        session.setAttribute("user", user);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (String role : user.getRoles()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                user.isEmailConfirmed(), // enabled
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                authorities
        );
    }
}