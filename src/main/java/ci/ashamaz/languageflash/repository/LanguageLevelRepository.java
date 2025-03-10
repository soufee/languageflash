package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.LanguageLevel;
import ci.ashamaz.languageflash.model.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LanguageLevelRepository extends JpaRepository<LanguageLevel, Long> {
    List<LanguageLevel> findByLanguageId(Long languageId);
    Optional<LanguageLevel> findByLanguageIdAndLevel(Long languageId, Level level);
}