package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {
    List<Word> findByLanguageId(Long languageId);

    @Query("SELECT w FROM Word w WHERE w.language.id = :languageId AND w.level >= :minLevel")
    List<Word> findByLanguageIdAndMinLevel(@Param("languageId") Long languageId, @Param("minLevel") String minLevel);

    @Query("SELECT w FROM Word w WHERE w.language.id = :languageId AND w.level >= :minLevel AND w.tags LIKE %:tag%")
    List<Word> findByLanguageIdAndMinLevelAndTag(@Param("languageId") Long languageId,
                                                 @Param("minLevel") String minLevel,
                                                 @Param("tag") String tag);

    Page<Word> findByWordStartingWith(String wordFilter, Pageable pageable);

    Page<Word> findByTranslationStartingWith(String translationFilter, Pageable pageable);

    @Query("SELECT w FROM Word w WHERE w.word LIKE :wordFilter% AND w.translation LIKE :translationFilter%")
    Page<Word> findByWordStartingWithAndTranslationStartingWith(@Param("wordFilter") String wordFilter,
                                                                @Param("translationFilter") String translationFilter,
                                                                Pageable pageable);
}