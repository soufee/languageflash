package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
    Optional<User> findByEmail(String email);
}