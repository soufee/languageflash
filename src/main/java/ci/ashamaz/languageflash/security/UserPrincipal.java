package ci.ashamaz.languageflash.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public record UserPrincipal(Long id, String email, List<String> roles) {

    public Collection<GrantedAuthority> authorities() {
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }

    public boolean isAdmin() {
        return roles.contains("ADMIN");
    }
}
