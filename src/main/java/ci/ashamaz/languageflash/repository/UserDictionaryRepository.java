package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.DictionarySource;
import ci.ashamaz.languageflash.model.UserDictionaryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserDictionaryRepository extends JpaRepository<UserDictionaryEntry, Long> {

    long countByUserId(Long userId);

    Optional<UserDictionaryEntry> findByIdAndUserId(Long id, Long userId);

    Optional<UserDictionaryEntry> findByUserIdAndWordId(Long userId, Long wordId);

    @Query("""
            SELECT d FROM UserDictionaryEntry d LEFT JOIN FETCH d.word
            WHERE d.user.id = :userId
              AND (:source IS NULL OR d.source = :source)
              AND (:learned IS NULL OR d.learned = :learned)
            ORDER BY d.addedAt DESC
            """)
    Page<UserDictionaryEntry> findFiltered(@Param("userId") Long userId,
                                           @Param("source") DictionarySource source,
                                           @Param("learned") Boolean learned,
                                           Pageable pageable);

    @Query("SELECT d FROM UserDictionaryEntry d LEFT JOIN FETCH d.word WHERE d.user.id = :userId AND d.inActiveBatch = true AND d.learned = false")
    List<UserDictionaryEntry> findActiveBatch(@Param("userId") Long userId);

    @Query("""
            SELECT d FROM UserDictionaryEntry d LEFT JOIN FETCH d.word
            WHERE d.user.id = :userId AND d.learned = false AND d.inActiveBatch = false
            ORDER BY d.addedAt ASC
            """)
    List<UserDictionaryEntry> findBacklog(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndLearnedTrue(Long userId);

    long countByUserIdAndInActiveBatchTrueAndLearnedFalse(Long userId);

    @Query("""
            SELECT d FROM UserDictionaryEntry d LEFT JOIN FETCH d.word
            WHERE d.user.id = :userId AND d.learned = true
            ORDER BY d.lastReviewed DESC
            """)
    Page<UserDictionaryEntry> findLearned(@Param("userId") Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
