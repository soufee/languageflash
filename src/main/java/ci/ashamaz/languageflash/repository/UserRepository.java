package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
}