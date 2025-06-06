package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.WordProgress;
import ci.ashamaz.languageflash.model.WordSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordProgressRepository extends JpaRepository<WordProgress, Long> {
    Optional<WordProgress> findByUserIdAndWordId(Long userId, Long wordId);
    List<WordProgress> findByUserId(Long userId);

    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = false")
    List<WordProgress> findActiveByUserId(@Param("userId") Long userId);

    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = true")
    List<WordProgress> findLearnedByUserId(@Param("userId") Long userId);
    
    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = false AND wp.source = :source")
    List<WordProgress> findActiveByUserIdAndSource(@Param("userId") Long userId, @Param("source") WordSource source);
    
    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.isLearned = true AND wp.source = :source")
    List<WordProgress> findLearnedByUserIdAndSource(@Param("userId") Long userId, @Param("source") WordSource source);
    
    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.source = :source")
    List<WordProgress> findByUserIdAndSource(@Param("userId") Long userId, @Param("source") WordSource source);
    
    @Query("SELECT wp FROM WordProgress wp WHERE wp.user.id = :userId AND wp.source = :source AND wp.text.id = :textId")
    List<WordProgress> findByUserIdAndSourceAndTextId(@Param("userId") Long userId, @Param("source") WordSource source, @Param("textId") Long textId);

    boolean existsByUserIdAndTextIdAndSource(Long userId, Long textId, WordSource source);
}