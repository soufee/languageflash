package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.WordProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordProgressRepository extends JpaRepository<WordProgress, Long> {
    List<WordProgress> findByUserId(Long userId);

    Optional<WordProgress> findByUserIdAndWordId(Long userId, Long wordId);

    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = false")
    List<WordProgress> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = true")
    List<WordProgress> findLearnedByUserId(@Param("userId") Long userId);
}