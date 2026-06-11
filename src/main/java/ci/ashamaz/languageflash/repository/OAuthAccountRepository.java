package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {
    Optional<OAuthAccount> findByProviderAndProviderUserId(String provider, String providerUserId);
    List<OAuthAccount> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
