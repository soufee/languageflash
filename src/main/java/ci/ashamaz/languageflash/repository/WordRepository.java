package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Tag;
import ci.ashamaz.languageflash.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Query("""
            SELECT w FROM Word w
            WHERE w.language.id = :languageId AND w.active = true
              AND w.levelOrder >= :minOrder AND w.levelOrder <= :maxOrder
              AND (:tag IS NULL OR :tag MEMBER OF w.tags)
            """)
    Page<Word> findForBrowse(@Param("languageId") Long languageId,
                             @Param("minOrder") int minOrder,
                             @Param("maxOrder") int maxOrder,
                             @Param("tag") Tag tag,
                             Pageable pageable);

    @Query("""
            SELECT w FROM Word w
            WHERE w.language.id = :languageId AND w.active = true
              AND w.levelOrder >= :minOrder AND w.levelOrder <= :maxOrder
              AND (:tag IS NULL OR :tag MEMBER OF w.tags)
              AND w.id NOT IN (SELECT d.word.id FROM UserDictionaryEntry d
                               WHERE d.user.id = :userId AND d.word IS NOT NULL)
            ORDER BY w.levelOrder ASC, w.id ASC
            """)
    List<Word> findCandidatesForUser(@Param("userId") Long userId,
                                     @Param("languageId") Long languageId,
                                     @Param("minOrder") int minOrder,
                                     @Param("maxOrder") int maxOrder,
                                     @Param("tag") Tag tag,
                                     Pageable pageable);

    @Query("SELECT w FROM Word w WHERE w.language.id = :languageId AND w.word = :word AND w.active = true")
    List<Word> findByLanguageIdAndWord(@Param("languageId") Long languageId, @Param("word") String word);

    long countByLanguageIdAndLevelOrderAndActiveTrue(Long languageId, int levelOrder);
}
