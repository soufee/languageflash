package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.TranslationCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TranslationCacheRepository extends JpaRepository<TranslationCacheEntry, Long> {
    Optional<TranslationCacheEntry> findBySourceTextAndSourceLanguageAndTargetLanguage(
            String sourceText, String sourceLanguage, String targetLanguage);

    @Modifying
    @Query("DELETE FROM TranslationCacheEntry t WHERE t.cachedAt < :cutoff")
    void deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
